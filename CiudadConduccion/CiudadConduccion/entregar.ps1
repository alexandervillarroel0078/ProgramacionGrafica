# entregar.ps1: genera CiudadConduccion_entrega.zip con lo necesario para compilar, ejecutar y leer el proyecto.
# Uso (en PowerShell, desde cualquier carpeta):  powershell -ExecutionPolicy Bypass -File .\entregar.ps1
# Incluye: src/, pom.xml, README.md, ENTREGA.md y docs/.
# Excluye: target/, .idea/, .vscode/, .git/, .github/ y archivos temporales (*.tmp, *.bak, *.log, *~, .DS_Store, ...).

$ErrorActionPreference = 'Stop'                                   # Cualquier error detiene el script.
$raiz = $PSScriptRoot                                             # Carpeta del proyecto: donde está este script.
$nombreZip = 'CiudadConduccion_entrega.zip'                       # Nombre del archivo de entrega.
$destino = Join-Path $raiz $nombreZip                             # Se genera junto a pom.xml.
$incluir = @('src', 'pom.xml', 'README.md', 'ENTREGA.md', 'docs') # Lo único que entra en la entrega.
$carpetasExcluidas = @('target', '.idea', '.vscode', '.git', '.github') # Compilados, configuración de IDE y control de versiones.
$patronesTemporales = @('*.tmp', '*.temp', '*.bak', '*.swp', '*.swo', '*~', '*.log', '*.orig', '.DS_Store', 'Thumbs.db', 'desktop.ini') # Archivos temporales.

# Comprueba que todo lo que se quiere incluir exista (salvo docs/, que es opcional).
foreach ($elemento in $incluir) {
    if (-not (Test-Path (Join-Path $raiz $elemento)) -and $elemento -ne 'docs') {
        throw "Falta '$elemento' en $raiz"                         # Sin src/ o pom.xml no hay entrega posible.
    }
}

# Copia los archivos elegidos a una carpeta temporal, respetando las rutas y aplicando las exclusiones.
$temporal = Join-Path ([System.IO.Path]::GetTempPath()) ('entrega_' + [guid]::NewGuid())
$carpetaZip = Join-Path $temporal 'CiudadConduccion'              # Al descomprimir se obtiene una sola carpeta.
New-Item -ItemType Directory -Path $carpetaZip | Out-Null

function EsExcluido([string]$rutaRelativa) {
    $partes = $rutaRelativa -split '[\\/]'                         # Cada carpeta de la ruta.
    foreach ($parte in $partes) {
        if ($carpetasExcluidas -contains $parte) { return $true } # Está dentro de una carpeta excluida.
    }
    $nombre = $partes[-1]                                          # Nombre del archivo.
    foreach ($patron in $patronesTemporales) {
        if ($nombre -like $patron) { return $true }                # Es un archivo temporal.
    }
    return $false                                                  # Se incluye.
}

$copiados = 0                                                      # Cantidad de archivos copiados.
try {
    foreach ($elemento in $incluir) {
        $origen = Join-Path $raiz $elemento
        if (-not (Test-Path $origen)) { continue }                 # docs/ puede no existir todavía.
        $archivos = if (Test-Path $origen -PathType Container) { Get-ChildItem -Path $origen -Recurse -File -Force } else { Get-Item $origen }
        foreach ($archivo in $archivos) {
            $relativa = $archivo.FullName.Substring($raiz.Length).TrimStart('\', '/') # Ruta dentro del proyecto.
            if (EsExcluido $relativa) { continue }                 # Salta exclusiones y temporales.
            $copia = Join-Path $carpetaZip $relativa
            New-Item -ItemType Directory -Path (Split-Path $copia -Parent) -Force | Out-Null
            Copy-Item -LiteralPath $archivo.FullName -Destination $copia
            $copiados++
        }
    }

    # Crea el ZIP entrada por entrada. En PowerShell 5.1, CreateFromDirectory escribe las rutas con '\', que Linux y
    # macOS no interpretan como carpetas; aquí cada ruta se guarda con '/', el separador del formato ZIP.
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path $destino) { Remove-Item $destino -Force }        # Reemplaza una entrega anterior.
    $zip = [System.IO.Compression.ZipFile]::Open($destino, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($archivo in Get-ChildItem -Path $temporal -Recurse -File) {
            $entrada = $archivo.FullName.Substring($temporal.Length).TrimStart('\', '/') -replace '\\', '/' # 'CiudadConduccion/src/...'.
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $archivo.FullName, $entrada, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
    finally {
        $zip.Dispose()                                             # Cierra el ZIP para que quede completo en disco.
    }

    $tamano = [math]::Round((Get-Item $destino).Length / 1KB, 1)
    Write-Host "Listo: $destino ($copiados archivos, $tamano KB)"
}
finally {
    Remove-Item -Recurse -Force $temporal -ErrorAction SilentlyContinue # Borra la carpeta temporal pase lo que pase.
}
