import { ApiResponse, ScanResult, LogEntry, FileInfo, SerializableFileInfo } from '../types/FileTypes';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export class ApiService {
  private async request<T>(
    endpoint: string, 
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        headers: {
          'Content-Type': 'application/json',
          ...options.headers,
        },
        ...options,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`API request failed: ${endpoint}`, error);
      throw error;
    }
  }

  // Scanning endpoints
  async scanDirectories(
    directories: string[],
    includeSubdirectories: boolean = true,
    fileExtensions?: string[],
    excludePatterns?: string[]
  ): Promise<ScanResult> {
    const response = await this.request<ScanResult>('/scan', {
      method: 'POST',
      body: JSON.stringify({
        directories,
        includeSubdirectories,
        fileExtensions,
        excludePatterns
      }),
    });

    if (!response.success) {
      throw new Error(response.message || 'Scan failed');
    }

    return response.data!;
  }

  async getCommonDirectories(): Promise<{ platform: string; directories: string[] }> {
    const response = await this.request<{ platform: string; directories: string[] }>('/scan/directories');

    if (!response.success) {
      throw new Error(response.message || 'Failed to get directories');
    }

    return response.data!;
  }

  // File operations
  async deleteFiles(filePaths: string[]): Promise<{
    deletedFiles: string[];
    failedFiles: string[];
    totalRequested: number;
    successCount: number;
    failureCount: number;
  }> {
    const response = await this.request<{
      deletedFiles: string[];
      failedFiles: string[];
      totalRequested: number;
      successCount: number;
      failureCount: number;
    }>('/files/delete', {
      method: 'POST',
      body: JSON.stringify({
        filePaths,
        confirmed: true
      }),
    });

    if (!response.success && response.data?.failureCount === response.data?.totalRequested) {
      throw new Error(response.message || 'All deletions failed');
    }

    return response.data!;
  }

  async saveFilesToDB(files: SerializableFileInfo[]): Promise<{
    savedFiles: string[];
    failedFiles: string[];
    totalRequested: number;
    successCount: number;
    failureCount: number;
  }> {
    const response = await this.request<{
      savedFiles: string[];
      failedFiles: string[];
      totalRequested: number;
      successCount: number;
      failureCount: number;
    }>('/files/save', {
      method: 'POST',
      body: JSON.stringify(files.map(file => ({
        path: file.path,
        name: file.name,
        size: file.size,
        hash: file.hash,
        category: file.category,
        extension: file.extension
      }))),
    });

    if (!response.success && response.data?.failureCount === response.data?.totalRequested) {
      throw new Error(response.message || 'All saves failed');
    }

    return response.data!;
  }

  async validateFiles(filePaths: string[]): Promise<{
    validationResults: Array<{
      filePath: string;
      exists: boolean;
      valid: boolean;
      accessible: boolean;
    }>;
    accessibleFiles: string[];
    inaccessibleFiles: string[];
    totalFiles: number;
    accessibleCount: number;
    inaccessibleCount: number;
  }> {
    const response = await this.request<{
      validationResults: Array<{
        filePath: string;
        exists: boolean;
        valid: boolean;
        accessible: boolean;
      }>;
      accessibleFiles: string[];
      inaccessibleFiles: string[];
      totalFiles: number;
      accessibleCount: number;
      inaccessibleCount: number;
    }>('/files/validate', {
      method: 'POST',
      body: JSON.stringify({ filePaths }),
    });

    if (!response.success) {
      throw new Error(response.message || 'File validation failed');
    }

    return response.data!;
  }

  async getFileInfo(filePath: string): Promise<FileInfo> {
    const response = await this.request<FileInfo>(`/files/info?filePath=${encodeURIComponent(filePath)}`);

    if (!response.success) {
      throw new Error(response.message || 'Failed to get file info');
    }

    return response.data!;
  }

  // Logging endpoints
  async getLogs(limit?: number, type?: string): Promise<{
    logs: LogEntry[];
    totalLogs: number;
    filteredCount: number;
  }> {
    const params = new URLSearchParams();
    if (limit) params.append('limit', limit.toString());
    if (type) params.append('type', type);

    const response = await this.request<{
      logs: LogEntry[];
      totalLogs: number;
      filteredCount: number;
    }>(`/logs?${params.toString()}`);

    if (!response.success) {
      throw new Error(response.message || 'Failed to get logs');
    }

    return response.data!;
  }

  async getLogStats(): Promise<{ total: number; byType: Record<string, number> }> {
    const response = await this.request<{ total: number; byType: Record<string, number> }>('/logs/stats');

    if (!response.success) {
      throw new Error(response.message || 'Failed to get log stats');
    }

    return response.data!;
  }

  async clearLogs(): Promise<void> {
    const response = await this.request('/logs', {
      method: 'DELETE',
    });

    if (!response.success) {
      throw new Error(response.message || 'Failed to clear logs');
    }
  }

  async exportLogs(): Promise<string> {
    try {
      const response = await fetch(`${API_BASE_URL}/logs/export`);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.text();
    } catch (error) {
      console.error('Failed to export logs:', error);
      throw error;
    }
  }

  // Health check
  async healthCheck(): Promise<{
    status: string;
    timestamp: string;
    environment: string;
  }> {
    const response = await this.request<{
      status: string;
      timestamp: string;
      environment: string;
    }>('/health');

    if (!response.success) {
      throw new Error(response.message || 'Health check failed');
    }

    return response.data!;
  }
}

// Export singleton instance
export const apiService = new ApiService();
