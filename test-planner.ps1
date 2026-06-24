#!/usr/bin/env pwsh
# Kill any existing Java processes
Write-Host "Killing existing Java processes..."
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Start the application with DEBUG logging
Write-Host "Starting application with DEBUG logging..."
$process = Start-Process -FilePath "java" `
    -Args '-jar target/ns-framework-demo-app-0.1.0-SNAPSHOT.jar --spring.profiles.active=ollama --logging.level.com.tesis.nsframework.planner.docker=DEBUG' `
    -WorkingDirectory 'C:\sistemas\TFE\full\ns-framework-demo' `
    -NoNewWindow `
    -RedirectStandardOutput 'C:\sistemas\TFE\full\app.log' `
    -PassThru

Write-Host "Application PID: $($process.Id)"
Write-Host "Waiting for application to start..."
Start-Sleep -Seconds 10

# Make a test request
Write-Host "Sending test request..."
$body = '{"input":"Quiero viajar desde Madrid para visitar Universidad de La Rioja el 2026-07-10"}'
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/execute' `
        -Method Post `
        -Body $body `
        -ContentType 'application/json' `
        -UseBasicParsing `
        -ErrorAction Stop

    Write-Host "Response Status: $($response.StatusCode)"
    Write-Host "Response Body:"
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
}
catch {
    Write-Host "Request failed: $_"
}

# Capture logs
Write-Host "`nRecent logs:"
if (Test-Path 'C:\sistemas\TFE\full\app.log') {
    Get-Content 'C:\sistemas\TFE\full\app.log' | Select-Object -Last 100
}

