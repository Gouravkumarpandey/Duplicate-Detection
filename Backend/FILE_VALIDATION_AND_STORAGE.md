# File Type Validation and Storage Configuration

## Overview

This document describes the enhanced file type validation and multiple storage options implemented in the File Deduplication application.

## 🔧 File Type Validation

### Strict Validation Rules

The application now enforces strict file type validation, accepting only:

- **Text files**: `.txt` (MIME type: `text/plain`)
- **PDF files**: `.pdf` (MIME type: `application/pdf`)
- **Word documents**: `.docx` (MIME type: `application/vnd.openxmlformats-officedocument.wordprocessingml.document`)

### Validation Process

1. **MIME Type Check**: Validates the HTTP Content-Type header
2. **File Extension Check**: Validates the file extension
3. **Both Must Match**: For a file to be accepted, both MIME type and extension must be valid
4. **File Size Limit**: Maximum 100MB per file
5. **Filename Security**: Prevents malicious filenames with path traversal characters

### Example Validation Response

```json
{
  "error": "Unsupported file type. Only .txt, .pdf, and .docx files are allowed.",
  "receivedType": "image/jpeg",
  "fileName": "photo.jpg",
  "allowedTypes": [
    "text/plain",
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  ],
  "allowedExtensions": [".txt", ".pdf", ".docx"]
}
```

## 🗄️ File Storage Options

### Option A: Local Storage (Default)

Files are stored in the local filesystem with the following structure:

```
uploads/
├── user1/
│   ├── 2025/01/06/
│   │   ├── user1_20250106_143025_a1b2c3d4.pdf
│   │   └── user1_20250106_143126_e5f6g7h8.txt
│   └── ...
├── user2/
│   └── ...
└── temp/
    ├── backup_user1_20250106_143025_a1b2c3d4.pdf
    └── ...
```

**Configuration:**
```properties
app.file.storage.type=LOCAL
app.file.upload-dir=uploads/
app.file.temp-dir=temp/
```

**Features:**
- Organized by user and date
- Automatic backup copies in temp directory
- Unique filename generation to prevent conflicts
- Sanitized user paths for security

### Option B: MongoDB GridFS

Store files directly in MongoDB using GridFS for large file handling.

**Configuration:**
```properties
app.file.storage.type=GRIDFS
```

**Features:**
- Files stored in MongoDB collections
- Metadata stored with each file
- Automatic backup to local storage
- Supports streaming large files

**GridFS File Structure:**
```json
{
  "_id": "ObjectId",
  "filename": "gridfs_user1_20250106_143025_a1b2c3d4.pdf",
  "metadata": {
    "originalFileName": "document.pdf",
    "uploadedBy": "user1@example.com",
    "uploadedDate": "2025-01-06T14:30:25",
    "contentType": "application/pdf",
    "fileSize": 1024000
  }
}
```

### Option C: AWS S3 (Future Implementation)

**Configuration:**
```properties
app.file.storage.type=S3
# Additional S3 configuration would be required
```

### Option D: Google Cloud Storage (Future Implementation)

**Configuration:**
```properties
app.file.storage.type=GOOGLE_CLOUD
# Additional Google Cloud configuration would be required
```

## 📊 API Endpoints

### File Upload with Validation

```http
POST /api/files/upload
Content-Type: multipart/form-data

Parameters:
- email: user email
- password: user password
- file: the file to upload (must be .txt, .pdf, or .docx)
```

### Multiple File Upload

```http
POST /api/files/upload-multiple
Content-Type: multipart/form-data

Parameters:
- email: user email
- password: user password
- files: array of files (max 10, each must be .txt, .pdf, or .docx)
```

### File Type Validation

```http
POST /api/files/validate-type
Content-Type: multipart/form-data

Parameters:
- file: the file to validate
```

**Response:**
```json
{
  "valid": true,
  "fileName": "document.pdf",
  "contentType": "application/pdf",
  "fileSize": 1024000,
  "message": "File type is supported"
}
```

