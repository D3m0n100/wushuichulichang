param([Parameter(Mandatory=$true)][string]$Root)

$ErrorActionPreference = 'Stop'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
Set-Location -LiteralPath $rootPath
$logDir = Join-Path $rootPath 'logs'
$runtimeDir = Join-Path $rootPath 'runtime'
$jar = Join-Path $rootPath 'app\waterfactoryceshi.jar'
$config = Join-Path $rootPath 'config\application-prod.yaml'
$watchdogLog = Join-Path $logDir 'watchdog.log'
$statusFile = Join-Path $runtimeDir 'status.txt'
$backendPidFile = Join-Path $runtimeDir 'backend.pid'
$supervisorPidFile = Join-Path $runtimeDir 'supervisor.pid'
$restartFile = Join-Path $runtimeDir 'restart-count.txt'
$backupScript = Join-Path $rootPath 'scripts\backup-db.ps1'
New-Item -ItemType Directory -Force -Path $logDir,$runtimeDir | Out-Null

$authStateDir = Join-Path $env:ProgramData 'JinanWaterBackend'
New-Item -ItemType Directory -Force -Path $authStateDir | Out-Null
try {
  $authAcl = Get-Acl -LiteralPath $authStateDir
  $authAcl.SetAccessRuleProtection($true, $false)
  foreach ($existingRule in @($authAcl.Access)) {
    $authAcl.RemoveAccessRuleSpecific($existingRule)
  }
  $inheritance = [Security.AccessControl.InheritanceFlags]'ContainerInherit, ObjectInherit'
  $propagation = [Security.AccessControl.PropagationFlags]::None
  $accessType = [Security.AccessControl.AccessControlType]::Allow
  $systemSid = New-Object Security.Principal.SecurityIdentifier('S-1-5-18')
  $administratorsSid = New-Object Security.Principal.SecurityIdentifier('S-1-5-32-544')
  $authAcl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($systemSid, 'FullControl', $inheritance, $propagation, $accessType)))
  $authAcl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($administratorsSid, 'FullControl', $inheritance, $propagation, $accessType)))
  Set-Acl -LiteralPath $authStateDir -AclObject $authAcl -ErrorAction Stop
}
catch {
  Add-Content -LiteralPath $watchdogLog -Value ('[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] WARNING: Unable to restrict authentication state directory ACL: ' + $_.Exception.Message) -Encoding UTF8
}

$createdNew = $false
$mutex = New-Object System.Threading.Mutex($true, 'Global\JinanWaterBackendSupervisor', [ref]$createdNew)
if (-not $createdNew) { exit 0 }

function Write-Log([string]$Message) {
  Add-Content -LiteralPath $watchdogLog -Value ('[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] ' + $Message) -Encoding UTF8
}

function Get-MySqlState {
  $service = Get-Service -ErrorAction SilentlyContinue | Where-Object {
    $_.Name -in @('MySQL80', 'MySQL84', 'mysql') -and $_.Status -eq 'Running'
  } | Select-Object -First 1
  if ($service) { return "RUNNING:$($service.Name)" }
  return 'NOT_RUNNING'
}

function Ensure-MySqlRunning {
  if ((Get-MySqlState) -like 'RUNNING:*') { return $true }
  $service = Get-Service -ErrorAction SilentlyContinue | Where-Object {
    $_.Name -in @('MySQL80', 'MySQL84', 'mysql')
  } | Select-Object -First 1
  if (-not $service) { return $false }
  try {
    Start-Service -Name $service.Name
    $service.WaitForStatus('Running', [TimeSpan]::FromSeconds(30))
    Write-Log "MySQL service started: $($service.Name)"
    return $true
  }
  catch {
    Write-Log ('Unable to start MySQL service: ' + $_.Exception.Message)
    return $false
  }
}

