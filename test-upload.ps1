$uri = "http://localhost:8080/api/uploads/upload"
$filePath = "c:\Users\kumar\OneDrive\Desktop\Duplicate Detection\test-file.txt"

# Create multipart form data
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$bodyLines = (
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"test-file.txt`"",
    "Content-Type: text/plain$LF",
    (Get-Content $filePath -Raw),
    "--$boundary--$LF"
) -join $LF

try {
    $response = Invoke-RestMethod -Uri $uri -Method Post -Body $bodyLines -ContentType "multipart/form-data; boundary=$boundary"
    Write-Host "Upload successful!"
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Upload failed: $($_.Exception.Message)"
}
