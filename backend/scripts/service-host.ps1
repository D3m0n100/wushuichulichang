param([Parameter(Mandatory=$true)][string]$Root)

$ErrorActionPreference = 'Stop'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$initScript = Join-Path $rootPath 'scripts\init-db.ps1'
$supervisorScript = Join-Path $rootPath 'scripts\supervisor.ps1'
$logFile = Join-Path $rootPath 'logs\service-host.log'

try {
  & powershell -NoProfile -ExecutionPolicy Bypass -File $initScript -Root $rootPath
  if ($LASTEXITCODE -ne 0) { throw "Database initialization failed. Exit code: $LASTEXITCODE" }
  & $supervisorScript -Root $rootPath
}
catch {
  Add-Content -LiteralPath $logFile -Value ('[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] ERROR: ' + $_.Exception.Message) -Encoding UTF8
  throw
}

