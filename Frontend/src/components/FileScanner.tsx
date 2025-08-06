import React, { useRef } from 'react';
import { FolderOpen, Search, Loader } from 'lucide-react';

interface FileScannerProps {
  onFolderSelect: (files: FileList) => void;
  isScanning: boolean;
  fileCount: number;
}

const FileScanner: React.FC<FileScannerProps> = ({ onFolderSelect, isScanning, fileCount }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFolderClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (files && files.length > 0) {
      onFolderSelect(files);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-lg border border-gray-100 p-8 mb-8">
      <div className="text-center">
        <input
          ref={fileInputRef}
          type="file"
          webkitdirectory=""
          multiple
          onChange={handleFileChange}
          className="hidden"
          accept="*/*"
        />

        <div className="mb-6">
          {isScanning ? (
            <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full text-white">
              <Loader className="animate-spin" size={32} />
            </div>
          ) : (
            <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full text-white hover:from-blue-600 hover:to-purple-600 transition-all cursor-pointer">
              <FolderOpen size={32} />
            </div>
          )}
        </div>

        <h2 className="text-2xl font-bold text-gray-800 mb-3">
          {isScanning ? 'Scanning Files...' : 'Select Folder to Scan'}
        </h2>

        <p className="text-gray-600 mb-6 max-w-md mx-auto">
          {isScanning
            ? `Processing ${fileCount} files and detecting duplicates based on content hash...`
            : 'Choose a folder containing applications to scan for duplicates. We\'ll analyze file content to find exact matches.'
          }
        </p>

        {!isScanning && (
          <button
            onClick={handleFolderClick}
            className="inline-flex items-center gap-3 px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-medium rounded-lg hover:from-blue-600 hover:to-purple-600 transform hover:scale-105 transition-all shadow-lg"
          >
            <Search size={20} />
            Browse Folder
          </button>
        )}

        {isScanning && (
          <div className="mt-4">
            <div className="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
              <div className="bg-gradient-to-r from-blue-500 to-purple-500 h-2 rounded-full animate-pulse"></div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default FileScanner;