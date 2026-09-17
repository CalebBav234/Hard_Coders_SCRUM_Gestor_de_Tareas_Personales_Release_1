[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Token
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker Desktop debe estar instalado y ejecutándose para usar el escáner incluido.'
}

$classes = Join-Path $projectRoot 'backend\target\classes'
if (-not (Test-Path -LiteralPath $classes)) {
    throw 'No existe backend\target\classes. Ejecuta primero: cd backend; mvn clean test'
}

docker run --rm `
    -e "SONAR_HOST_URL=http://host.docker.internal:9000" `
    -e "SONAR_TOKEN=$Token" `
    -v "${projectRoot}:/usr/src" `
    -w /usr/src `
    sonarsource/sonar-scanner-cli

if ($LASTEXITCODE -ne 0) {
    throw "El análisis falló (código $LASTEXITCODE). Revisa que SonarQube esté operativo en http://localhost:9000."
}
