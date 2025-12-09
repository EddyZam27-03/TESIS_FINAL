# Script para descargar modelos de MediaPipe
$ErrorActionPreference = "Stop"

$baseDir = "app\src\main\assets\INFO"
if (-not (Test-Path $baseDir)) {
    New-Item -ItemType Directory -Path $baseDir -Force | Out-Null
}

Write-Host "Descargando modelos de MediaPipe..."

# URLs alternativas
$urls = @{
    "palm_detection.tflite" = @(
        "https://storage.googleapis.com/mediapipe-models/palm_detector/palm_detection_full/float16/1/palm_detection_full.tflite",
        "https://github.com/google/mediapipe/raw/master/mediapipe/modules/palm_detection/palm_detection_full.tflite",
        "https://github.com/google/mediapipe/raw/v0.10.8/mediapipe/modules/palm_detection/palm_detection_full.tflite"
    )
    "hand_landmark.tflite" = @(
        "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task",
        "https://github.com/google/mediapipe/raw/master/mediapipe/modules/hand_landmark/hand_landmark_full.tflite",
        "https://github.com/google/mediapipe/raw/v0.10.8/mediapipe/modules/hand_landmark/hand_landmark_full.tflite"
    )
}

foreach ($file in $urls.Keys) {
    $outputPath = Join-Path $baseDir $file
    $downloaded = $false
    
    foreach ($url in $urls[$file]) {
        try {
            Write-Host "Intentando descargar $file desde: $url"
            Invoke-WebRequest -Uri $url -OutFile $outputPath -TimeoutSec 60 -UseBasicParsing
            if (Test-Path $outputPath -PathType Leaf) {
                $fileSize = (Get-Item $outputPath).Length
                if ($fileSize -gt 0) {
                    $sizeKB = [math]::Round($fileSize/1024, 2)
                    Write-Host "✓ $file descargado correctamente ($sizeKB KB)" -ForegroundColor Green
                    $downloaded = $true
                    break
                }
            }
        } catch {
            Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Yellow
            if (Test-Path $outputPath) {
                Remove-Item $outputPath -Force
            }
        }
    }
    
    if (-not $downloaded) {
        Write-Host "✗ No se pudo descargar $file" -ForegroundColor Red
    }
}

Write-Host "`nVerificando archivos descargados:"
Get-ChildItem $baseDir -Filter "*.tflite" | ForEach-Object {
    Write-Host "  - $($_.Name): $([math]::Round($_.Length/1KB, 2)) KB"
}