### Storage Information

```http
GET /api/files/storage-info
```

**Response:**
```json
{
  "maxFileSize": "100MB",
  "maxFileSizeBytes": 104857600,
  "allowedTypes": [
    "text/plain",
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  ],
  "allowedExtensions": [".txt", ".pdf", ".docx"],
  "storageOptions": ["LOCAL", "GRIDFS", "S3", "GOOGLE_CLOUD"],
  "currentStorageType": "LOCAL"
}
```

### Supported File Types

```http
GET /api/files/supported-types
```

**Response:**
```json
{
  "mimeTypes": [
    "text/plain",
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  ],
  "extensions": [".txt", ".pdf", ".docx"],
  "descriptions": {
    "text/plain": "Plain text files (.txt)",
    "application/pdf": "Portable Document Format (.pdf)",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "Microsoft Word documents (.docx)"
  }
}
```

## ⚙️ Configuration

### Application Properties

```properties
# File Storage Configuration
app.file.upload-dir=uploads/
app.file.temp-dir=temp/
app.file.storage.type=LOCAL

# File Type Validation Configuration
app.file.allowed-types=text/plain,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document
app.file.allowed-extensions=.txt,.pdf,.docx
app.file.max-size=104857600

# File Upload Configuration
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

## 🔒 Security Features

1. **Strict File Type Validation**: Only allows specific file types
2. **File Size Limits**: Prevents large file attacks
3. **Filename Sanitization**: Prevents path traversal attacks
4. **User Path Sanitization**: Safe directory creation
5. **Malicious Content Detection**: Basic security checks

## 🛠️ Implementation Details

### FileStorageService

The `FileStorageService` class handles all file storage operations:

- **File validation** with strict type checking
- **Multiple storage backends** (Local, GridFS, S3, Google Cloud)
- **Unique filename generation** to prevent conflicts
- **Backup creation** for reliability
- **Error handling** with cleanup on failure

### FileController Enhancements

- Enhanced validation in upload endpoints
- New validation and information endpoints
- Comprehensive error responses
- Multiple file upload support with validation

### Database Integration

- New repository methods for storage management
- GridFS configuration for MongoDB file storage
- Storage location tracking in file records

## 🚀 Usage Examples

### Upload a Valid File

```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -F "email=user@example.com" \
  -F "password=password123" \
  -F "file=@document.pdf"
```

### Upload an Invalid File (Will Fail)

```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -F "email=user@example.com" \
  -F "password=password123" \
  -F "file=@image.jpg"
```

**Response:**
```json
{
  "error": "Unsupported file type. Only .txt, .pdf, and .docx files are allowed.",
  "receivedType": "image/jpeg",
  "fileName": "image.jpg",
  "allowedTypes": ["text/plain", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"],
  "allowedExtensions": [".txt", ".pdf", ".docx"]
}
```

### Validate File Type Before Upload

```bash
curl -X POST "http://localhost:8080/api/files/validate-type" \
  -F "file=@document.pdf"
```

## 📈 Monitoring and Logging

The application logs all file operations:

- File upload attempts
- Validation failures
- Storage operations
- Error conditions

Example log entries:
```
2025-01-06 14:30:25 - File upload initiated - User: user@example.com, File: document.pdf, Size: 1024000 bytes
2025-01-06 14:30:26 - File uploaded and processed successfully - User: user@example.com, File: document.pdf, Category: Documents, Storage: LOCAL, ID: 507f1f77bcf86cd799439011
```

## 🔧 Troubleshooting

### Common Issues

1. **File Type Rejection**: Ensure the file has the correct extension and MIME type
2. **File Size Too Large**: Check the file size limit (100MB default)
3. **GridFS Errors**: Ensure MongoDB is properly configured
4. **Storage Directory Issues**: Check file system permissions

### Error Codes

- `400 Bad Request`: Invalid file type, size, or missing parameters
- `500 Internal Server Error`: Storage backend issues or server errors
