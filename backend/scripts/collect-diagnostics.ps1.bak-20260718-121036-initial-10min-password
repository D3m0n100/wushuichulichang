param([Parameter(Mandatory=$true)][string]$Root)

$ErrorActionPreference = 'Continue'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$diagnosticsDir = Join-Path $rootPath 'diagnostics'
$tempDir = Join-Path $diagnosticsDir ('temp-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$zipFile = Join-Path $diagnosticsDir ('diagnostics-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.zip')
New-Item -ItemType Directory -Force -Path $diagnosticsDir,$tempDir | Out-Null

try {
  $systemInfo = @()
  $systemInfo += 'Generated: ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
  $systemInfo += 'Computer: ' + $env:COMPUTERNAME
  $systemInfo += 'User: ' + $env:USERNAME
  $systemInfo += ''
  $systemInfo += '=== OS ==='
  $systemInfo += (Get-CimInstance Win32_OperatingSystem | Select-Object Caption,Version,LastBootUpTime,FreePhysicalMemory | Format-List | Out-String)
  $systemInfo += '=== DISKS ==='
  $systemInfo += (Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" | Select-Object DeviceID,Size,FreeSpace | Format-Table -AutoSize | Out-String)
  $systemInfo += '=== MYSQL SERVICES ==='
  $systemInfo += (Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -like '*mysql*' } | Select-Object Name,Status | Format-Table -AutoSize | Out-String)
  $systemInfo += '=== JAVA PROCESSES ==='
  $systemInfo += (Get-CimInstance Win32_Process -Filter "name='java.exe'" | Select-Object ProcessId,CommandLine | Format-List | Out-String)
  $systemInfo += '=== PORT 8081 ==='
  $systemInfo += (netstat -ano | Select-String ':8081' | Out-String)
  Set-Content -LiteralPath (Join-Path $tempDir 'system-info.txt') -Value $systemInfo -Encoding UTF8

  $config = Join-Path $rootPath 'config\application-prod.yaml'
  if (Test-Path -LiteralPath $config) {
    (Get-Content -LiteralPath $config) -replace '^(\s*(?:initial-password|password(?:-\d+)?|permanent-password):)\s*.*$', '$1 ***REDACTED***' | Set-Content -LiteralPath (Join-Path $tempDir 'application-prod-redacted.yaml') -Encoding UTF8
  }

  foreach ($folder in @('logs','runtime')) {
    $source = Join-Path $rootPath $folder
    if (Test-Path -LiteralPath $source) { Copy-Item -LiteralPath $source -Destination (Join-Path $tempDir $folder) -Recurse -Force }
  }

  Compress-Archive -Path (Join-Path $tempDir '*') -DestinationPath $zipFile -CompressionLevel Optimal -Force
  Write-Output $zipFile
}
finally {
  Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
