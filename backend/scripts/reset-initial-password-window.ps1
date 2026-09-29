param([Parameter(Mandatory = $true)][string]$Root)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$initialWindowMinutes = 150
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$configPath = Join-Path $rootPath 'config\application-prod.yaml'
$jarPath = Join-Path $rootPath 'app\waterfactoryceshi.jar'
$runtimeDir = Join-Path $rootPath 'runtime'
$supervisorPidFile = Join-Path $runtimeDir 'supervisor.pid'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupDir = Join-Path $rootPath "backups\auth-schedule-reset-$stamp"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Test-Administrator {
  $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
  $principal = New-Object Security.Principal.WindowsPrincipal($identity)
  return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Write-AtomicText([string]$Path, [string]$Content) {
  $parent = Split-Path -Parent $Path
  New-Item -ItemType Directory -Path $parent -Force | Out-Null
  $temporary = "$Path.tmp"
  [System.IO.File]::WriteAllText($temporary, $Content, $utf8NoBom)
  Move-Item -LiteralPath $temporary -Destination $Path -Force
}

if (-not (Test-Administrator)) {
  $elevatedArguments = @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', ('"' + $PSCommandPath + '"'),
    '-Root', ('"' + $rootPath + '"')
  )
  try {
    Write-Host 'Requesting administrator permission...'
    $elevated = Start-Process -FilePath 'powershell.exe' -Verb RunAs -Wait -PassThru `
      -ArgumentList $elevatedArguments
    exit $elevated.ExitCode
  }
  catch {
    Write-Host ('Administrator authorization failed: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
  }
}
if (-not (Test-Path -LiteralPath $configPath)) {
  throw "Backend configuration file was not found: $configPath"
}

$backendRunning = Get-CimInstance Win32_Process -Filter "name='java.exe'" -ErrorAction SilentlyContinue |
  Where-Object { $_.CommandLine -like "*$jarPath*" }
if ($backendRunning) {
  throw 'The backend is still running. Stop the system before running this tool.'
}

if (Test-Path -LiteralPath $supervisorPidFile) {
  $supervisorPid = Get-Content -LiteralPath $supervisorPidFile -ErrorAction SilentlyContinue |
    Select-Object -First 1
  if ($supervisorPid) {
    $supervisor = Get-CimInstance Win32_Process -Filter "ProcessId=$supervisorPid" -ErrorAction SilentlyContinue
    if ($supervisor -and
        ($supervisor.CommandLine -like '*supervisor.ps1*' -or
         $supervisor.CommandLine -like '*service-host.ps1*')) {
      throw 'The supervisor is still running. Stop the system before running this tool.'
    }
  }
}

$programData = $env:ProgramData
if ($programData -and -not [string]::IsNullOrWhiteSpace($programData)) {
  $stateDir = Join-Path $programData 'JinanWaterBackend'
}
else {
  $stateDir = $runtimeDir
}
$statePath = Join-Path $stateDir 'auth-password-schedule.properties'
$stateBackupPath = "$statePath.bak"

New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
Copy-Item -LiteralPath $configPath -Destination (Join-Path $backupDir 'application-prod.yaml') -Force
if (Test-Path -LiteralPath $statePath) {
  Copy-Item -LiteralPath $statePath -Destination (Join-Path $backupDir 'auth-password-schedule.properties') -Force
}
if (Test-Path -LiteralPath $stateBackupPath) {
  Copy-Item -LiteralPath $stateBackupPath -Destination (Join-Path $backupDir 'auth-password-schedule.properties.bak') -Force
}

$configText = [System.IO.File]::ReadAllText($configPath, [System.Text.Encoding]::UTF8)
$minutesPattern = '(?m)^([ \t]*initial-password-minutes:[ \t]*)\d+[ \t]*$'
$minutesMatches = [regex]::Matches($configText, $minutesPattern)
if ($minutesMatches.Count -ne 1) {
  throw "Unexpected initial-password-minutes entry count: $($minutesMatches.Count)"
}
$updatedConfig = [regex]::Replace($configText, $minutesPattern, "`${1}$initialWindowMinutes")
Write-AtomicText $configPath $updatedConfig

if (Test-Path -LiteralPath $statePath) {
  Remove-Item -LiteralPath $statePath -Force
}
if (Test-Path -LiteralPath $stateBackupPath) {
  Remove-Item -LiteralPath $stateBackupPath -Force
}

$auditPath = Join-Path $rootPath "logs\auth-password-window-reset-$stamp.txt"
$auditText = "Reset prepared at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`r`n" +
  "Initial password timing starts at: next successful backend startup`r`n" +
  "Initial password window minutes: $initialWindowMinutes`r`n" +
  "Backup directory: $backupDir`r`n"
Write-AtomicText $auditPath $auditText

Write-Host ''
Write-Host 'PASSWORD 0 RESET IS READY.' -ForegroundColor Green
Write-Host 'Timing starts at the next successful backend startup.'
Write-Host "Password 0 window: $initialWindowMinutes minutes (2 hours 30 minutes)."
Write-Host "Backup directory: $backupDir"
Write-Host 'Start the system, then sign in with password 0.'
