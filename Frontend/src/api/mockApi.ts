import { ApiResponse, LogEntry, ScanResult, DuplicateGroup, FileInfo } from '../types/FileTypes';
import { createLogEntry, categorizeFiles } from '../utils/fileUtils';

// Mock API functions that simulate the Java backend endpoints
// In production, replace these with actual fetch calls to your Spring Boot API

export const mockApiScan = async (files: FileInfo[]): Promise<ScanResult> => {
  // Simulate API delay
  await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 2000));
  
  const logs: LogEntry[] = [];
  
  // Log scanning start
  logs.push(createLogEntry('SCAN', `Started scanning ${files.length} files`));
  
  // Group files by hash to find duplicates
  const hashGroups: Record<string, FileInfo[]> = {};
  
  files.forEach(file => {
    logs.push(createLogEntry('SCAN', `Scanned file`, file.path, file.hash));
    
    if (!hashGroups[file.hash]) {
      hashGroups[file.hash] = [];
    }
    hashGroups[file.hash].push(file);
  });
  
  // Create duplicate groups (only groups with more than one file)
  const duplicateGroups: DuplicateGroup[] = [];
  
  Object.entries(hashGroups).forEach(([hash, groupFiles]) => {
    if (groupFiles.length > 1) {
      duplicateGroups.push({
        hash,
        files: groupFiles,
        count: groupFiles.length,
        category: groupFiles[0].category
      });
      
      // Log duplicate detection
      logs.push(createLogEntry(
        'DUPLICATE', 
        `Found ${groupFiles.length} duplicate files`,
        groupFiles.map(f => f.path).join(', '),
        hash
      ));
    }
  });
  
  // Categorize files
  const { categories, logs: categoryLogs } = categorizeFiles(files);
  logs.push(...categoryLogs);
  
  logs.push(createLogEntry('SCAN', `Scan completed. Found ${duplicateGroups.length} duplicate groups`));
  
  return {
    totalFiles: files.length,
    duplicateGroups,
    categories,
    logs
  };
};

export const mockApiGetLogs = async (): Promise<LogEntry[]> => {
  // In real implementation, this would read from logs.txt file
  const storedLogs = localStorage.getItem('duplicateRemoverLogs');
  return storedLogs ? JSON.parse(storedLogs) : [];
};

export const mockApiSaveLogs = async (logs: LogEntry[]): Promise<void> => {
  // In real implementation, this would write to logs.txt file
  localStorage.setItem('duplicateRemoverLogs', JSON.stringify(logs));
};

// Mock directory scanning (simulates Java backend scanning directories)
export const mockApiScanDirectories = async (directories: string[]): Promise<ScanResult> => {
  await new Promise(resolve => setTimeout(resolve, 2000));
  
  const logs: LogEntry[] = [];
  logs.push(createLogEntry('SCAN', `Started scanning directories: ${directories.join(', ')}`));
  
  // Simulate finding files in directories
  const mockFiles: FileInfo[] = [
    {
      path: '/Applications/Chrome.app',
      name: 'Chrome.app',
      size: 150000000,
      hash: 'a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456',
      file: new File([], 'Chrome.app'),
      category: 'Browser',
      extension: '.app'
    },
    {
      path: '/Downloads/chrome-installer.exe',
      name: 'chrome-installer.exe',
      size: 150000000,
      hash: 'a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456', // Same hash as above
      file: new File([], 'chrome-installer.exe'),
      category: 'Browser',
      extension: '.exe'
    },
    {
      path: '/Applications/VLC.app',
      name: 'VLC.app',
      size: 80000000,
      hash: 'b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234567',
      file: new File([], 'VLC.app'),
      category: 'Media Player',
      extension: '.app'
    },
    {
      path: '/Downloads/vlc-setup.exe',
      name: 'vlc-setup.exe',
      size: 80000000,
      hash: 'b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234567', // Same hash as VLC
      file: new File([], 'vlc-setup.exe'),
      category: 'Media Player',
      extension: '.exe'
    }
  ];
  
  return mockApiScan(mockFiles);
};

export const mockApiDelete = async (filePaths: string[]): Promise<ApiResponse> => {
  // Simulate API delay
  await new Promise(resolve => setTimeout(resolve, 500));
  
  const logs: LogEntry[] = await mockApiGetLogs();
  
  // Log deletion events
  filePaths.forEach(path => {
    logs.push(createLogEntry('DELETE', `File deleted`, path));
  });
  
  await mockApiSaveLogs(logs);
  
  // In a real implementation, this would delete actual files
  console.log('Mock delete operation for files:', filePaths);
  
  // Simulate some random failures for demo purposes
  if (Math.random() < 0.1) {
    throw new Error('Simulated delete failure');
  }
  
  return {
    success: true,
    message: `Successfully deleted ${filePaths.length} files`,
    logs: logs.slice(-10) // Return last 10 log entries
  };
};

export const mockApiSaveToDb = async (files: FileInfo[]): Promise<ApiResponse> => {
  // Simulate API delay
  await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 1000));
  
  const logs: LogEntry[] = await mockApiGetLogs();
  
  // Log database save events
  files.forEach(file => {
    logs.push(createLogEntry('SCAN', `File saved to database`, file.path, file.hash));
  });
  
  await mockApiSaveLogs(logs);
  
  // In a real implementation, this would save files to the actual database
  console.log('Mock save to DB operation for files:', files.map(f => f.path));
  
  // Simulate some random failures for demo purposes
  const failureRate = 0.05; // 5% failure rate
  const savedFiles: string[] = [];
  const failedFiles: string[] = [];
  
  files.forEach(file => {
    if (Math.random() < failureRate) {
      failedFiles.push(file.path);
    } else {
      savedFiles.push(file.path);
    }
  });
  
  return {
    success: true,
    message: `Successfully saved ${savedFiles.length} files to database${failedFiles.length > 0 ? `, ${failedFiles.length} failed` : ''}`,
    data: {
      savedFiles,
      failedFiles,
      totalRequested: files.length,
      successCount: savedFiles.length,
      failureCount: failedFiles.length
    },
    logs: logs.slice(-10) // Return last 10 log entries
  };
};

// New API endpoints for the enhanced functionality
export const mockApiGetScan = async (): Promise<ApiResponse> => {
  const logs = await mockApiGetLogs();
  const recentScans = logs.filter(log => log.type === 'SCAN').slice(-5);
    
  return {
    success: true,
    data: {
      recentScans,
      totalScans: recentScans.length
    }
  };
};

export const mockApiRemove = async (filePaths: string[]): Promise<ApiResponse> => {
  return mockApiDelete(filePaths);
};

export const mockApiGetLogsEndpoint = async (): Promise<ApiResponse> => {
  const logs = await mockApiGetLogs();
  
  return {
    success: true,
    data: {
      logs: logs.slice(-50), // Return last 50 log entries
      totalLogs: logs.length
    }
  };
};

// Simulate the Java backend REST endpoints
export const simulateJavaBackend = {
  // GET /scan
  getScan: mockApiGetScan,
  
  // POST /remove
  postRemove: mockApiRemove,
  
  // GET /logs
  getLogs: mockApiGetLogsEndpoint,
  
  // POST /scan-directories
  postScanDirectories: mockApiScanDirectories
};