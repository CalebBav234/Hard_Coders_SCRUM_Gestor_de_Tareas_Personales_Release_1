[CmdletBinding()]
param(
    [string]$HostName = 'localhost',
    [ValidateRange(1, 65535)]
    [int]$Port = 5432,
    [string]$DatabaseName = 'gestor_tareas',
    [string]$Username = 'gestor_tareas_app',
    [Security.SecureString]$Password,
    [string]$PsqlPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($PsqlPath) {
    if (-not (Test-Path -LiteralPath $PsqlPath -PathType Leaf)) {
        throw "No se encontró psql en: $PsqlPath"
    }
    $psql = (Resolve-Path -LiteralPath $PsqlPath).Path
}
else {
    $command = Get-Command psql -ErrorAction SilentlyContinue
    $candidate = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
    if ($command) {
        $psql = $command.Source
    }
    elseif (Test-Path -LiteralPath $candidate -PathType Leaf) {
        $psql = $candidate
    }
    else {
        throw 'No se encontró psql. Instala PostgreSQL 18 o usa -PsqlPath.'
    }
}

if (-not $Password) {
    $Password = Read-Host "Contraseña de '$Username'" -AsSecureString
}

$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password)
try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
}

$env:PGPASSWORD = $plainPassword
$verificationFile = Join-Path $PSScriptRoot 'verify_schema.sql'

try {
    & $psql -X -h $HostName -p $Port -U $Username -d $DatabaseName `
        -v ON_ERROR_STOP=1 -f $verificationFile

    if ($LASTEXITCODE -ne 0) {
        throw 'La verificación de la base de datos falló.'
    }

    Write-Host 'Verificación completada correctamente.' -ForegroundColor Green
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    $plainPassword = $null
}
