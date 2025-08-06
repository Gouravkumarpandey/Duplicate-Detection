import { ApiService } from './ApiService';

export interface FileUploadResponse {
  success: boolean;
  isDuplicate: boolean;
  message: string;
  fileId?: string;
  fileName?: string;
  contentHash?: string;
  filePath?: string;
  existingFile?: {
    id: string;
    fileName: string;
    uploadDate: string;
    filePath: string;
    hash: string;
  };
}

export interface ContentExtractionResponse {
  fileName: string;
  fileExtension: string;
  contentLength: number;
  contentPreview: string;
  contentHash: string;
  mimeType: string;
  fileSize: number;
}

export interface DatabaseFile {
  id: string;
  fileName: string;
  filePath: string;
  fileHash: string;
  fileSize: number;
  fileExtension: string;
  mimeType: string;
  uploadedDate: string;
  isDuplicate: boolean;
  category?: string;
}

export interface DuplicatesResponse {
  duplicates: DatabaseFile[];
  totalCount: number;
  page: number;
  size: number;
}

export interface StorageInfoResponse {
  uploadDirectory: string;
  maxFileSize: string;
  maxFileSizeBytes: number;
  allowedExtensions: string[];
  allowedMimeTypes: string[];
  hashAlgorithm: string;
  contentExtraction: Record<string, string>;
  duplicateDetection: string;
}

export interface DeleteResponse {
  message: string;
}

export class RealApiService extends ApiService {
  
  /**
   * Upload file using the enhanced backend endpoint
   */
  async uploadFile(file: File): Promise<FileUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/uploads/upload`, {
        method: 'POST',
        body: formData,
        // Don't set Content-Type header, let browser set it for FormData
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('File upload failed:', error);
      throw error;
    }
  }

  /**
   * Test content extraction
   */
  async testContentExtraction(file: File): Promise<ContentExtractionResponse> {
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080/api'}/files/test-content-extraction`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Content extraction test failed:', error);
      throw error;
    }
  }

  /**
   * Get all files from database
   */
  async getAllFiles(): Promise<DatabaseFile[]> {
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/uploads/all`);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get files:', error);
      throw error;
    }
  }

  /**
   * Get duplicates from database
   */
  async getDuplicates(): Promise<DuplicatesResponse> {
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/uploads/duplicates`);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get duplicates:', error);
      throw error;
    }
  }

  /**
   * Get storage info from backend
   */
  async getStorageInfo(): Promise<StorageInfoResponse> {
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080/api'}/files/storage-info-enhanced`);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get storage info:', error);
      throw error;
    }
  }

  /**
   * Delete file by ID
   */
  async deleteFile(fileId: string): Promise<DeleteResponse> {
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080/api'}/files/${fileId}`, {
        method: 'DELETE',
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to delete file:', error);
      throw error;
    }
  }
}

// Export singleton instance
export const realApiService = new RealApiService();
