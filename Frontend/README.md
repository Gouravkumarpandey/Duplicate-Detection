# Duplicate Application Remover

A full-stack application for detecting and removing duplicate applications using SHA-256 content hashing, with rule-based categorization and comprehensive logging.

## 🚀 Features

- **Content-based Duplicate Detection**: Uses SHA-256 hashing to identify true duplicates based on file content, not filenames
- **Rule-based Categorization**: Automatically categorizes files using configurable JSON rules
- **Directory Scanning**: Scan predefined directories or configure custom paths
- **Comprehensive Logging**: Track all operations with detailed logs (scan, duplicate detection, categorization, deletion)
- **Bulk Operations**: Select and delete multiple duplicate files at once
- **Responsive Design**: Modern, intuitive interface with smooth animations
- **Real-time Progress**: Live feedback during scanning and operations
- **Log Export**: Export activity logs to text files for analysis

## 🛠️ Tech Stack

### Frontend (React)
- **React 18** with TypeScript
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **Lucide React** for icons

### Backend (Java - Not included in this WebContainer demo)
- **Spring Boot** REST API
- **SHA-256 hashing** for content comparison
- **File system operations** for deletion
- **Rule-based categorization** using JSON configuration
- **Comprehensive logging** to logs.txt file

## 📁 Project Structure

```
src/
├── components/           # React components
│   ├── FileScanner.tsx   # Folder selection and scanning UI
│   ├── DuplicateList.tsx # Display duplicate file groups
│   ├── CategoryView.tsx  # Categorized file view
│   ├── LogViewer.tsx     # Activity logs display
│   └── DirectoryScanner.tsx # Directory configuration
├── config/              # Configuration files
│   └── rules.json       # Categorization rules
├── types/               # TypeScript type definitions
│   └── FileTypes.ts     # File, duplicate, and category types
├── utils/               # Utility functions
│   └── fileUtils.ts     # File hashing, categorization, and logging
├── api/                 # API layer
│   └── mockApi.ts       # Mock backend for demo (replace with real API)
└── App.tsx             # Main application component
```

## 🎯 How It Works

1. **Directory Configuration**: Configure directories to scan or select folders manually
2. **File Scanning**: Each file is processed to generate a SHA-256 hash
3. **Duplicate Detection**: Files with identical hashes are grouped as duplicates
4. **Rule-based Categorization**: Files are categorized using configurable JSON rules
5. **Selection & Deletion**: Users can select duplicate files and delete them safely
6. **Logging**: All operations are logged with timestamps and details

## 🔧 Configuration

### Categorization Rules (rules.json)

The application uses a JSON configuration file for categorization rules:

```typescript
{
  "rules": [
    {
      "name": "Browser",
      "conditions": {
        "filenameContains": ["chrome", "firefox", "edge", "safari"],
        "extensions": [".exe", ".app"]
      }
    },
    {
      "name": "Installer",
      "conditions": {
        "pathContains": ["setup", "install"],
        "extensions": [".msi", ".pkg", ".dmg"]
      }
    }
  ]
}
```

### Directory Configuration

Default directories are configured in the DirectoryScanner component:
- Windows: `C:\Program Files`, `C:\Program Files (x86)`
- macOS: `/Applications`, `/Downloads`
- Custom directories can be added through the UI

## 🚦 Getting Started

### Frontend Setup

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

### Backend Setup (Java Spring Boot)

Create a Spring Boot application with these endpoints:

#### POST /scan
```java
@PostMapping("/scan")
public ResponseEntity<ScanResult> scanDirectories(@RequestBody List<String> directories) {
    ScanResult result = fileService.scanDirectories(directories);
    return ResponseEntity.ok(result);
}
```

#### POST /remove
```java
@PostMapping("/remove")
public ResponseEntity<ApiResponse> deleteFiles(@RequestBody List<String> filePaths) {
    boolean success = fileService.deleteFiles(filePaths);
    return ResponseEntity.ok(new ApiResponse(success, "Files deleted successfully"));
}
```

#### GET /logs
```java
@GetMapping("/logs")
public ResponseEntity<List<LogEntry>> getLogs() {
    List<LogEntry> logs = logService.readLogs();
    return ResponseEntity.ok(logs);
}
```

### Java Implementation Features

1. **Content Hashing**: Use `MessageDigest.getInstance("SHA-256")` for file hashing
2. **Rule Processing**: Load rules.json and apply categorization logic
3. **Logging**: Write to logs.txt with timestamps and operation details
4. **File Operations**: Safe file deletion with proper error handling

## 🧪 Testing

Create a test directory structure:

```
test-folder/
├── browsers/
│   ├── chrome-installer.exe
│   ├── chrome-setup.exe        # Duplicate (same content)
│   └── firefox-setup.exe
├── media/
│   ├── vlc-player.exe
│   └── vlc-media-player.exe    # Duplicate (same content)
└── dev-tools/
    ├── vscode-setup.exe
    └── visual-studio-code.exe  # Duplicate (same content)
```

## 🔒 Security Considerations

- File operations require appropriate permissions
- Validate file paths to prevent directory traversal attacks
- Implement rate limiting for API endpoints
- Add user confirmation for destructive operations
- Sanitize log entries to prevent log injection

## 📝 API Documentation

### Scan Directories
- **URL**: `POST /scan`
- **Input**: `string[]` - Array of directory paths
- **Output**: `ScanResult` - Complete scan results with duplicates, categories, and logs

### Remove Files
- **URL**: `POST /remove`
- **Input**: `string[]` - Array of file paths to delete
- **Output**: `ApiResponse` - Success status and message

### Get Logs
- **URL**: `GET /logs`
- **Output**: `LogEntry[]` - Array of log entries

## 📊 Logging Format

The application maintains detailed logs in the following format:
```
[2024-01-15 10:30:45] SCAN: Started scanning 150 files | File: /Applications
[2024-01-15 10:30:46] DUPLICATE: Found 3 duplicate files | Hash: a1b2c3d4e5f6...
[2024-01-15 10:30:47] CATEGORY: File categorized as Browser | File: /Applications/Chrome.app
[2024-01-15 10:30:50] DELETE: File deleted | File: /Downloads/chrome-installer.exe
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit your changes: `git commit -am 'Add feature'`
4. Push to the branch: `git push origin feature-name`
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## ⚠️ Disclaimer

This application performs file deletion operations. Always backup important data before using. Test thoroughly in a safe environment before running on production systems. The developers are not responsible for any data loss.