$uri = "http://localhost:8080/api/upload-folder"
$folderPath = "c:\Users\kumar\OneDrive\Desktop\Duplicate Detection\demo-files"

Write-Host "🚀 Testing Folder Upload Functionality" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Get all files from the demo-files folder
$files = Get-ChildItem -Path $folderPath -File

Write-Host "📁 Files in demo folder:" -ForegroundColor Yellow
foreach ($file in $files) {
    $status = if ($file.Extension -eq ".txt") { "✅ VALID" } else { "❌ INVALID" }
    Write-Host "  $status $($file.Name) ($($file.Extension)) - $($file.Length) bytes" -ForegroundColor $(if($file.Extension -eq ".txt"){"Green"}else{"Red"})
}
Write-Host ""

# Create multipart form data
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$bodyLines = @()
$bodyLines += "--$boundary"

foreach ($file in $files) {
    $filePath = $file.FullName
    $fileName = $file.Name
    $fileContent = [System.IO.File]::ReadAllBytes($filePath)
    
    # Determine MIME type based on extension
    $mimeType = switch ($file.Extension.ToLower()) {
        ".txt" { "text/plain" }
        ".pdf" { "application/pdf" }
        ".docx" { "application/vnd.openxmlformats-officedocument.wordprocessingml.document" }
        default { "application/octet-stream" }
    }
    
    $bodyLines += "Content-Disposition: form-data; name=`"files`"; filename=`"$fileName`""
    $bodyLines += "Content-Type: $mimeType$LF"
    $bodyLines += [System.Text.Encoding]::UTF8.GetString($fileContent)
    $bodyLines += "--$boundary"
}

$bodyLines[-1] = "--$boundary--$LF"
$body = $bodyLines -join $LF

try {
    Write-Host "📤 Uploading files to Spring Boot backend..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri $uri -Method Post -Body $body -ContentType "multipart/form-data; boundary=$boundary"
    
    Write-Host ""
    Write-Host "🎉 UPLOAD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "===================" -ForegroundColor Green
    
    # Display summary
    Write-Host ""
    Write-Host "📊 UPLOAD SUMMARY:" -ForegroundColor Cyan
    Write-Host "  Total Files: $($response.totalFiles)" -ForegroundColor White
    Write-Host "  Successful: $($response.successfulUploads)" -ForegroundColor Green
    Write-Host "  Failed: $($response.failedUploads)" -ForegroundColor Red
    Write-Host "  Duplicates: $($response.duplicatesFound)" -ForegroundColor Yellow
    Write-Host "  Success Rate: $($response.statistics.successRate)%" -ForegroundColor Cyan
    Write-Host ""
    
    # Display individual file results
    Write-Host "📋 FILE DETAILS:" -ForegroundColor Cyan
    foreach ($fileResult in $response.uploadResults) {
        $icon = if (-not $fileResult.success) { "❌" } elseif ($fileResult.duplicate) { "⚠️" } else { "✅" }
        $status = if (-not $fileResult.success) { "FAILED" } elseif ($fileResult.duplicate) { "DUPLICATE" } else { "SUCCESS" }
        
        $color = if (-not $fileResult.success) { "Red" } elseif ($fileResult.duplicate) { "Yellow" } else { "Green" }
        Write-Host "  $icon $($fileResult.filename) - $status" -ForegroundColor $color
        
        if ($fileResult.success) {
            Write-Host "     🆔 File ID: $($fileResult.fileId)" -ForegroundColor Gray
            Write-Host "     🔍 SHA-256: $($fileResult.hash.Substring(0, 16))..." -ForegroundColor Gray
            Write-Host "     💾 Size: $($fileResult.fileSize) bytes" -ForegroundColor Gray
            Write-Host "     📁 Type: $($fileResult.fileType)" -ForegroundColor Gray
        } else {
            Write-Host "     ❗ Error: $($fileResult.error)" -ForegroundColor Red
        }
        Write-Host ""
    }
    
    Write-Host "🗄️  All successful uploads have been saved to MongoDB Atlas!" -ForegroundColor Green
    Write-Host "🔗 Backend API: $uri" -ForegroundColor Gray
    Write-Host "🔗 Frontend: http://localhost:5176 (Folder Upload tab)" -ForegroundColor Gray
    
} catch {
    Write-Host ""
    Write-Host "💥 UPLOAD FAILED!" -ForegroundColor Red
    Write-Host "=================" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}
