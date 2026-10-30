[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('local', 'render')]
    [string]$Environment,
    [switch]$CheckConnection
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$artifact = Join-Path $projectRoot 'target\benefits-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $artifact -PathType Leaf)) {
    throw 'Compile primeiro: .\mvnw.cmd -DskipTests package'
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($artifact)
try {
    if ($null -eq $archive.GetEntry('BOOT-INF/classes/com/weg/benefits/identity/AdminPasswordMaintenanceCli.class')) {
        throw 'O JAR ainda não contém a ferramenta. Recompile: .\mvnw.cmd -DskipTests package'
    }
} finally { $archive.Dispose() }
$javaCommand = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
if (-not (Test-Path -LiteralPath $javaCommand -PathType Leaf)) { throw 'Configure JAVA_HOME para o JDK 21.' }
$versionCheck = New-Object System.Diagnostics.ProcessStartInfo
$versionCheck.FileName = $javaCommand
$versionCheck.Arguments = '-version'
$versionCheck.UseShellExecute = $false
$versionCheck.CreateNoWindow = $true
$versionCheck.RedirectStandardOutput = $true
$versionCheck.RedirectStandardError = $true
$versionProcess = [System.Diagnostics.Process]::Start($versionCheck)
try {
    $version = $versionProcess.StandardOutput.ReadToEnd() + $versionProcess.StandardError.ReadToEnd()
    $versionProcess.WaitForExit()
    if ($versionProcess.ExitCode -ne 0 -or $version -notmatch 'version "21(?:\.|\")') {
        throw 'Use Java 21. Ajuste JAVA_HOME neste terminal e tente novamente.'
    }
} finally { $versionProcess.Dispose() }

# Keep the Java console attached. Never redirect/pipeline input or pass credentials as arguments.
$previousInputEncoding = [Console]::InputEncoding
$previousOutputEncoding = [Console]::OutputEncoding
try {
    # Preserve passwords with accents/Unicode. Restore only this terminal's encoding afterwards.
    [Console]::InputEncoding = [Text.UTF8Encoding]::new($false)
    [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
    $cliMode = if ($CheckConnection) { "--check-$($Environment.ToLowerInvariant())" } else { "--$($Environment.ToLowerInvariant())" }
    & $javaCommand '-Dloader.main=com.weg.benefits.identity.AdminPasswordMaintenanceCli' `
        '-cp' $artifact 'org.springframework.boot.loader.launch.PropertiesLauncher' $cliMode
} finally {
    [Console]::InputEncoding = $previousInputEncoding
    [Console]::OutputEncoding = $previousOutputEncoding
}
if ($LASTEXITCODE -ne 0) {
    throw 'A ferramenta não confirmou a troca. Leia o aviso acima; não envie senhas ou detalhes de conexão ao chat.'
}
