# Local dev preparation for IntelliJ IDEA (no extra CMD windows for Java services).
# Does: optional Docker, mvn install, optional env helper output.
#
# Typical:  .\scripts\dev-up.ps1
#           .\scripts\dev-up.ps1 -Docker
#           .\scripts\print-idea-env.ps1   # paste output into IDEA Run -> Environment variables

param(
  [switch]$Docker,
  [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$envFile = Join-Path $root "deploy\.env"
if (Test-Path $envFile) {
  Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $i = $line.IndexOf("=")
    if ($i -lt 1) { return }
    $k = $line.Substring(0, $i).Trim()
    $v = $line.Substring($i + 1).Trim()
    [Environment]::SetEnvironmentVariable($k, $v, "Process")
  }
}
if ($env:MYSQL_ROOT_PASSWORD -and -not $env:MYSQL_PASSWORD) {
  $env:MYSQL_PASSWORD = $env:MYSQL_ROOT_PASSWORD
}
if (-not $env:REDIS_HOST) { $env:REDIS_HOST = "127.0.0.1" }
if (-not $env:REDIS_PORT) { $env:REDIS_PORT = "6379" }

Set-Location $root
$mvnExe = (Get-Command mvn -ErrorAction Stop).Source

if ($Docker) {
  Write-Host "Starting MySQL + Redis (docker compose)..."
  $compose = Join-Path $root "deploy\docker-compose.yml"
  $envLocal = Join-Path $root "deploy\.env"
  if (-not (Test-Path $envLocal)) { throw "deploy/.env missing (see deploy/env.example)" }
  & docker compose -f $compose --env-file $envLocal up -d mysql redis
  if ($LASTEXITCODE -ne 0) { throw "docker compose failed" }
  Write-Host "Waiting for MySQL to accept connections..."
  Start-Sleep -Seconds 5
}

if (-not $SkipInstall) {
  Write-Host "mvn install -DskipTests (build common libs once, required before IDEA Run)..."
  & $mvnExe -q install -DskipTests
  if ($LASTEXITCODE -ne 0) { throw "mvn install failed" }
} else {
  Write-Host "Skipped mvn install (-SkipInstall)."
}

Write-Host ""
Write-Host "========== Next: start services in IDEA =========="
Write-Host "1) Open repo root pom.xml as Maven project (File -> Open)."
Write-Host "2) Each service: open *Application.java -> right-click green Run icon on main() or class."
Write-Host "   Maven module / main class (manual order if not Compound):"
Write-Host "      user-center-service  -> UserCenterApplication     (8101)"
Write-Host "      billing-service      -> BillingApplication         (8103)"
Write-Host "      adapter-service      -> AdapterApplication         (8102)"
Write-Host "      payment-service      -> PaymentApplication         (8104)"
Write-Host "      ops-console          -> OpsConsoleApplication      (8105)"
Write-Host "      gateway-service      -> GatewayApplication         (8080)  <- start last"
Write-Host "3) Optional: Run -> Edit Configurations -> + Compound -> add the six above in that order."
Write-Host "4) Environment: same variables for all six (from deploy/.env). Paste output of:"
Write-Host "      powershell -File scripts\print-idea-env.ps1"
Write-Host "   into each configuration -> Modify options -> Environment variables (or use EnvFile plugin on deploy/.env)."
Write-Host "5) Frontend: Terminal in repo: cd console-web && npm run dev  -> http://localhost:5173"
Write-Host "========== Ports =========="
Write-Host "gateway-service 8080 | user-center-service 8101 | adapter-service 8102 | billing-service 8103 | payment-service 8104 | ops-console 8105 | console-web (Vite) 5173"
Write-Host ""
