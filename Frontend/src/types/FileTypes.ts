export interface FileInfo {
  path: string;
  name: string;
  size: number;
  hash: string;
  file: File;
  category?: string;
  extension: string;
}

export interface DuplicateGroup {
  hash: string;
  files: FileInfo[];
  count: number;
  category?: string;
}

export interface CategoryRule {
  name: string;
  conditions: {
    pathContains?: string[];
    filenameContains?: string[];
    extensions?: string[];
  };
}

export interface CategoryRules {
  rules: CategoryRule[];
}

export interface ScanResult {
  totalFiles: number;
  duplicateGroups: DuplicateGroup[];
  categories: Record<string, FileInfo[]>;
  logs: LogEntry[];
}

export interface LogEntry {
  timestamp: string;
  type: 'SCAN' | 'DUPLICATE' | 'CATEGORY' | 'DELETE';
  message: string;
  filePath?: string;
  hash?: string;
  [category: string]: string[];
}

export interface ApiResponse {
  success: boolean;
  message?: string;
  data?: any;
  logs?: LogEntry[];
}