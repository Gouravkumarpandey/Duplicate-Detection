# Enhanced File Upload API Documentation

## Endpoint: `/api/files/upload-enhanced`

### Method: POST

### Description
This endpoint accepts file uploads for .txt, .pdf, and .docx files only. It extracts the text content, calculates a SHA-256 hash, checks for duplicates in MongoDB, and saves the file to the local `/uploads` folder.

### Request Parameters
- `file` (multipart/form-data): The file to upload

### Supported File Types
- `.txt` files - processed with BufferedReader
- `.pdf` files - processed with Apache PDFBox  
- `.docx` files - processed with Apache POI

### Response Examples

#### Success Response (New File)
```json
{
  "success": true,
  "isDuplicate": false,
  "message": "File uploaded and processed successfully",
  "fileId": "64a7b8c9d1e2f3g4h5i6j7k8",
  "fileName": "example.txt",
  "fileSize": 1024,
  "fileType": ".txt",
  "uploadDate": "2025-01-15T10:30:00",
  "filePath": "/uploads/example_1642234567890.txt",
  "contentHash": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6",
  "contentPreview": "This is a preview of the file content...",
  "textContentLength": 2048
}
```

#### Duplicate File Response
```json
{
  "success": false,
  "isDuplicate": true,
  "message": "Duplicate file detected",
  "existingFile": {
    "id": "64a7b8c9d1e2f3g4h5i6j7k8",
    "fileName": "original.txt",
    "uploadDate": "2025-01-14T15:20:00",
    "filePath": "/uploads/original_1642148567890.txt",
    "hash": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6"
  },
  "contentHash": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6"
}
```

#### Error Responses
```json
{
  "error": "Only .txt, .pdf, and .docx files are allowed",
  "receivedType": "image/jpeg",
  "fileName": "image.jpg",
  "allowedTypes": ["text/plain", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"]
}
```

```json
{
  "error": "File size exceeds maximum limit of 100MB"
}
```

```json
{
  "error": "Failed to extract content from file: Invalid PDF format"
}
```

### curl Example
```bash
curl -X POST http://localhost:8080/api/files/upload-enhanced \
  -F "file=@example.txt" \
  -H "Content-Type: multipart/form-data"
```

### Frontend JavaScript Example
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('/api/files/upload-enhanced', {
    method: 'POST',
    body: formData
})
.then(response => response.json())
.then(data => {
    if (data.success) {
        if (data.isDuplicate) {
            alert('Duplicate file detected!');
        } else {
            alert('File uploaded successfully!');
        }
    } else {
        alert('Error: ' + data.error);
    }
});
```

### How It Works

1. **File Validation**: Checks file type and size
2. **Content Extraction**: 
   - .txt files: Uses BufferedReader with UTF-8 encoding
   - .pdf files: Uses Apache PDFBox PDFTextStripper
   - .docx files: Uses Apache POI XWPFWordExtractor
3. **Hash Calculation**: SHA-256 hash of extracted text content
4. **Duplicate Detection**: Queries MongoDB for existing files with same hash
5. **File Storage**: Saves to local `/uploads` folder with unique filename
6. **Metadata Storage**: Saves file metadata to MongoDB including:
   - File name, size, type, upload date
   - File path and hash
   - Content preview (first 500 characters)
   - Verification status and timestamps

### Database Schema (FileRecord)
The following fields are stored in MongoDB:
- `fileName`: Original filename
- `filePath`: Path to stored file
- `fileHash`: SHA-256 hash of content
- `fileSize`: File size in bytes
- `fileExtension`: File extension (.txt, .pdf, .docx)
- `mimeType`: MIME type
- `uploadedDate`: Upload timestamp
- `storedFileName`: Unique filename in storage
- `contentPreview`: First 500 characters of content
- `hasPreview`: Boolean indicating if preview is available
- `isVerified`: Boolean indicating if file is verified
- `lastVerifiedDate`: Last verification timestamp
