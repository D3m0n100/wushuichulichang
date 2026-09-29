param([Parameter(Mandatory=$true)][string]$Root)

$ErrorActionPreference = 'Stop'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$initScript = Join-Path $rootPath 'scripts\init-db.ps1'
$supervisorScript = Join-Path $rootPath 'scripts\supervisor.ps1'
$logFile = Join-Path $rootPath 'logs\service-host.log'

try {
  $databasePassword = [Environment]::GetEnvironmentVariable('WATER_DB_PASSWORD', 'Machine')
  if ([string]::IsNullOrWhiteSpace($databasePassword)) { throw 'WATER_DB_PASSWORD machine environment variable is required.' }
  & $initScript -Root $rootPath -DbPassword $databasePassword
  if ($LASTEXITCODE -ne 0) { throw "Database initialization failed. Exit code: $LASTEXITCODE" }
  & $supervisorScript -Root $rootPath -DbPassword $databasePassword
}
catch {
  Add-Content -LiteralPath $logFile -Value ('[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] ERROR: ' + $_.Exception.Message) -Encoding UTF8
  throw
}
