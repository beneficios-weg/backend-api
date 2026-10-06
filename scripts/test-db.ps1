$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$configText = Get-Content (Join-Path $repositoryRoot 'supabase/config.toml') -Raw
$projectMatch = [regex]::Match($configText, '(?m)^project_id\s*=\s*"([a-zA-Z0-9_-]+)"')
if (-not $projectMatch.Success) { throw 'Local project id unavailable' }
$containerName = 'supabase_db_' + $projectMatch.Groups[1].Value
Get-Content (Join-Path $repositoryRoot 'tests/access-and-sync.sql') -Raw |
    docker exec -i $containerName psql -U postgres -d postgres -f -
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
