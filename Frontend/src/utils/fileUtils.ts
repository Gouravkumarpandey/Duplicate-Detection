import { FileInfo, CategoryRules, CategoryRule, LogEntry } from '../types/FileTypes';
import rulesConfig from '../config/rules.json';

export const generateFileHash = async (file: File): Promise<string> => {
  const arrayBuffer = await file.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest('SHA-256', arrayBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
};

export const formatFileSize = (bytes: number): string => {
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  if (bytes === 0) return '0 Bytes';
  
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const formattedSize = (bytes / Math.pow(1024, i)).toFixed(2);
  
  return `${formattedSize} ${sizes[i]}`;
};

export const categorizeFiles = (files: FileInfo[]): { categories: Record<string, FileInfo[]>, logs: LogEntry[] } => {
  const categories: Record<string, FileInfo[]> = {};
  const logs: LogEntry[] = [];
  const rules = rulesConfig as CategoryRules;
  
  files.forEach(file => {
    let categorized = false;
    const fileName = file.name.toLowerCase();
    const filePath = file.path.toLowerCase();
    const extension = getFileExtension(file.name).toLowerCase();
    
    for (const rule of rules.rules) {
      if (matchesRule(fileName, filePath, extension, rule)) {
        if (!categories[rule.name]) {
          categories[rule.name] = [];
        }
        
        const categorizedFile = { ...file, category: rule.name, extension };
        categories[rule.name].push(categorizedFile);
        categorized = true;
        
        logs.push({
          timestamp: new Date().toISOString(),
          type: 'CATEGORY',
          message: `File categorized as ${rule.name}`,
          filePath: file.path,
          hash: file.hash
        });
        
        break;
      }
    }
    
    if (!categorized) {
      if (!categories['Uncategorized']) {
        categories['Uncategorized'] = [];
      }
      const uncategorizedFile = { ...file, category: 'Uncategorized', extension };
      categories['Uncategorized'].push(uncategorizedFile);
      
      logs.push({
        timestamp: new Date().toISOString(),
        type: 'CATEGORY',
        message: 'File marked as uncategorized',
        filePath: file.path,
        hash: file.hash
      });
    }
  });
  
  return { categories, logs };
};

const matchesRule = (fileName: string, filePath: string, extension: string, rule: CategoryRule): boolean => {
  const { conditions } = rule;
  
  // Check path contains
  if (conditions.pathContains) {
    if (conditions.pathContains.some(keyword => filePath.includes(keyword.toLowerCase()))) {
      return true;
    }
  }
  
  // Check filename contains
  if (conditions.filenameContains) {
    if (conditions.filenameContains.some(keyword => fileName.includes(keyword.toLowerCase()))) {
      return true;
    }
  }
  
  // Check extensions
  if (conditions.extensions) {
    if (conditions.extensions.some(ext => extension === ext.toLowerCase())) {
      return true;
    }
  }
  
  return false;
};

const getFileExtension = (filename: string): string => {
  const lastDotIndex = filename.lastIndexOf('.');
  return lastDotIndex !== -1 ? filename.substring(lastDotIndex) : '';
};

export const createLogEntry = (type: LogEntry['type'], message: string, filePath?: string, hash?: string): LogEntry => {
  return {
    timestamp: new Date().toISOString(),
    type,
    message,
    filePath,
    hash
  };
};

export const exportLogs = (logs: LogEntry[]): string => {
  return logs.map(log => {
    const timestamp = new Date(log.timestamp).toLocaleString();
    const filePath = log.filePath ? ` | File: ${log.filePath}` : '';
    const hash = log.hash ? ` | Hash: ${log.hash.substring(0, 12)}...` : '';
    return `[${timestamp}] ${log.type}: ${log.message}${filePath}${hash}`;
  }).join('\n');
};