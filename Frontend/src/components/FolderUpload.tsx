import React, { useState } from 'react';

interface FileUploadResult {
  success: boolean;
  filename: string;
  message: string;
  fileId?: string;
  hash?: string;
  fileSize: number;
  fileType?: string;
  isDuplicate: boolean;
  error?: string;
}

interface FolderUploadResponse {
  success: boolean;
  message: string;
  totalFiles: number;
  successfulUploads: number;
  failedUploads: number;
  duplicatesFound: number;
  uploadResults: FileUploadResult[];
  statistics: {
    successRate: number;
    duplicateRate: number;
    fileTypeDistribution: Record<string, number>;
    totalSizeBytes: number;
    totalSizeMB: number;
  };
}

const FolderUpload: React.FC = () => {
  const [uploading, setUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState<FolderUploadResponse | null>(null);
  const [selectedFiles, setSelectedFiles] = useState<FileList | null>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    setSelectedFiles(files);
    setUploadResult(null); // Clear previous results
  };

  const uploadFolder = async () => {
    if (!selectedFiles || selectedFiles.length === 0) {
      alert('Please select files to upload');
      return;
    }

    setUploading(true);
    
    try {
      const formData = new FormData();
      
      // Add all selected files to FormData
      for (let i = 0; i < selectedFiles.length; i++) {
        formData.append('files', selectedFiles[i]);
      }

      const response = await fetch('http://localhost:8080/api/upload-folder', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result: FolderUploadResponse = await response.json();
      setUploadResult(result);

    } catch (error) {
      console.error('Upload failed:', error);
      alert('Upload failed: ' + (error as Error).message);
    } finally {
      setUploading(false);
    }
  };

  const getFileStatusIcon = (file: FileUploadResult) => {
    if (!file.success) return '❌';
    if (file.isDuplicate) return '⚠️';
    return '✅';
  };

  const getFileStatusClass = (file: FileUploadResult) => {
    if (!file.success) return 'border-red-500 bg-red-50';
    if (file.isDuplicate) return 'border-yellow-500 bg-yellow-50';
    return 'border-green-500 bg-green-50';
  };

  return (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-lg shadow-sm border">
        <h2 className="text-xl font-semibold mb-4">📂 Folder Upload</h2>
        <p className="text-gray-600 mb-4">
          Upload multiple files from a folder. Only .txt, .pdf, and .docx files are supported.
          Files will be automatically analyzed and metadata saved to MongoDB Atlas.
        </p>

        {/* File Selection */}
        <div className="border-2 border-dashed border-blue-300 rounded-lg p-8 text-center mb-4 hover:border-blue-400 transition-colors">
          <div className="space-y-4">
            <div className="text-4xl">📁</div>
            <div>
              <label htmlFor="folder-input" className="cursor-pointer">
                <span className="text-blue-600 hover:text-blue-800 font-medium">
                  Choose files from folder
                </span>
                <input
                  id="folder-input"
                  type="file"
                  multiple
                  accept=".txt,.pdf,.docx"
                  onChange={handleFileSelect}
                  className="hidden"
                />
              </label>
            </div>
            {selectedFiles && (
              <p className="text-sm text-gray-600">
                {selectedFiles.length} file(s) selected
              </p>
            )}
          </div>
        </div>

        {/* Upload Button */}
        <button
          onClick={uploadFolder}
          disabled={!selectedFiles || uploading}
          className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
        >
          {uploading ? (
            <span className="flex items-center justify-center space-x-2">
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              <span>Uploading and Analyzing...</span>
            </span>
          ) : (
            `Upload ${selectedFiles ? selectedFiles.length : ''} Files`
          )}
        </button>
      </div>

      {/* Upload Results */}
      {uploadResult && (
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <h3 className="text-lg font-semibold mb-4">Upload Results</h3>
          
          {/* Summary Statistics */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
            <div className="bg-blue-50 p-3 rounded-lg text-center">
              <div className="text-2xl font-bold text-blue-600">{uploadResult.totalFiles}</div>
              <div className="text-sm text-gray-600">Total Files</div>
            </div>
            <div className="bg-green-50 p-3 rounded-lg text-center">
              <div className="text-2xl font-bold text-green-600">{uploadResult.successfulUploads}</div>
              <div className="text-sm text-gray-600">Successful</div>
            </div>
            <div className="bg-red-50 p-3 rounded-lg text-center">
              <div className="text-2xl font-bold text-red-600">{uploadResult.failedUploads}</div>
              <div className="text-sm text-gray-600">Failed</div>
            </div>
            <div className="bg-yellow-50 p-3 rounded-lg text-center">
              <div className="text-2xl font-bold text-yellow-600">{uploadResult.duplicatesFound}</div>
              <div className="text-sm text-gray-600">Duplicates</div>
            </div>
            <div className="bg-purple-50 p-3 rounded-lg text-center">
              <div className="text-2xl font-bold text-purple-600">
                {uploadResult.statistics.successRate.toFixed(1)}%
              </div>
              <div className="text-sm text-gray-600">Success Rate</div>
            </div>
          </div>

          {/* Overall Status */}
          <div className={`p-4 rounded-lg mb-4 ${
            uploadResult.success ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'
          }`}>
            <div className={`font-medium ${uploadResult.success ? 'text-green-800' : 'text-red-800'}`}>
              {uploadResult.success ? '✅' : '❌'} {uploadResult.message}
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Total size: {uploadResult.statistics.totalSizeMB.toFixed(2)} MB
            </div>
          </div>

          {/* Individual File Results */}
          <div className="space-y-3">
            <h4 className="font-medium text-gray-900">File Details:</h4>
            {uploadResult.uploadResults.map((file, index) => (
              <div
                key={index}
                className={`p-4 rounded-lg border-2 ${getFileStatusClass(file)}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-2">
                      <span className="text-lg">{getFileStatusIcon(file)}</span>
                      <span className="font-medium">{file.filename}</span>
                      {file.isDuplicate && (
                        <span className="text-xs bg-yellow-200 text-yellow-800 px-2 py-1 rounded">
                          DUPLICATE
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-gray-600 mt-1">{file.message}</p>
                    
                    {file.success ? (
                      <div className="mt-2 text-xs text-gray-500 space-y-1">
                        <div><strong>File ID:</strong> {file.fileId}</div>
                        <div><strong>Type:</strong> {file.fileType}</div>
                        <div><strong>Size:</strong> {file.fileSize} bytes</div>
                        <div><strong>SHA-256:</strong> <code className="bg-gray-100 px-1">{file.hash}</code></div>
                      </div>
                    ) : (
                      <div className="mt-2 text-xs text-red-600">
                        <strong>Error:</strong> {file.error}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* File Type Distribution */}
          {uploadResult.statistics.fileTypeDistribution && (
            <div className="mt-6 p-4 bg-gray-50 rounded-lg">
              <h4 className="font-medium text-gray-900 mb-2">File Type Distribution:</h4>
              <div className="flex flex-wrap gap-2">
                {Object.entries(uploadResult.statistics.fileTypeDistribution).map(([type, count]) => (
                  <span key={type} className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm">
                    {type}: {count}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default FolderUpload;
