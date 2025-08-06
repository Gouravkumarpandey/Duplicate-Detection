import React, { useState, useCallback } from 'react';
import { FolderOpen, Search, Trash2, FileX, CheckSquare, Square, FileText, Download } from 'lucide-react';
import FileScanner from './components/FileScanner';
import DuplicateList from './components/DuplicateList';
import CategoryView from './components/CategoryView';
import LogViewer from './components/LogViewer';
import DirectoryScanner from './components/DirectoryScanner';
import { FileInfo, DuplicateGroup, LogEntry } from './types/FileTypes';
import { generateFileHash, exportLogs } from './utils/fileUtils';
import { apiService } from './api/ApiService';

function App() {
  const [files, setFiles] = useState<FileInfo[]>([]);
  const [duplicates, setDuplicates] = useState<DuplicateGroup[]>([]);
  const [categories, setCategories] = useState<Record<string, FileInfo[]>>({});
  const [selectedFiles, setSelectedFiles] = useState<Set<string>>(new Set());
  const [isScanning, setIsScanning] = useState(false);
  const [activeTab, setActiveTab] = useState<'duplicates' | 'categories' | 'logs' | 'directories'>('duplicates');
  const [logs, setLogs] = useState<LogEntry[]>([]);

  const handleFolderSelect = useCallback(async (selectedFiles: FileList) => {
    setIsScanning(true);
    const fileArray: FileInfo[] = [];
    
    // Convert FileList to array and process files
    for (let i = 0; i < selectedFiles.length; i++) {
      const file = selectedFiles[i];
      try {
        const hash = await generateFileHash(file);
        const extension = file.name.substring(file.name.lastIndexOf('.')) || '';
        fileArray.push({
          path: file.webkitRelativePath || file.name,
          name: file.name,
          size: file.size,
          hash,
          file,
          extension
        });
      } catch (error) {
        console.error(`Error processing file ${file.name}:`, error);
      }
    }

    setFiles(fileArray);

    // Scan files using mock API
    try {
      const scanResult = await mockApiScan(fileArray);
      setDuplicates(scanResult.duplicateGroups);
      setCategories(scanResult.categories);
      setLogs(prev => [...prev, ...scanResult.logs]);
      
      // Save logs to mock storage
      await mockApiSaveLogs([...logs, ...scanResult.logs]);
      
    } catch (error) {
      console.error('Error scanning files:', error);
    } finally {
      setIsScanning(false);
    }
  }, [logs]);

  const handleDirectoryScan = useCallback(async (directories: string[]) => {
    setIsScanning(true);
    try {
      const scanResult = await mockApiScanDirectories(directories);
      setFiles(scanResult.duplicateGroups.flatMap(group => group.files));
      setDuplicates(scanResult.duplicateGroups);
      setCategories(scanResult.categories);
      setLogs(prev => [...prev, ...scanResult.logs]);
      
      await mockApiSaveLogs([...logs, ...scanResult.logs]);
    } catch (error) {
      console.error('Error scanning directories:', error);
    } finally {
      setIsScanning(false);
    }
  }, [logs]);

  const handleFileSelection = useCallback((filePath: string, selected: boolean) => {
    setSelectedFiles(prev => {
      const newSet = new Set(prev);
      if (selected) {
        newSet.add(filePath);
      } else {
        newSet.delete(filePath);
      }
      return newSet;
    });
  }, []);

  const handleDeleteSelected = useCallback(async () => {
    if (selectedFiles.size === 0) return;

    const confirmed = window.confirm(
      `Are you sure you want to delete ${selectedFiles.size} selected files? This action cannot be undone.`
    );

    if (!confirmed) return;

    try {
      const filesToDelete = Array.from(selectedFiles);
      const response = await mockApiDelete(filesToDelete);
      
      // Remove deleted files from state
      setFiles(prev => prev.filter(f => !selectedFiles.has(f.path)));
      setDuplicates(prev => prev.map(group => ({
        ...group,
        files: group.files.filter(f => !selectedFiles.has(f.path))
      })).filter(group => group.files.length > 1));
      
      // Update categories by removing deleted files
      const newCategories = { ...categories };
      Object.keys(newCategories).forEach(category => {
        newCategories[category] = newCategories[category].filter(f => !selectedFiles.has(f.path));
        if (newCategories[category].length === 0) {
          delete newCategories[category];
        }
      });
      setCategories(newCategories);
      
      setSelectedFiles(new Set());
      
      // Update logs
      if (response.logs) {
        setLogs(prev => [...prev, ...response.logs!]);
      }
      
      alert(`Successfully deleted ${filesToDelete.length} files.`);
    } catch (error) {
      console.error('Error deleting files:', error);
      alert('Error deleting files. Please try again.');
    }
  }, [selectedFiles, files, categories]);

  const selectAllDuplicates = useCallback(() => {
    const allDuplicatePaths = duplicates.flatMap(group => 
      group.files.slice(1).map(f => f.path) // Skip first file in each group
    );
    setSelectedFiles(new Set(allDuplicatePaths));
  }, [duplicates]);

  const clearSelection = useCallback(() => {
    setSelectedFiles(new Set());
  }, []);

  const handleExportLogs = useCallback(() => {
    const logContent = exportLogs(logs);
    const blob = new Blob([logContent], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `duplicate-remover-logs-${new Date().toISOString().split('T')[0]}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, [logs]);
  const totalDuplicates = duplicates.reduce((sum, group) => sum + (group.count - 1), 0);
  const duplicateSize = duplicates.reduce((sum, group) => 
    sum + group.files.slice(1).reduce((fileSum, file) => fileSum + file.size, 0), 0
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-green-50">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-4">
            <div className="p-3 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full text-white mr-3">
              <FileX size={32} />
            </div>
            <h1 className="text-4xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
              Duplicate Application Remover
            </h1>
          </div>
          <p className="text-gray-600 text-lg max-w-2xl mx-auto">
            Scan your folders to find duplicate applications based on file content, categorize them automatically, 
            and remove unwanted duplicates to free up space.
          </p>
        </div>

        {/* File Scanner */}
        <FileScanner 
          onFolderSelect={handleFolderSelect}
          isScanning={isScanning}
          fileCount={files.length}
        />

        {/* Directory Scanner */}
        <DirectoryScanner 
          onDirectoryScan={handleDirectoryScan}
          isScanning={isScanning}
        />
        {/* Statistics */}
        {files.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
              <div className="text-2xl font-bold text-blue-600">{files.length}</div>
              <div className="text-gray-600">Total Files</div>
            </div>
            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
              <div className="text-2xl font-bold text-purple-600">{duplicates.length}</div>
              <div className="text-gray-600">Duplicate Groups</div>
            </div>
            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
              <div className="text-2xl font-bold text-red-600">{totalDuplicates}</div>
              <div className="text-gray-600">Duplicate Files</div>
            </div>
            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
              <div className="text-2xl font-bold text-green-600">
                {(duplicateSize / (1024 * 1024)).toFixed(1)}MB
              </div>
              <div className="text-gray-600">Potential Savings</div>
            </div>
          </div>
        )}

        {/* Action Bar */}
        {(duplicates.length > 0 || Object.keys(categories).length > 0 || logs.length > 0) && (
          <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100 mb-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="flex bg-gray-100 rounded-lg p-1">
                  <button
                    onClick={() => setActiveTab('duplicates')}
                    className={`px-4 py-2 rounded-md transition-all ${
                      activeTab === 'duplicates'
                        ? 'bg-white shadow-sm text-blue-600 font-medium'
                        : 'text-gray-600 hover:text-gray-800'
                    }`}
                  >
                    Duplicates ({duplicates.length})
                  </button>
                  <button
                    onClick={() => setActiveTab('categories')}
                    className={`px-4 py-2 rounded-md transition-all ${
                      activeTab === 'categories'
                        ? 'bg-white shadow-sm text-purple-600 font-medium'
                        : 'text-gray-600 hover:text-gray-800'
                    }`}
                  >
                    Categories ({Object.keys(categories).length})
                  </button>
                  <button
                    onClick={() => setActiveTab('logs')}
                    className={`px-4 py-2 rounded-md transition-all ${
                      activeTab === 'logs'
                        ? 'bg-white shadow-sm text-green-600 font-medium'
                        : 'text-gray-600 hover:text-gray-800'
                    }`}
                  >
                    Logs ({logs.length})
                  </button>
                  <button
                    onClick={() => setActiveTab('directories')}
                    className={`px-4 py-2 rounded-md transition-all ${
                      activeTab === 'directories'
                        ? 'bg-white shadow-sm text-orange-600 font-medium'
                        : 'text-gray-600 hover:text-gray-800'
                    }`}
                  >
                    Directory Scan
                  </button>
                </div>
              </div>

              <div className="flex items-center gap-3">
                {activeTab === 'logs' && logs.length > 0 && (
                  <button
                    onClick={handleExportLogs}
                    className="flex items-center gap-2 px-3 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
                  >
                    <Download size={16} />
                    Export Logs
                  </button>
                )}
                
                {activeTab === 'duplicates' && duplicates.length > 0 && (
                  <>
                    {selectedFiles.size > 0 && (
                      <span className="text-sm text-gray-600">
                        {selectedFiles.size} files selected
                      </span>
                    )}
                    <button
                      onClick={selectAllDuplicates}
                      className="flex items-center gap-2 px-3 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                    >
                      <CheckSquare size={16} />
                      Select All Duplicates
                    </button>
                    <button
                      onClick={clearSelection}
                      className="flex items-center gap-2 px-3 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
                    >
                      <Square size={16} />
                      Clear Selection
                    </button>
                    <button
                      onClick={handleDeleteSelected}
                      disabled={selectedFiles.size === 0}
                      className="flex items-center gap-2 px-3 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
                    >
                      <Trash2 size={16} />
                      Delete Selected ({selectedFiles.size})
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Content */}
        {activeTab === 'duplicates' && duplicates.length > 0 && (
          <DuplicateList
            duplicates={duplicates}
            selectedFiles={selectedFiles}
            onFileSelection={handleFileSelection}
          />
        )}

        {activeTab === 'categories' && Object.keys(categories).length > 0 && (
          <CategoryView
            categories={categories}
            selectedFiles={selectedFiles}
            onFileSelection={handleFileSelection}
          />
        )}

        {activeTab === 'logs' && (
          <LogViewer logs={logs} />
        )}

        {activeTab === 'directories' && (
          <DirectoryScanner 
            onDirectoryScan={handleDirectoryScan}
            isScanning={isScanning}
          />
        )}
        {/* Empty State */}
        {files.length === 0 && !isScanning && (
          <div className="text-center py-12">
            <FolderOpen size={64} className="mx-auto text-gray-400 mb-4" />
            <h3 className="text-xl font-semibold text-gray-600 mb-2">No folder selected</h3>
            <p className="text-gray-500">Select a folder or configure directory scanning to find duplicate applications.</p>
          </div>
        )}

        {files.length > 0 && duplicates.length === 0 && !isScanning && (
          <div className="text-center py-12">
            <CheckSquare size={64} className="mx-auto text-green-400 mb-4" />
            <h3 className="text-xl font-semibold text-gray-600 mb-2">No duplicates found</h3>
            <p className="text-gray-500">Your selected folder doesn't contain any duplicate files.</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;