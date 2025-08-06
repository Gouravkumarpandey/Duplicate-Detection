$uri = "http://localhost:8080/api/upload-folder"
$folderPath = "c:\Users\kumar\OneDrive\Desktop\Duplicate Detection\test-folder"

# Get all files from the folder
$files = Get-ChildItem -Path $folderPath -File

Write-Host "Testing folder upload with files:"
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
    $fileContentBase64 = [System.Convert]::ToBase64String($fileContent)
    
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
    Write-Host "Uploading folder..."
    $response = Invoke-RestMethod -Uri $uri -Method Post -Body $body -ContentType "multipart/form-data; boundary=$boundary"
    
    Write-Host "✅ Folder upload successful!" -ForegroundColor Green
    Write-Host "Response:"
    $response | ConvertTo-Json -Depth 10
    
} catch {
    Write-Host "❌ Folder upload failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Error details: $responseBody" -ForegroundColor Red
    }
}
