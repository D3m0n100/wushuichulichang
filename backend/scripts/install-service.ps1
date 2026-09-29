param(
  [Parameter(Mandatory=$true)][string]$Root
)

$ErrorActionPreference = 'Stop'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$taskName = 'JinanWaterBackendProduction'
$powerShell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$serviceHost = Join-Path $rootPath 'scripts\service-host.ps1'

$arguments = '-NoProfile -ExecutionPolicy Bypass -File "' + $serviceHost + '" -Root "' + $rootPath + '"'
$action = New-ScheduledTaskAction -Execute $powerShell -Argument $arguments -WorkingDirectory $rootPath
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description 'Jingzhou Jinan WWTP backend production supervisor' -Force | Out-Null

$verify = Get-ScheduledTask -TaskName $taskName -ErrorAction Stop
if (-not $verify) { throw 'Scheduled task registration could not be verified.' }
Write-Output $taskName
