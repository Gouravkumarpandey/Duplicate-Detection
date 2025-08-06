$uri = "http://localhost:8080/api/upload-folder"
$folderPath = "c:\Users\kumar\OneDrive\Desktop\Duplicate Detection\demo-files"

Write-Host "Testing Folder Upload..." -ForegroundColor Green

# Get all files from the demo-files folder
$files = Get-ChildItem -Path $folderPath -File

Write-Host "Files to upload:"
foreach ($file in $files) {
    Write-Host "  - $($file.Name) ($($file.Extension))"
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
    Write-Host "Uploading files..."
    $response = Invoke-RestMethod -Uri $uri -Method Post -Body $body -ContentType "multipart/form-data; boundary=$boundary"
    
    Write-Host "✅ Upload successful!" -ForegroundColor Green
    Write-Host "Message: $($response.message)"
    Write-Host "Total Files: $($response.totalFiles)"
    Write-Host "Successful: $($response.successfulUploads)" 
    Write-Host "Failed: $($response.failedUploads)"
    Write-Host "Duplicates: $($response.duplicatesFound)"
    
    Write-Host "`nFile Results:"
    foreach ($fileResult in $response.uploadResults) {
        $status = if ($fileResult.success) { "✅" } else { "❌" }
        Write-Host "  $status $($fileResult.filename) - $($fileResult.message)"
        if ($fileResult.error) {
            Write-Host "    Error: $($fileResult.error)" -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host "❌ Upload failed: $($_.Exception.Message)" -ForegroundColor Red
}