function Find-Java {
  $candidates = @()
  $candidates += (Join-Path $runtimeDir 'java\bin\java.exe')
  if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
  $candidates += 'D:\pro\jdk17\bin\java.exe'
  $command = Get-Command java.exe -ErrorAction SilentlyContinue
  if ($command) { $candidates += $command.Source }
  foreach ($candidate in ($candidates | Select-Object -Unique)) {
    if (-not $candidate -or -not (Test-Path -LiteralPath $candidate)) { continue }
    $previousErrorActionPreference = $ErrorActionPreference
    try {
      # java -version writes normal version text to stderr; do not treat that as a fatal script error.
      $ErrorActionPreference = 'Continue'
      $versionOutput = & $candidate -version 2>&1
      $versionLine = ($versionOutput | ForEach-Object { $_.ToString() } | Select-Object -First 1) -as [string]
    }
    finally {
      $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($versionLine -match 'version\s+"(?:(?<legacy>1)\.)?(?<major>\d+)') {
      $major = [int]$Matches['major']
      if ($major -ge 17) { return $candidate }
      Write-Log "Skipped incompatible Java runtime: $candidate ($versionLine)"
    }
  }
  throw 'Java 17 or newer was not found. Install JDK17 or set JAVA_HOME to a compatible runtime.'
}

function Get-BackendProcess {
  if (-not (Test-Path -LiteralPath $backendPidFile)) { return $null }
  $pidText = Get-Content -LiteralPath $backendPidFile -ErrorAction SilentlyContinue | Select-Object -First 1
  if (-not $pidText) { return $null }
  $process = Get-CimInstance Win32_Process -Filter "ProcessId=$pidText" -ErrorAction SilentlyContinue
  if (-not $process -or $process.Name -ne 'java.exe' -or $process.CommandLine -notlike "*$jar*") { return $null }
  return $process
}

function Test-BackendHealth {
  try {
    $response = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:8081/api/monitor/points' -TimeoutSec 5
    return $response.StatusCode -eq 200
  }
  catch { return $false }
}

function Read-RestartCount {
  if (-not (Test-Path -LiteralPath $restartFile)) { return 0 }
  $value = Get-Content -LiteralPath $restartFile -ErrorAction SilentlyContinue | Select-Object -First 1
  $number = 0
  if ([int]::TryParse($value, [ref]$number)) { return $number }
  return 0
}

function Increment-RestartCount {
  $count = (Read-RestartCount) + 1
  Set-Content -LiteralPath $restartFile -Value $count -Encoding ASCII
  return $count
}

function Read-RuntimeFile([string]$Name) {
  $file = Join-Path $runtimeDir $Name
  if (Test-Path -LiteralPath $file) {
    $line = Get-Content -LiteralPath $file -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($line) { return $line }
  }
  return '-'
}

function Get-DiskFreeText {
  try {
    $driveRoot = [System.IO.Path]::GetPathRoot($rootPath)
    $driveName = $driveRoot.Substring(0, 1)
    $drive = Get-PSDrive -Name $driveName -ErrorAction Stop
    return ('{0:N2}GB' -f ($drive.Free / 1GB))
  }
  catch { return '-' }
}

function Get-BackupAgeHours([string]$LastBackupLine) {
  if (-not $LastBackupLine -or $LastBackupLine -eq '-') { return '-' }
  try {
    $stamp = ($LastBackupLine -split '\|')[0]
    $lastBackupTime = [datetime]::ParseExact($stamp, 'yyyy-MM-dd HH:mm:ss', [Globalization.CultureInfo]::InvariantCulture)
    return ('{0:N1}' -f ((Get-Date) - $lastBackupTime).TotalHours)
  }
  catch { return '-' }
}

function Stop-BackendProcess {
  $process = Get-BackendProcess
  if ($process) {
    Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    Write-Log "Backend stopped. pid=$($process.ProcessId)"
  }
  Remove-Item -LiteralPath $backendPidFile -Force -ErrorAction SilentlyContinue
}

function Start-BackendProcess {
  if (-not (Ensure-MySqlRunning)) { throw 'MySQL service is not running.' }
  $java = Find-Java
  $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
  $out = Join-Path $logDir "backend-$stamp.out.log"
  $err = Join-Path $logDir "backend-$stamp.err.log"
  $arguments = @('-Dfile.encoding=UTF-8','-jar',$jar,"--spring.config.additional-location=file:$config")
  $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $rootPath -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle Hidden -PassThru
  Set-Content -LiteralPath $backendPidFile -Value $process.Id -Encoding ASCII
  Write-Log "Backend started. pid=$($process.Id), java=$java"
  return $process.Id
}

function Write-Status([string]$State, [string]$Message, [int]$FailureCount) {
  $backend = Get-BackendProcess
  $pidText = if ($backend) { $backend.ProcessId } else { '-' }
  $lastBackup = Read-RuntimeFile 'last-backup.txt'
  $lastBackupCopy = Read-RuntimeFile 'last-backup-copy.txt'
  $lastBackupError = Read-RuntimeFile 'last-backup-error.txt'
  $text = "state=$State`r`ntime=$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`r`nmessage=$Message`r`nsupervisorPid=$PID`r`nbackendPid=$pidText`r`nmysql=$(Get-MySqlState)`r`nhealthFailures=$FailureCount`r`nrestartCount=$(Read-RestartCount)`r`ndiskFree=$(Get-DiskFreeText)`r`nbackupAgeHours=$(Get-BackupAgeHours $lastBackup)`r`nlastBackup=$lastBackup`r`nlastBackupCopy=$lastBackupCopy`r`nlastBackupError=$lastBackupError`r`ndebug=http://127.0.0.1:8081/plc-debug.html"
  Set-Content -LiteralPath $statusFile -Value $text -Encoding UTF8
}

function Invoke-DailyMaintenance {
  $today = Get-Date -Format 'yyyy-MM-dd'
  $lastDateFile = Join-Path $runtimeDir 'last-backup-date.txt'
  $lastDate = if (Test-Path -LiteralPath $lastDateFile) { Get-Content -LiteralPath $lastDateFile | Select-Object -First 1 } else { '' }
  if ((Get-Date).Hour -ge 2 -and $lastDate -ne $today -and (Test-Path -LiteralPath $backupScript)) {
    try {
      & powershell -NoProfile -ExecutionPolicy Bypass -File $backupScript -Root $rootPath | Out-Null
      if ($LASTEXITCODE -eq 0) {
        Set-Content -LiteralPath $lastDateFile -Value $today -Encoding ASCII
        Write-Log 'Daily database backup completed.'
      }
    }
    catch { Write-Log ('Daily database backup failed: ' + $_.Exception.Message) }
  }
  Get-ChildItem -LiteralPath $logDir -File -ErrorAction SilentlyContinue | Where-Object {
    $_.LastWriteTime -lt (Get-Date).AddDays(-1095)
  } | Remove-Item -Force -ErrorAction SilentlyContinue
}

try {
  if (-not (Test-Path -LiteralPath $jar)) { throw "Backend jar not found: $jar" }
  if (-not (Test-Path -LiteralPath $config)) { throw "Backend config not found: $config" }
  Set-Content -LiteralPath $supervisorPidFile -Value $PID -Encoding ASCII
  Write-Log "Supervisor started. pid=$PID, root=$rootPath"
  $healthFailures = 0
  $knownBackendPid = $null
  while ($true) {
    try {
      $backend = Get-BackendProcess
      if (-not $backend) {
        if ($knownBackendPid) {
          $restartCount = Increment-RestartCount
          Write-Log "Backend process exited unexpectedly. automaticRestartCount=$restartCount"
        }
        $newPid = Start-BackendProcess
        $knownBackendPid = $newPid
        $healthFailures = 0
        Write-Status 'STARTING' "Backend starting, PID=$newPid" $healthFailures
        Start-Sleep -Seconds 15
      }
      elseif (Test-BackendHealth) {
        $knownBackendPid = $backend.ProcessId
        $healthFailures = 0
        Write-Status 'RUNNING' 'Backend process and HTTP health check are normal.' $healthFailures
      }
      else {
        $healthFailures++
        Write-Log "HTTP health check failed. consecutiveFailures=$healthFailures"
        Write-Status 'DEGRADED' 'Backend process exists but HTTP health check failed.' $healthFailures
        if ($healthFailures -ge 4) {
          Stop-BackendProcess
          $restartCount = Increment-RestartCount
          Write-Log "Backend restart triggered after 4 health failures. restartCount=$restartCount"
          $knownBackendPid = $null
          Start-Sleep -Seconds 3
          $healthFailures = 0
        }
      }
      Invoke-DailyMaintenance
    }
    catch {
      Write-Log ('Supervisor loop error: ' + $_.Exception.Message)
      Write-Status 'ERROR' $_.Exception.Message $healthFailures
    }
    Start-Sleep -Seconds 15
  }
}
finally {
  Remove-Item -LiteralPath $supervisorPidFile -Force -ErrorAction SilentlyContinue
  if ($mutex) { $mutex.ReleaseMutex(); $mutex.Dispose() }
}
