param(
  [Parameter(Mandatory=$true)][string]$Root,
  [string]$DbUser = 'root',
  [Parameter(Mandatory=$true)][string]$DbPassword
)

$ErrorActionPreference = 'Stop'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$logDir = Join-Path $rootPath 'logs'
$sqlFile = Join-Path $rootPath 'database\create_sewage_monitor_db.sql'
$logFile = Join-Path $logDir 'database-init.log'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Write-Log([string]$Message) {
  $line = '[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] ' + $Message
  Add-Content -LiteralPath $logFile -Value $line -Encoding UTF8
  Write-Host $line
}

function Find-MySqlClient {
  $candidates = @(
    'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe',
    'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe'
  )
  foreach ($candidate in $candidates) {
    if (Test-Path -LiteralPath $candidate) { return $candidate }
  }
  $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
  if ($command) { return $command.Source }
  throw 'mysql.exe was not found. Install MySQL Server first.'
}

function Ensure-MySqlService {
  $service = Get-Service -ErrorAction SilentlyContinue | Where-Object {
    $_.Name -in @('MySQL80', 'MySQL84', 'mysql')
  } | Sort-Object @{Expression={ if ($_.Status -eq 'Running') { 0 } else { 1 } }} | Select-Object -First 1

  if (-not $service) {
    Write-Log 'MySQL Windows service was not found; continuing with direct client check.'
    return
  }
  if ($service.Status -ne 'Running') {
    Write-Log "Starting MySQL service: $($service.Name)"
    Start-Service -Name $service.Name
    $service.WaitForStatus('Running', [TimeSpan]::FromSeconds(30))
  }
  Write-Log "MySQL service is running: $($service.Name)"
}

if (-not (Test-Path -LiteralPath $sqlFile)) { throw "Database SQL file not found: $sqlFile" }
Ensure-MySqlService
$mysql = Find-MySqlClient
$sourcePath = $sqlFile -replace '\\','/'
Write-Log "Applying database schema with: $mysql"
& $mysql "-u$DbUser" "-p$DbPassword" --default-character-set=utf8mb4 -e "source $sourcePath"
if ($LASTEXITCODE -ne 0) { throw "Database schema initialization failed. Exit code: $LASTEXITCODE" }

$verify = & $mysql "-u$DbUser" "-p$DbPassword" --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='sewage_monitor';"
if ($LASTEXITCODE -ne 0 -or -not $verify -or [int]$verify -lt 1) {
  throw 'Database verification failed: sewage_monitor contains no tables.'
}
Write-Log "Database is ready. Table count: $verify"
