import React, { useState, useCallback } from 'react';
import { Upload, Database, TestTube, FileText, AlertCircle, CheckCircle } from 'lucide-react';
import { 
  realApiService, 
  DatabaseFile, 
  FileUploadResponse, 
  DuplicatesResponse, 
  StorageInfoResponse
} from '../api/RealApiService';

interface DatabaseTestProps {
  onFileUploaded?: (response: FileUploadResponse) => void;
}

const DatabaseTest: React.FC<DatabaseTestProps> = ({ onFileUploaded }) => {
  const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'success' | 'error'>('idle');
  const [uploadResponse, setUploadResponse] = useState<FileUploadResponse | { error: string } | null>(null);
  const [files, setFiles] = useState<DatabaseFile[]>([]);
  const [duplicates, setDuplicates] = useState<DuplicatesResponse | null>(null);
  const [storageInfo, setStorageInfo] = useState<StorageInfoResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const loadAllFiles = useCallback(async () => {
    setIsLoading(true);
    try {
      const allFiles = await realApiService.getAllFiles();
      setFiles(allFiles);
    } catch (error) {
      console.error('Failed to load files:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const handleFileUpload = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploadStatus('uploading');
    setUploadResponse(null);

    try {
      const response = await realApiService.uploadFile(file);
      setUploadResponse(response);
      setUploadStatus('success');
      
      if (onFileUploaded) {
        onFileUploaded(response);
      }
      
      // Refresh files list after upload
      await loadAllFiles();
    } catch (error) {
      console.error('Upload failed:', error);
      setUploadStatus('error');
      setUploadResponse({ error: error instanceof Error ? error.message : 'Unknown error' });
    }
  }, [onFileUploaded, loadAllFiles]);

  const loadDuplicates = useCallback(async () => {
    setIsLoading(true);
    try {
      const duplicatesData = await realApiService.getDuplicates();
      setDuplicates(duplicatesData);
    } catch (error) {
      console.error('Failed to load duplicates:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadStorageInfo = useCallback(async () => {
    setIsLoading(true);
    try {
      const info = await realApiService.getStorageInfo();
      setStorageInfo(info);
    } catch (error) {
      console.error('Failed to load storage info:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const testContentExtraction = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setIsLoading(true);
    try {
      const result = await realApiService.testContentExtraction(file);
      alert(`Content extraction test:\n${JSON.stringify(result, null, 2)}`);
    } catch (error) {
      console.error('Content extraction test failed:', error);
      alert(`Test failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const handleDeleteFile = useCallback(async (fileId: string) => {
    if (!confirm('Are you sure you want to delete this file?')) return;

    try {
      await realApiService.deleteFile(fileId);
      await loadAllFiles(); // Refresh the list
      alert('File deleted successfully');
    } catch (error) {
      console.error('Failed to delete file:', error);
      alert(`Delete failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }, [loadAllFiles]);

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h2 className="text-2xl font-bold text-gray-800 mb-4 flex items-center gap-2">
          <Database className="text-blue-600" size={24} />
          Database Connection Test
        </h2>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <button
            onClick={loadAllFiles}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:bg-gray-300"
          >
            <Database size={16} />
            Load All Files ({files.length})
          </button>
          
          <button
            onClick={loadDuplicates}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 disabled:bg-gray-300"
          >
            <FileText size={16} />
            Load Duplicates
          </button>
          
          <button
            onClick={loadStorageInfo}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 disabled:bg-gray-300"
          >
            <TestTube size={16} />
            Storage Info
          </button>

          <label className="flex items-center gap-2 px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 cursor-pointer">
            <TestTube size={16} />
            Test Extraction
            <input
              type="file"
              accept=".txt,.pdf,.docx"
              onChange={testContentExtraction}
              className="hidden"
            />
          </label>
        </div>

        {/* File Upload Section */}
        <div className="bg-gray-50 rounded-lg p-4 mb-6">
          <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
            <Upload size={20} />
            Upload File to Database
          </h3>
          
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 cursor-pointer">
              <Upload size={16} />
              {uploadStatus === 'uploading' ? 'Uploading...' : 'Choose File'}
              <input
                type="file"
                accept=".txt,.pdf,.docx"
                onChange={handleFileUpload}
                disabled={uploadStatus === 'uploading'}
                className="hidden"
              />
            </label>
            
            {uploadStatus === 'success' && (
              <div className="flex items-center gap-1 text-green-600">
                <CheckCircle size={16} />
                Upload successful!
              </div>
            )}
            
            {uploadStatus === 'error' && (
              <div className="flex items-center gap-1 text-red-600">
                <AlertCircle size={16} />
                Upload failed!
              </div>
            )}
          </div>

          {uploadResponse && (
            <div className="mt-4 p-3 bg-white rounded border">
              <h4 className="font-semibold mb-2">Upload Response:</h4>
              <pre className="text-sm text-gray-700 overflow-auto">
                {JSON.stringify(uploadResponse, null, 2)}
              </pre>
            </div>
          )}
        </div>

        {/* Storage Info */}
        {storageInfo && (
          <div className="bg-green-50 rounded-lg p-4 mb-6">
            <h3 className="text-lg font-semibold mb-3">Storage Configuration</h3>
            <pre className="text-sm text-gray-700 overflow-auto">
              {JSON.stringify(storageInfo, null, 2)}
            </pre>
          </div>
        )}

        {/* Files List */}
        {files.length > 0 && (
          <div className="bg-blue-50 rounded-lg p-4 mb-6">
            <h3 className="text-lg font-semibold mb-3">Files in Database ({files.length})</h3>
            <div className="space-y-2 max-h-60 overflow-auto">
              {files.map((file) => (
                <div key={file.id} className="bg-white p-3 rounded border flex justify-between items-center">
                  <div className="flex-1">
                    <div className="font-medium">{file.fileName}</div>
                    <div className="text-sm text-gray-600">
                      Size: {(file.fileSize / 1024).toFixed(1)}KB | 
                      Hash: {file.fileHash?.substring(0, 16)}... | 
                      Uploaded: {new Date(file.uploadedDate).toLocaleString()}
                    </div>
                  </div>
                  <button
                    onClick={() => handleDeleteFile(file.id)}
                    className="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600 text-sm"
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Duplicates */}
        {duplicates && (
          <div className="bg-purple-50 rounded-lg p-4">
            <h3 className="text-lg font-semibold mb-3">Duplicates</h3>
            <pre className="text-sm text-gray-700 overflow-auto">
              {JSON.stringify(duplicates, null, 2)}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
};

export default DatabaseTest;
