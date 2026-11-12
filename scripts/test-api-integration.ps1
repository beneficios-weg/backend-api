# PostgreSQL and credentials below are disposable test fixtures, never production credentials.
[CmdletBinding()]
param([switch]$Offline)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$container = 'weg-benefits-api-test-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$created = $false
$exitCode = 1
$environmentNames = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'API_TEST_DB_URL', 'CATALOG_TEST_DB_URL')
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

Push-Location -LiteralPath $repo
try {
    # The integration server follows the project's local port convention; never stop another app.
    $portProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 8081)
    try { $portProbe.Start() }
    catch { throw 'A porta 8081 esta ocupada. Encerre sua API local antes de executar esta suite.' }
    finally { $portProbe.Stop() }
    & docker info --format '{{.ServerVersion}}'
    if ($LASTEXITCODE -ne 0) { throw 'Docker indisponivel. Inicie o Docker Desktop e tente novamente.' }

    Write-Host 'Iniciando PostgreSQL descartavel em memoria, sem volume persistente.'
    & docker run --detach --rm --name $container --label 'com.weg.benefits.purpose=integration-test' `
        --tmpfs /var/lib/postgresql/data -p '127.0.0.1::5432' `
        -e POSTGRES_DB=benefits_api_test -e POSTGRES_USER=benefits_test `
        -e POSTGRES_PASSWORD=benefits_test_only postgres:17-alpine
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel criar o PostgreSQL de teste.' }
    $created = $true

    $ready = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        & docker exec $container pg_isready -U benefits_test -d benefits_api_test *> $null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'PostgreSQL de teste nao ficou pronto em 45 segundos.' }

    # Existing Catalog tests have their own guarded database name.
    & docker exec $container createdb -U benefits_test benefits_catalog_test
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel criar o banco isolado dos testes de Catalog.' }
    $testPort = $null
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        $portsJson = & docker inspect --format '{{json .NetworkSettings.Ports}}' $container
        if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel inspecionar a porta de teste.' }
        $ports = $portsJson | ConvertFrom-Json
        $binding = @($ports.'5432/tcp') | Where-Object HostIp -eq '127.0.0.1' | Select-Object -First 1
        if ($null -ne $binding -and [string]$binding.HostPort -match '^[0-9]+$') {
            $testPort = [string]$binding.HostPort
            break
        }
        Start-Sleep -Seconds 1
    }
    if ($null -eq $testPort) { throw 'Nao foi possivel identificar a porta local exclusiva de teste.' }
    $env:API_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:${testPort}/benefits_api_test"
    $env:CATALOG_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:${testPort}/benefits_catalog_test"
    $env:DB_URL = $env:API_TEST_DB_URL
    $env:DB_USERNAME = 'benefits_test'
    $env:DB_PASSWORD = 'benefits_test_only'

    # Keep Windows/JDK test instrumentation sockets inside the repository.
    $null = New-Item -ItemType Directory -Path (Join-Path $repo 'target') -Force
    $mavenArguments = @('-B', "-DargLine=-Djdk.net.unixdomain.tmpdir=$repo\target", 'verify')
    if ($Offline) { $mavenArguments = @('-o') + $mavenArguments }
    Write-Host 'Executando suite Java completa com os dois bancos descartaveis.'
    & (Join-Path $repo 'mvnw.cmd') @mavenArguments
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) { throw 'A validacao falhou. Consulte target/surefire-reports.' }

    # An explicitly enabled integration suite must never silently pass by skipping all tests.
    $reportPath = Join-Path $repo 'target/surefire-reports/TEST-com.weg.benefits.integration.SessionEngagementIntegrationTests.xml'
    [xml]$report = Get-Content -LiteralPath $reportPath -Raw
    if ([int]$report.testsuite.tests -lt 1 -or [int]$report.testsuite.skipped -ne 0) {
        $exitCode = 1
        throw 'A suite de sessao/Engagement nao foi executada integralmente.'
    }
} finally {
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    if ($created) {
        try {
            # JSON avoids nested native-command quoting differences in Windows PowerShell 5.1.
            $labelsJson = & docker inspect --format '{{json .Config.Labels}}' $container
            if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel conferir o container de teste.' }
            $labels = $labelsJson | ConvertFrom-Json
            if ($labels.'com.weg.benefits.purpose' -ne 'integration-test') {
                throw 'A identificacao do container de teste nao corresponde ao esperado.'
            }
            & docker rm --force --volumes $container | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel remover o container de teste.' }
        } finally {
            Pop-Location
        }
    } else {
        Pop-Location
    }
}
Write-Host 'Validacao e limpeza concluidas; nenhum banco de desenvolvimento ou hospedado foi utilizado.'
