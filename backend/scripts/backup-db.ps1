param(
  [Parameter(Mandatory=$true)][string]$Root,
  [string]$DbUser = 'root',
  [Parameter(Mandatory=$true)][string]$DbPassword,
  [int]$RetentionDays = 1500,
  [string]$CopyTo = $env:WATER_BACKUP_COPY_TO
)

$ErrorActionPreference = 'Stop'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$backupDir = Join-Path $rootPath 'backups'
$runtimeDir = Join-Path $rootPath 'runtime'
$logDir = Join-Path $rootPath 'logs'
$logFile = Join-Path $logDir 'database-backup.log'
New-Item -ItemType Directory -Force -Path $backupDir,$runtimeDir,$logDir | Out-Null

function Write-Log([string]$Message) {
  Add-Content -LiteralPath $logFile -Value ('[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] ' + $Message) -Encoding UTF8
}

function Find-MySqlDump {
  $candidates = @(
    'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe',
    'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe',
    'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysqldump.exe'
  )
  foreach ($candidate in $candidates) {
    if (Test-Path -LiteralPath $candidate) { return $candidate }
  }
  $command = Get-Command mysqldump.exe -ErrorAction SilentlyContinue
  if ($command) { return $command.Source }
  throw 'mysqldump.exe was not found.'
}

$dump = Find-MySqlDump
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$sqlFile = Join-Path $backupDir "sewage_monitor_$stamp.sql"
$zipFile = Join-Path $backupDir "sewage_monitor_$stamp.zip"
Write-Log "Backup started: $zipFile"

try {
  & $dump "-u$DbUser" "-p$DbPassword" --single-transaction --routines --events --triggers --hex-blob --default-character-set=utf8mb4 "--result-file=$sqlFile" sewage_monitor
  if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $sqlFile)) {
    throw "mysqldump failed. Exit code: $LASTEXITCODE"
  }
  Compress-Archive -LiteralPath $sqlFile -DestinationPath $zipFile -CompressionLevel Optimal -Force
  if (-not (Test-Path -LiteralPath $zipFile)) { throw 'Backup archive was not created.' }
  Remove-Item -LiteralPath $sqlFile -Force
  $copyStatus = 'SKIPPED'
  if ($CopyTo -and $CopyTo.Trim().Length -gt 0) {
    $copyDir = $CopyTo.Trim()
    New-Item -ItemType Directory -Force -Path $copyDir | Out-Null
    $copyFile = Join-Path $copyDir (Split-Path -Leaf $zipFile)
    Copy-Item -LiteralPath $zipFile -Destination $copyFile -Force
    $sourceLength = (Get-Item -LiteralPath $zipFile).Length
    $copyLength = (Get-Item -LiteralPath $copyFile).Length
    if ($sourceLength -ne $copyLength) {
      throw "Backup copy size mismatch: $copyFile"
    }
    $copyStatus = "OK|$copyFile"
    Get-ChildItem -LiteralPath $copyDir -Filter 'sewage_monitor_*.zip' -File | Where-Object {
      $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays)
    } | Remove-Item -Force -ErrorAction SilentlyContinue
    Write-Log "Backup copied: $copyFile"
  }
  Set-Content -LiteralPath (Join-Path $runtimeDir 'last-backup.txt') -Value ((Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '|' + $zipFile + '|copy=' + $copyStatus) -Encoding UTF8
  Set-Content -LiteralPath (Join-Path $runtimeDir 'last-backup-copy.txt') -Value $copyStatus -Encoding UTF8
  Remove-Item -LiteralPath (Join-Path $runtimeDir 'last-backup-error.txt') -Force -ErrorAction SilentlyContinue
  Get-ChildItem -LiteralPath $backupDir -Filter 'sewage_monitor_*.zip' -File | Where-Object {
    $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays)
  } | Remove-Item -Force -ErrorAction SilentlyContinue
  Write-Log "Backup completed: $zipFile"
  Write-Output $zipFile
}
catch {
  Remove-Item -LiteralPath $sqlFile -Force -ErrorAction SilentlyContinue
  Set-Content -LiteralPath (Join-Path $runtimeDir 'last-backup-error.txt') -Value ((Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '|' + $_.Exception.Message) -Encoding UTF8
  Write-Log ('Backup failed: ' + $_.Exception.Message)
  throw
}
