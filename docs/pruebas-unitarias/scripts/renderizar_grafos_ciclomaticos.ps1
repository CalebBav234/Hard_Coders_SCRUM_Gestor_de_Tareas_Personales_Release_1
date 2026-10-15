param([Parameter(Mandatory = $true)][string]$PayloadPath)

Add-Type -AssemblyName System.Drawing
$payload = Get-Content -Raw -Encoding UTF8 -LiteralPath $PayloadPath | ConvertFrom-Json
$outputDir = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) '..\evidencias\grafos'
$outputDir = [System.IO.Path]::GetFullPath($outputDir)
$blue = [System.Drawing.ColorTranslator]::FromHtml('#0B6EA8')
$lightBlue = [System.Drawing.ColorTranslator]::FromHtml('#EAF5FB')
$orange = [System.Drawing.ColorTranslator]::FromHtml('#F6A623')
$red = [System.Drawing.ColorTranslator]::FromHtml('#E21B23')
$gray = [System.Drawing.ColorTranslator]::FromHtml('#55585D')
$lightGray = [System.Drawing.ColorTranslator]::FromHtml('#F4F5F6')
$black = [System.Drawing.ColorTranslator]::FromHtml('#202124')
$white = [System.Drawing.Color]::White
$titleFont = [System.Drawing.Font]::new('Segoe UI', 42, [System.Drawing.FontStyle]::Bold)
$subtitleFont = [System.Drawing.Font]::new('Segoe UI', 24, [System.Drawing.FontStyle]::Bold)
$bodyFont = [System.Drawing.Font]::new('Segoe UI', 20)
$smallFont = [System.Drawing.Font]::new('Segoe UI', 17)
$tableFont = [System.Drawing.Font]::new('Segoe UI', 17)
$tableBoldFont = [System.Drawing.Font]::new('Segoe UI', 17, [System.Drawing.FontStyle]::Bold)
$codeFont = [System.Drawing.Font]::new('Consolas', 20)
$nodeFont = [System.Drawing.Font]::new('Segoe UI', 19, [System.Drawing.FontStyle]::Bold)
$pen = [System.Drawing.Pen]::new($black, 3)
$thinPen = [System.Drawing.Pen]::new($black, 2)
$headerPen = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml('#D7DADD'), 3)
$brushBlack = [System.Drawing.SolidBrush]::new($black)
$brushGray = [System.Drawing.SolidBrush]::new($gray)
$brushBlue = [System.Drawing.SolidBrush]::new($blue)
$brushWhite = [System.Drawing.SolidBrush]::new($white)
$brushOrange = [System.Drawing.SolidBrush]::new($orange)
$brushRed = [System.Drawing.SolidBrush]::new($red)

function Draw-Arrow($Graphics, [string]$From, [string]$To, [string]$Label, $Bend, $Nodes) {
    $coords = [System.Collections.Generic.List[System.Drawing.PointF]]::new()
    $startNode = $Nodes.PSObject.Properties[$From].Value
    $endNode = $Nodes.PSObject.Properties[$To].Value
    $coords.Add([System.Drawing.PointF]::new([single]$startNode[0], [single]$startNode[1]))
    if ($null -ne $Bend) {
        foreach ($point in $Bend) { $coords.Add([System.Drawing.PointF]::new([single]$point[0], [single]$point[1])) }
    }
    $coords.Add([System.Drawing.PointF]::new([single]$endNode[0], [single]$endNode[1]))
    $points = $coords.ToArray()
    $Graphics.DrawLines($thinPen, $points)
    $a = $points[$points.Length - 2]
    $b = $points[$points.Length - 1]
    $dx = $b.X - $a.X
    $dy = $b.Y - $a.Y
    $length = [Math]::Max([Math]::Sqrt($dx * $dx + $dy * $dy), 1)
    $ux = $dx / $length
    $uy = $dy / $length
    $px = -$uy
    $py = $ux
    $size = 12
    $triangle = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new($b.X, $b.Y),
        [System.Drawing.PointF]::new($b.X - $ux * $size + $px * $size * 0.55, $b.Y - $uy * $size + $py * $size * 0.55),
        [System.Drawing.PointF]::new($b.X - $ux * $size - $px * $size * 0.55, $b.Y - $uy * $size - $py * $size * 0.55)
    )
    $Graphics.FillPolygon($brushBlack, $triangle)
    if ($Label) {
        $mx = ($a.X + $b.X) / 2 + 7
        $my = ($a.Y + $b.Y) / 2 - 22
        $Graphics.DrawString($Label, $smallFont, $brushBlue, $mx, $my)
    }
}

function Draw-Node($Graphics, [string]$Label, $Position) {
    $x = [single]$Position[0]
    $y = [single]$Position[1]
    $radius = if ($Label.Length -le 2) { 25 } else { 30 }
    $fill = if ($Label -in @('I', 'F')) { $white } else { $lightBlue }
    $brush = [System.Drawing.SolidBrush]::new($fill)
    $rect = [System.Drawing.RectangleF]::new($x - $radius, $y - $radius, $radius * 2, $radius * 2)
    $Graphics.FillEllipse($brush, $rect)
    $Graphics.DrawEllipse($pen, $rect)
    $size = $Graphics.MeasureString($Label, $nodeFont)
    $Graphics.DrawString($Label, $nodeFont, $brushBlack, $x - $size.Width / 2, $y - $size.Height / 2 - 2)
    $brush.Dispose()
}

function Wrap-Cell([string]$Text, [int]$MaxChars) {
    $words = $Text -split '\s+'
    $lines = [System.Collections.Generic.List[string]]::new()
    $line = ''
    foreach ($word in $words) {
        if ($line -and ($line.Length + 1 + $word.Length) -gt $MaxChars) {
            $lines.Add($line)
            $line = $word
        } else {
            $line = if ($line) { "$line $word" } else { $word }
        }
    }
    if ($line) { $lines.Add($line) }
    return @($lines | Select-Object -First 2)
}

