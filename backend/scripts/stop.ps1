param([Parameter(Mandatory=$true)][string]$Root)

$ErrorActionPreference = 'Continue'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$runtimeDir = Join-Path $rootPath 'runtime'
$statusFile = Join-Path $runtimeDir 'status.txt'
$backendPidFile = Join-Path $runtimeDir 'backend.pid'
$supervisorPidFile = Join-Path $runtimeDir 'supervisor.pid'
$jar = Join-Path $rootPath 'app\waterfactoryceshi.jar'

function Stop-ManagedProcess([string]$PidFile, [string]$ExpectedName, [string]$CommandFragment) {
  if (-not (Test-Path -LiteralPath $PidFile)) { return }
  $pidText = Get-Content -LiteralPath $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($pidText) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$pidText" -ErrorAction SilentlyContinue
    if ($process -and $process.Name -eq $ExpectedName -and $process.CommandLine -like "*$CommandFragment*") {
      Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
  }
  Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
}

if (Test-Path -LiteralPath $supervisorPidFile) {
  $supervisorPid = Get-Content -LiteralPath $supervisorPidFile -ErrorAction SilentlyContinue | Select-Object -First 1
  $supervisorProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$supervisorPid" -ErrorAction SilentlyContinue
  if ($supervisorProcess -and $supervisorProcess.Name -eq 'powershell.exe' -and
      ($supervisorProcess.CommandLine -like '*supervisor.ps1*' -or $supervisorProcess.CommandLine -like '*service-host.ps1*')) {
    Stop-Process -Id $supervisorProcess.ProcessId -Force -ErrorAction SilentlyContinue
  }
  Remove-Item -LiteralPath $supervisorPidFile -Force -ErrorAction SilentlyContinue
}
Stop-ManagedProcess $backendPidFile 'java.exe' $jar

Get-CimInstance Win32_Process -Filter "name='java.exe'" -ErrorAction SilentlyContinue | Where-Object {
  $_.CommandLine -like "*$jar*"
} | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }

$text = "state=STOPPED`r`ntime=$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`r`nmessage=Backend and supervisor stopped by operator."
Set-Content -LiteralPath $statusFile -Value $text -Encoding UTF8
