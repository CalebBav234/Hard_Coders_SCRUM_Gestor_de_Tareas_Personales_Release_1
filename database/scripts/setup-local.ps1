[CmdletBinding()]
param(
    [string]$HostName = 'localhost',
    [ValidateRange(1, 65535)]
    [int]$Port = 5432,
    [string]$AdminUser = 'postgres',
    [string]$DatabaseName = 'gestor_tareas',
    [string]$OwnerRole = 'gestor_tareas_owner',
    [string]$AppRole = 'gestor_tareas_app',
    [Security.SecureString]$AdminPassword,
    [Security.SecureString]$OwnerPassword,
    [Security.SecureString]$AppPassword,
    [string]$PsqlPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-PsqlPath {
    param([string]$ExplicitPath)

    if ($ExplicitPath) {
        if (-not (Test-Path -LiteralPath $ExplicitPath -PathType Leaf)) {
            throw "No se encontró psql en: $ExplicitPath"
        }
        return (Resolve-Path -LiteralPath $ExplicitPath).Path
    }

    $command = Get-Command psql -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidate = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        return $candidate
    }

    throw 'No se encontró psql. Instala PostgreSQL 18 o usa -PsqlPath.'
}

function Assert-SqlIdentifier {
    param([string]$Value, [string]$Label)

    if ($Value -notmatch '^[a-z][a-z0-9_]{0,62}$') {
        throw "$Label debe usar letras minúsculas, números o guion bajo y comenzar con letra."
    }
}

function ConvertTo-PlainText {
    param([Security.SecureString]$SecureValue)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Escape-SqlLiteral {
    param([string]$Value)
    return $Value.Replace("'", "''")
}

Assert-SqlIdentifier -Value $DatabaseName -Label 'DatabaseName'
Assert-SqlIdentifier -Value $OwnerRole -Label 'OwnerRole'
Assert-SqlIdentifier -Value $AppRole -Label 'AppRole'
if ($OwnerRole -eq $AppRole) {
    throw 'OwnerRole y AppRole deben ser roles distintos.'
}

$psql = Resolve-PsqlPath -ExplicitPath $PsqlPath
if (-not $AdminPassword) {
    $AdminPassword = Read-Host "Contraseña del usuario administrador '$AdminUser'" -AsSecureString
}
if (-not $OwnerPassword) {
    $OwnerPassword = Read-Host "Nueva contraseña para '$OwnerRole'" -AsSecureString
}
if (-not $AppPassword) {
    $AppPassword = Read-Host "Nueva contraseña para '$AppRole'" -AsSecureString
}

$adminPlain = ConvertTo-PlainText $AdminPassword
$ownerPlain = ConvertTo-PlainText $OwnerPassword
$appPlain = ConvertTo-PlainText $AppPassword

try {
    $ownerPasswordSql = Escape-SqlLiteral $ownerPlain
    $appPasswordSql = Escape-SqlLiteral $appPlain

    $rolesSql = @"
SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
    '$OwnerRole', '$ownerPasswordSql'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$OwnerRole')
\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', '$OwnerRole', '$ownerPasswordSql')
\gexec

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
    '$AppRole', '$appPasswordSql'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$AppRole')
\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', '$AppRole', '$appPasswordSql')
\gexec
"@

    $env:PGPASSWORD = $adminPlain
    $rolesSql | & $psql -X -h $HostName -p $Port -U $AdminUser -d postgres -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudieron crear o actualizar los roles de PostgreSQL.'
    }

    $databaseSql = @"
SELECT format(
    'CREATE DATABASE %I OWNER %I ENCODING ''UTF8'' TEMPLATE template0',
    '$DatabaseName', '$OwnerRole'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '$DatabaseName')
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', '$DatabaseName', '$OwnerRole')
\gexec
SELECT format('ALTER DATABASE %I SET timezone TO ''UTC''', '$DatabaseName')
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', '$DatabaseName', '$AppRole')
\gexec
"@

    $databaseSql | & $psql -X -h $HostName -p $Port -U $AdminUser -d postgres -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudo crear o configurar la base de datos.'
    }

    $migrationScript = Join-Path $PSScriptRoot 'migrate.ps1'
    & $migrationScript `
        -HostName $HostName `
        -Port $Port `
        -DatabaseName $DatabaseName `
        -Username $OwnerRole `
        -Password $OwnerPassword `
        -PsqlPath $psql

    Write-Host ''
    Write-Host 'Base de datos lista.' -ForegroundColor Green
    Write-Host "Servidor: $HostName`:$Port"
    Write-Host "Base: $DatabaseName"
    Write-Host 'Esquema: task_manager'
    Write-Host "Usuario del backend: $AppRole"
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    $adminPlain = $null
    $ownerPlain = $null
    $appPlain = $null
}