foreach ($spec in $payload.specs) {
    $bitmap = [System.Drawing.Bitmap]::new(1920, 1080)
    $g = [System.Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear($white)
    $storyX = 850
    $g.DrawString('Caja Blanca - Caminos Basicos', $titleFont, $brushBlue, 55, 40)
    $g.DrawString("$($spec.story) - $($spec.unit)", [System.Drawing.Font]::new('Segoe UI', 27, [System.Drawing.FontStyle]::Bold), $brushGray, $storyX, 54)
    $g.DrawLine($headerPen, 55, 112, 1865, 112)

    $panel = [System.Drawing.Rectangle]::new(55, 150, 815, 475)
    $g.FillRectangle([System.Drawing.SolidBrush]::new($lightGray), $panel)
    $g.DrawRectangle([System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml('#D3D6DA'), 2), $panel)
    $g.DrawString('Codigo de la unidad', $subtitleFont, $brushGray, 80, 168)
    $y = 215
    foreach ($line in $spec.code) {
        if ($line[0] -gt 0) {
            $circleBrush = if ($line[2]) { $brushOrange } else { [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#B7BEC5')) }
            $g.FillEllipse($circleBrush, 70, $y - 2, 30, 30)
            $n = [string]$line[0]
            $nsize = $g.MeasureString($n, $smallFont)
            $g.DrawString($n, $smallFont, $brushWhite, 85 - $nsize.Width / 2, $y + 1)
        }
        $g.DrawString([string]$line[1], $codeFont, $brushBlue, 113, $y)
        $y += 37
    }

    $g.DrawString('Grafo de flujo de control', $subtitleFont, $brushGray, 1400, 142)
    foreach ($edge in $spec.edges) {
        Draw-Arrow $g ([string]$edge[0]) ([string]$edge[1]) ([string]$edge[2]) $edge[3] $spec.nodes
    }
    foreach ($entry in $spec.nodes.PSObject.Properties) { Draw-Node $g $entry.Name $entry.Value }
    $g.FillRectangle($brushWhite, 900, 510, 260, 105)
    $g.DrawRectangle([System.Drawing.Pen]::new($red, 4), 900, 510, 260, 105)
    $g.DrawString("V(G) = $($spec.vg)", [System.Drawing.Font]::new('Segoe UI', 34, [System.Drawing.FontStyle]::Bold), $brushBlack, 930, 526)
    $g.DrawString([string]$spec.formula, $smallFont, $brushGray, 921, 574)
    $g.DrawString('Casos derivados de los caminos independientes', $subtitleFont, $brushGray, 55, 638)

    $x0, $y0, $x1 = 55, 675, 1865
    $headerHeight = 46
    $rowHeight = [Math]::Min(58, [int](360 / [Math]::Max($spec.rows.Count, 1)))
    $widths = @(90, 470, 760, 490)
    $xs = [System.Collections.Generic.List[int]]::new()
    $xs.Add($x0)
    foreach ($width in $widths) { $xs.Add($xs[$xs.Count - 1] + $width) }
    $y1 = $y0 + $headerHeight + $rowHeight * $spec.rows.Count
    $g.DrawRectangle($thinPen, $x0, $y0, $x1 - $x0, $y1 - $y0)
    $g.FillRectangle([System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#E7E9EC')), $x0, $y0, $x1 - $x0, $headerHeight)
    $g.DrawRectangle($thinPen, $x0, $y0, $x1 - $x0, $headerHeight)
    for ($i = 1; $i -lt $xs.Count - 1; $i++) { $g.DrawLine($thinPen, $xs[$i], $y0, $xs[$i], $y1) }
    for ($i = 0; $i -le $spec.rows.Count; $i++) { $g.DrawLine($thinPen, $x0, $y0 + $headerHeight + $i * $rowHeight, $x1, $y0 + $headerHeight + $i * $rowHeight) }
    $headers = @('Nro', 'Camino', 'Entrada', 'Salida esperada')
    for ($c = 0; $c -lt 4; $c++) {
        $measure = $g.MeasureString($headers[$c], $tableBoldFont)
        $g.DrawString($headers[$c], $tableBoldFont, $brushBlack, $xs[$c] + (($xs[$c + 1] - $xs[$c] - $measure.Width) / 2), $y0 + 10)
    }
    $limits = @(5, 44, 76, 48)
    for ($r = 0; $r -lt $spec.rows.Count; $r++) {
        $values = $spec.rows[$r]
        $rowY = $y0 + $headerHeight + $r * $rowHeight
        for ($c = 0; $c -lt 4; $c++) {
            $lines = Wrap-Cell ([string]$values[$c]) $limits[$c]
            $textHeight = 20 * $lines.Count
            $ty = $rowY + [Math]::Max(5, ($rowHeight - $textHeight) / 2)
            foreach ($text in $lines) {
                if ($c -eq 0) {
                    $measure = $g.MeasureString($text, $tableFont)
                    $tx = $xs[$c] + (($xs[$c + 1] - $xs[$c] - $measure.Width) / 2)
                } else { $tx = $xs[$c] + 10 }
                $g.DrawString($text, $tableFont, $brushBlack, $tx, $ty)
                $ty += 20
            }
        }
    }
    $note = [string]$spec.note
    $g.DrawString($note, $smallFont, $brushGray, 55, 990)
    $g.DrawString('Hard Coders - Evidencia US2.2 / US2.3', $smallFont, [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#7B8085')), 1515, 1044)

    $outputPath = [string]$spec.output
    $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output $outputPath
    $g.Dispose()
    $bitmap.Dispose()
}
