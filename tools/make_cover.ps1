# 生成无限箭袋模组的封面图
#   powershell -ExecutionPolicy Bypass -File tools\make_cover.ps1            → 只有一个箭袋（默认）
#   powershell -ExecutionPolicy Bypass -File tools\make_cover.ps1 -Mats      → 额外生成带合成材料行版（*_mats.png）
#   powershell -ExecutionPolicy Bypass -File tools\make_cover.ps1 -Text      → 额外生成带文字版（*_text.png）
# 依赖：Windows PowerShell 5.1 + System.Drawing（无需 Pillow）
param([switch]$Text, [switch]$Mats)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$ROOT   = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$ICON   = Join-Path $ROOT 'src\main\resources\assets\endlessquiver\textures\item\quiver.png'
$OUTDIR = Join-Path $ROOT 'cover'
if (-not (Test-Path $OUTDIR)) { New-Item -ItemType Directory -Path $OUTDIR | Out-Null }

$TITLE    = '无限箭袋'
$SUBTITLE = 'Endless Quiver'
$INFO     = 'Minecraft 1.20.1  ·  Forge  ·  需要 Curios API'
$BULLETS  = @(
  '记录一种箭矢，此后无限供应',
  '原版箭 / 药水箭 / 模组箭都能记',
  '手持右键打开记录界面（任意箭矢）',
  '下界之星 + 回响碎片 + 末影之眼 + 下界合金'
)
$FOOTER   = 'endlessquiver  ·  Curios 背饰'
$MATFILES = @('nether_star', 'echo_shard', 'ender_eye', 'netherite_ingot', 'netherite_block') |
        ForEach-Object { Join-Path (Join-Path $OUTDIR 'materials') "$_.png" }

# 材料行：5 个原版材质 + 之间的小十字（图形，不用字体）
function Draw-MatsRow($g, [float]$x, [float]$y, [int]$ms) {
  $gap = $ms * 7
  $cx  = $x
  $i   = 0
  $teal = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(210, 47, 191, 159))
  $bw = [float]($gap * 0.46)
  $bh = [float]($ms * 16 * 0.055)
  foreach ($mf in $MATFILES) {
    if ($i -gt 0) {
      $by = $y + $ms * 8
      $g.FillRectangle($teal, [float]$cx, [float]($by - $bh / 2), $bw, $bh)
      $g.FillRectangle($teal, [float]($cx + $bw / 2 - $bh / 2), [float]($by - $bw / 2), $bh, $bw)
      $cx += $gap
    }
    $img = [System.Drawing.Image]::FromFile($mf)
    $mr  = New-Object System.Drawing.Rectangle -ArgumentList @([int]$cx, [int]$y, [int]($ms * 16), [int]($ms * 16))
    $g.DrawImage($img, $mr, 0, 0, $img.Width, $img.Height, [System.Drawing.GraphicsUnit]::Pixel)
    $img.Dispose()
    $cx += $ms * 16
    $i++
  }
  $teal.Dispose()
}

function Get-MatsRowWidth([int]$ms) { return 5 * $ms * 16 + 4 * ($ms * 7) }

