param([Parameter(Mandatory=$true)][string]$Root)

$ErrorActionPreference = 'Stop'
$taskName = 'JinanWaterBackendProduction'
$task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($task) { Unregister-ScheduledTask -TaskName $taskName -Confirm:$false }

