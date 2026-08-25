[CmdletBinding()]
param(
    [string]$HostName = 'localhost',
    [ValidateRange(1, 65535)]
    [int]$Port = 5432,
    [string]$DatabaseName = 'gestor_tareas',
    [string]$Username = 'gestor_tareas_owner',
    [Security.SecureString]$Password,
    [string]$PsqlPath,
    [string]$MigrationsPath = (Join-Path $PSScriptRoot '..\migrations')
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

$psql = Resolve-PsqlPath -ExplicitPath $PsqlPath
$resolvedMigrations = (Resolve-Path -LiteralPath $MigrationsPath).Path

if (-not $Password) {
    $Password = Read-Host "Contraseña de '$Username'" -AsSecureString
}

$plainPassword = ConvertTo-PlainText $Password
$env:PGPASSWORD = $plainPassword

try {
    $migrationTableSql = @"
CREATE TABLE IF NOT EXISTS public.database_schema_migrations (
    version       integer PRIMARY KEY,
    description   varchar(200) NOT NULL,
    checksum      char(64) NOT NULL,
    applied_at    timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_by    name NOT NULL DEFAULT CURRENT_USER
);
REVOKE ALL ON public.database_schema_migrations FROM PUBLIC;
"@

    $migrationTableSql | & $psql -X -h $HostName -p $Port -U $Username -d $DatabaseName -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudo preparar el control de migraciones.'
    }

    $files = Get-ChildItem -LiteralPath $resolvedMigrations -File -Filter 'V*.sql' |
        Sort-Object {
            if ($_.Name -match '^V(?<version>\d+)__') {
                [int]$Matches.version
            }
            else {
                [int]::MaxValue
            }
        }

    foreach ($file in $files) {
        if ($file.Name -notmatch '^V(?<version>\d+)__(?<description>.+)\.sql$') {
            throw "Nombre de migración inválido: $($file.Name)"
        }

        $version = [int]$Matches.version
        $description = $Matches.description.Replace('_', ' ')
        $checksum = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()

        $existingChecksum = & $psql -X -A -t -q `
            -h $HostName -p $Port -U $Username -d $DatabaseName `
            -v ON_ERROR_STOP=1 `
            -c "SELECT checksum FROM public.database_schema_migrations WHERE version = $version;"

        if ($LASTEXITCODE -ne 0) {
            throw "No se pudo consultar la migración V$version."
        }

        $existingChecksum = ($existingChecksum | Out-String).Trim()
        if ($existingChecksum) {
            if ($existingChecksum -ne $checksum) {
                throw "La migración V$version ya fue aplicada pero su archivo cambió. Restaura el archivo original."
            }
            Write-Host "V$version ya aplicada; se omite."
            continue
        }

        $descriptionSql = $description.Replace("'", "''")
        $insertSql = "INSERT INTO public.database_schema_migrations " +
            "(version, description, checksum) VALUES " +
            "($version, '$descriptionSql', '$checksum');"

        Write-Host "Aplicando V$version - $description..."
        & $psql -X -h $HostName -p $Port -U $Username -d $DatabaseName `
            -v ON_ERROR_STOP=1 --single-transaction `
            -f $file.FullName -c $insertSql

        if ($LASTEXITCODE -ne 0) {
            throw "Falló la migración V$version. La transacción fue revertida."
        }
    }

    Write-Host 'Migraciones al día.' -ForegroundColor Green
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    $plainPassword = $null
}