# $Mode: 'plain' 只有一个箭袋 / 'mats' 箭袋 + 材料行 / 'text' 箭袋 + 文字栏
function New-Cover([int]$W, [int]$H, [string]$Out, [string]$Mode) {
  $bmp = New-Object System.Drawing.Bitmap($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g   = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
  $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

  $rect = New-Object System.Drawing.Rectangle(0, 0, $W, $H)
  # 1) 背景渐变（深青黑 → 青灰）
  $lg = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect,
        [System.Drawing.Color]::FromArgb(255, 7, 18, 24),
        [System.Drawing.Color]::FromArgb(255, 20, 52, 58), 62.0)
  $g.FillRectangle($lg, $rect)

  # 2) 类 MC 方块噪点（大格子、低对比）
  $rnd  = New-Object System.Random(20260928)
  $cell = [int]($H / 40)
  for ($y = 0; $y -lt $H; $y += $cell) {
    for ($x = 0; $x -lt $W; $x += $cell) {
      $d = $rnd.Next(-7, 9)
      if ($d -gt 0) { $col = [System.Drawing.Color]::FromArgb(7 + $d, 255, 255, 255) }
      else { $col = [System.Drawing.Color]::FromArgb(9 + (-$d), 0, 0, 0) }
      $b = New-Object System.Drawing.SolidBrush($col)
      $g.FillRectangle($b, $x, $y, $cell, $cell)
      $b.Dispose()
    }
  }

  # 3) 图标位置与尺寸（整数倍放大，保证像素锐利）
  switch ($Mode) {
    'text' {
      $iconH = [int]($H * 0.56)
      $iconX = [int]($W * 0.055)
      $iconY = [int](($H - $iconH) / 2)
    }
    'mats' {
      $iconH = [int][Math]::Floor($H * 0.68 / 16) * 16
      $iconX = [int](($W - $iconH) / 2)
      $iconY = [int]($H * 0.055)
    }
    default {
      $iconH = [int][Math]::Floor($H * 0.78 / 16) * 16
      $iconX = [int](($W - $iconH) / 2)
      $iconY = [int](($H - $iconH) / 2)
    }
  }

  # 4) 图标外围的柔光（同心椭圆，由外向内加浓）
  $halo = if ($Mode -eq 'plain') { 0.62 } else { 0.95 }
  for ($i = 26; $i -ge 1; $i--) {
    $ex = $iconX + $iconH / 2 - ($iconH * $halo) * $i / 26
    $ey = $iconY + $iconH / 2 - ($iconH * $halo) * $i / 26
    $a  = [int](2 + 3 * (27 - $i) / 26 * 4)
    $b  = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb([Math]::Min(26, $a + 4), 47, 191, 159))
    $g.FillEllipse($b, [float]$ex, [float]$ey, [float]($iconH * 2 * $halo * $i / 26), [float]($iconH * 2 * $halo * $i / 26))
    $b.Dispose()
  }

  # 5) 图标投影（只有箭袋的版本不加）+ 图标本体
  $icon = [System.Drawing.Image]::FromFile($ICON)
  if ($Mode -ne 'plain') {
    $sh = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(90, 0, 0, 0))
    $g.FillEllipse($sh, [float]($iconX - $iconH * 0.10), [float]($iconY + $iconH * 0.80),
                   [float]($iconH * 1.20), [float]($iconH * 0.26))
    $sh.Dispose()
  }
  $irec = New-Object System.Drawing.Rectangle -ArgumentList @($iconX, $iconY, $iconH, $iconH)
  $g.DrawImage($icon, $irec, 0, 0, $icon.Width, $icon.Height, [System.Drawing.GraphicsUnit]::Pixel)

  $ms = [Math]::Max(2, [int][Math]::Floor($H * 0.062 / 16))

  if ($Mode -eq 'mats') {
    Draw-MatsRow $g ([float](($W - (Get-MatsRowWidth $ms)) / 2)) ([float]([int]($H * 0.80))) $ms
  }

  if ($Mode -eq 'text') {
    $textX = $iconX + $iconH + [int]($W * 0.045)
    $gx0 = [int]($textX - $W * 0.06)
    $gxW = [int]($W - $gx0)
    $gradRect = New-Object System.Drawing.Rectangle -ArgumentList @($gx0, 0, $gxW, $H)
    $gx = New-Object System.Drawing.Drawing2D.LinearGradientBrush($gradRect,
          [System.Drawing.Color]::FromArgb(0, 0, 0, 0), [System.Drawing.Color]::FromArgb(96, 0, 0, 0), 0.0)
    $g.FillRectangle($gx, $gradRect)

    $famTitle = New-Object System.Drawing.FontFamily('Microsoft YaHei')
    $famLatin = New-Object System.Drawing.FontFamily('Segoe UI')
    $fTitle = New-Object System.Drawing.Font($famTitle, [float]($H * 0.105), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $fSub   = New-Object System.Drawing.Font($famLatin, [float]($H * 0.042), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $fInfo  = New-Object System.Drawing.Font($famTitle, [float]($H * 0.026), [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $fBul   = New-Object System.Drawing.Font($famTitle, [float]($H * 0.029), [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $fFoot  = New-Object System.Drawing.Font($famLatin, [float]($H * 0.021), [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $white  = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 240, 253, 250))
    $teal   = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 47, 191, 159))
    $mint   = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 168, 255, 224))
    $gray   = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 150, 178, 180))
    $shadow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(190, 0, 0, 0))

    $y = [int]($H * 0.20)
    $g.DrawString($TITLE, $fTitle, $shadow, [float]($textX + $H * 0.004), [float]($y + $H * 0.005))
    $g.DrawString($TITLE, $fTitle, $white,  [float]$textX, [float]$y)
    $y += [int]($H * 0.135)
    $g.DrawString($SUBTITLE, $fSub, $teal, [float]$textX, [float]$y)
    $y += [int]($H * 0.062)
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(150, 47, 191, 159), [float]($H * 0.0035))
    $g.DrawLine($pen, [float]$textX, [float]$y, [float]($textX + $H * 0.30), [float]$y)
    $y += [int]($H * 0.030)
    $g.DrawString($INFO, $fInfo, $mint, [float]$textX, [float]$y)
    $y += [int]($H * 0.062)
    $mark = [float]($H * 0.012)
    foreach ($t in $BULLETS) {
      $g.FillRectangle($teal, [float]$textX, [float]($y + $H * 0.012), $mark, $mark)
      $g.DrawString($t, $fBul, $white, [float]($textX + $H * 0.030), [float]$y)
      $y += [int]($H * 0.046)
    }
    $y += [int]($H * 0.012)
    $g.DrawString('合成材料', $fFoot, $gray, [float]$textX, [float]$y)
    $y += [int]($H * 0.034)
    Draw-MatsRow $g ([float]$textX) ([float]$y) $ms
    $g.DrawString($FOOTER, $fFoot, $gray, [float]$textX, [float]($H * 0.90))
  }

  # 6) 青色细边框（只有箭袋的版本不加）
  if ($Mode -ne 'plain') {
    $pen2 = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(70, 47, 191, 159), [float]($H * 0.004))
    $g.DrawRectangle($pen2, [float]($H * 0.016), [float]($H * 0.016), [float]($W - $H * 0.032), [float]($H - $H * 0.032))
  }

  $g.Dispose()
  $bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  $icon.Dispose()
  Write-Host ("wrote {0}  ({1}x{2})  [{3}]" -f $Out, $W, $H, $Mode)
}

New-Cover 1920 1200 (Join-Path $OUTDIR 'cover_mcmod_1920x1200.png') 'plain'
New-Cover 1280  640 (Join-Path $OUTDIR 'cover_github_1280x640.png') 'plain'
New-Cover  640  400 (Join-Path $OUTDIR 'cover_mcmod_640x400.png')   'plain'
if ($Mats -or $Text) {
  New-Cover 1920 1200 (Join-Path $OUTDIR 'cover_mcmod_1920x1200_mats.png') 'mats'
  New-Cover 1280  640 (Join-Path $OUTDIR 'cover_github_1280x640_mats.png') 'mats'
}
if ($Text) {
  New-Cover 1920 1200 (Join-Path $OUTDIR 'cover_mcmod_1920x1200_text.png') 'text'
  New-Cover 1280  640 (Join-Path $OUTDIR 'cover_github_1280x640_text.png') 'text'
}
