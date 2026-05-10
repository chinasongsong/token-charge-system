# Read deploy/.env and print one line suitable for IntelliJ "Environment variables" (semicolon-separated on Windows).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$envFile = Join-Path $root "deploy\.env"
if (-not (Test-Path $envFile)) {
  Write-Host "No deploy/.env; set MYSQL_PASSWORD, JWT_SECRET, BILLING_INTERNAL_TOKEN in IDEA yourself."
  exit 0
}

$map = @{}
Get-Content $envFile | ForEach-Object {
  $line = $_.Trim()
  if ($line -eq "" -or $line.StartsWith("#")) { return }
  $i = $line.IndexOf("=")
  if ($i -lt 1) { return }
  $k = $line.Substring(0, $i).Trim()
  $v = $line.Substring($i + 1).Trim()
  $map[$k] = $v
}

if ($map["MYSQL_ROOT_PASSWORD"] -and -not $map["MYSQL_PASSWORD"]) {
  $map["MYSQL_PASSWORD"] = $map["MYSQL_ROOT_PASSWORD"]
}
if (-not $map["REDIS_HOST"]) { $map["REDIS_HOST"] = "127.0.0.1" }
if (-not $map["REDIS_PORT"]) { $map["REDIS_PORT"] = "6379" }

$parts = New-Object System.Collections.Generic.List[string]
foreach ($k in ($map.Keys | Sort-Object)) {
  $v = $map[$k]
  $v2 = $v -replace ';', '\;'
  $parts.Add("$k=$v2")
}
Write-Host ($parts -join ";")
