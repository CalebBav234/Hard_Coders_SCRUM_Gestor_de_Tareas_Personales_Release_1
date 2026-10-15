param([Parameter(Mandatory = $true)][string]$PayloadPath)

Add-Type -AssemblyName System.Drawing
$data = Get-Content -Raw -Encoding UTF8 -LiteralPath $PayloadPath | ConvertFrom-Json
$rowHeight = 38
$width = 1920
$top = 320
$bottom = $top + $rowHeight * ($data.rows.Count + 1)
$height = $bottom + 105
$bitmap = [System.Drawing.Bitmap]::new($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([System.Drawing.Color]::White)

$blue = [System.Drawing.ColorTranslator]::FromHtml('#0B6EA8')
$green = [System.Drawing.ColorTranslator]::FromHtml('#14804A')
$orange = [System.Drawing.ColorTranslator]::FromHtml('#D87500')
$red = [System.Drawing.ColorTranslator]::FromHtml('#C62828')
$gray = [System.Drawing.ColorTranslator]::FromHtml('#596168')
$black = [System.Drawing.ColorTranslator]::FromHtml('#202124')
$light = [System.Drawing.ColorTranslator]::FromHtml('#F6F8FA')
$rule = [System.Drawing.ColorTranslator]::FromHtml('#D5D9DD')
$fontTitle = [System.Drawing.Font]::new('Segoe UI', 42, [System.Drawing.FontStyle]::Bold)
$fontSubtitle = [System.Drawing.Font]::new('Segoe UI', 22)
$fontCard = [System.Drawing.Font]::new('Segoe UI', 19, [System.Drawing.FontStyle]::Bold)
$fontValue = [System.Drawing.Font]::new('Segoe UI', 32, [System.Drawing.FontStyle]::Bold)
$fontDetail = [System.Drawing.Font]::new('Segoe UI', 16)
$fontHeader = [System.Drawing.Font]::new('Segoe UI', 15, [System.Drawing.FontStyle]::Bold)
$fontCell = [System.Drawing.Font]::new('Segoe UI', 14)
$fontResult = [System.Drawing.Font]::new('Segoe UI', 14, [System.Drawing.FontStyle]::Bold)
$fontNote = [System.Drawing.Font]::new('Segoe UI', 14)
$brushBlack = [System.Drawing.SolidBrush]::new($black)
$brushGray = [System.Drawing.SolidBrush]::new($gray)
$penRule = [System.Drawing.Pen]::new($rule, 2)
$penTable = [System.Drawing.Pen]::new($black, 1)

function Format-Coverage([int]$covered, [int]$missed, [int]$digits = 1) {
    $total = $covered + $missed
    if ($total -eq 0) { return 'N/A' }
    $format = "F$digits"
    return ('{0}%' -f (100.0 * $covered / $total).ToString($format, [Globalization.CultureInfo]::InvariantCulture))
}

$title = 'Cobertura din' + [char]0x00E1 + 'mica - JaCoCo'
$subtitle = 'US2.2 y US2.3 - m' + [char]0x00E9 + 'todos de construcci' + [char]0x00F3 + 'n WBS y asignaci' + [char]0x00F3 + 'n de personal'
$g.DrawString($title, $fontTitle, [System.Drawing.SolidBrush]::new($blue), 70, 32)
$g.DrawString($subtitle, $fontSubtitle, $brushGray, 70, 88)
$g.DrawLine($penRule, 70, 130, 1850, 130)
$cards = @(
    [pscustomobject]@{ Title = 'US2.2 - lineas'; Value = (Format-Coverage $data.us22_lines[0] $data.us22_lines[1]); Detail = "$($data.us22_lines[0])/$($data.us22_lines[0] + $data.us22_lines[1]) lineas"; Color = $orange },
    [pscustomobject]@{ Title = 'US2.3 - lineas'; Value = (Format-Coverage $data.us23_lines[0] $data.us23_lines[1]); Detail = "$($data.us23_lines[0])/$($data.us23_lines[0] + $data.us23_lines[1]) lineas"; Color = $orange },
    [pscustomobject]@{ Title = 'Suite backend'; Value = "$($data.total_tests) pruebas"; Detail = "$($data.failures) fallos - $($data.errors) errores"; Color = $green },
    [pscustomobject]@{ Title = 'Cobertura global backend'; Value = (Format-Coverage $data.overall_lines[0] $data.overall_lines[1] 2); Detail = "$($data.overall_lines[0])/$($data.overall_lines[0] + $data.overall_lines[1]) lineas"; Color = $orange }
)
for ($i = 0; $i -lt $cards.Count; $i++) {
    $x = 70 + $i * 445
    $rect = [System.Drawing.Rectangle]::new($x, 150, 405, 135)
    $g.FillRectangle([System.Drawing.SolidBrush]::new($light), $rect)
    $g.DrawRectangle($penRule, $rect)
    $g.DrawString($cards[$i].Title, $fontCard, $brushGray, $x + 20, 166)
    $g.DrawString($cards[$i].Value, $fontValue, [System.Drawing.SolidBrush]::new($cards[$i].Color), $x + 20, 202)
    $g.DrawString($cards[$i].Detail, $fontDetail, $brushGray, $x + 20, 250)
}
$g.DrawString('Detalle por metodo instrumentado en JaCoCo', [System.Drawing.Font]::new('Segoe UI', 21, [System.Drawing.FontStyle]::Bold), $brushGray, 70, 290)

$columns = @(70, 205, 830, 1140, 1480, 1850)
$g.DrawRectangle($penTable, 70, $top, 1780, $bottom - $top)
$g.FillRectangle([System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#E7E9EC')), 70, $top, 1780, $rowHeight)
$g.DrawRectangle($penTable, 70, $top, 1780, $rowHeight)
for ($i = 1; $i -lt $columns.Count - 1; $i++) { $g.DrawLine($penTable, $columns[$i], $top, $columns[$i], $bottom) }
for ($i = 1; $i -le $data.rows.Count; $i++) { $g.DrawLine($penTable, 70, $top + $rowHeight * $i, 1850, $top + $rowHeight * $i) }
$headers = @('US', 'Metodo', 'Lineas', 'Ramas', 'Resultado')
for ($i = 0; $i -lt $headers.Count; $i++) { $g.DrawString($headers[$i], $fontHeader, $brushBlack, $columns[$i] + 12, $top + 10) }

for ($i = 0; $i -lt $data.rows.Count; $i++) {
    $r = $data.rows[$i]
    $y = $top + $rowHeight * ($i + 1) + 9
    $lineTotal = [int]$r.line_covered + [int]$r.line_missed
    $branchTotal = [int]$r.branch_covered + [int]$r.branch_missed
    $lineText = if ($lineTotal) { '{0}/{1} ({2}%)' -f $r.line_covered, $lineTotal, [math]::Round(100 * $r.line_covered / $lineTotal) } else { 'N/A' }
    $branchText = if ($branchTotal) { '{0}/{1} ({2}%)' -f $r.branch_covered, $branchTotal, [math]::Round(100 * $r.branch_covered / $branchTotal) } else { 'N/A' }
    $resultColor = switch ($r.result) { 'CUBIERTO' { $green } 'SIN COBERTURA' { $red } default { $orange } }
    $values = @($r.story, $r.method, $lineText, $branchText, $r.result)
    for ($col = 0; $col -lt $values.Count; $col++) {
        $brush = if ($col -eq 4) { [System.Drawing.SolidBrush]::new($resultColor) } else { $brushBlack }
        $font = if ($col -eq 4) { $fontResult } else { $fontCell }
        $g.DrawString([string]$values[$col], $font, $brush, $columns[$col] + 12, $y)
    }
}
$notes = $bottom + 14
$g.DrawString("Pruebas nuevas de US2.2/US2.3: $($data.new_tests) - Suite completa: $($data.total_tests)", $fontNote, $brushGray, 70, $notes)
$g.DrawString('La cobertura por metodo usa toda la suite; el total global incluye todo el backend.', $fontNote, $brushGray, 70, $notes + 22)
$g.DrawString('Fuente: backend/target/site/jacoco/jacoco.xml - backend/target/site/jacoco/index.html', $fontNote, $brushGray, 70, $notes + 44)

$parent = Split-Path -Parent $data.output
New-Item -ItemType Directory -Force -Path $parent | Out-Null
$bitmap.Save($data.output, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bitmap.Dispose()
Write-Output $data.output
