import React from 'react';
import { Copy, FileX, Check } from 'lucide-react';
import { DuplicateGroup } from '../types/FileTypes';
import { formatFileSize } from '../utils/fileUtils';

interface DuplicateListProps {
  duplicates: DuplicateGroup[];
  selectedFiles: Set<string>;
  onFileSelection: (filePath: string, selected: boolean) => void;
}

const DuplicateList: React.FC<DuplicateListProps> = ({ duplicates, selectedFiles, onFileSelection }) => {
  return (
    <div className="space-y-6">
      {duplicates.map((group, index) => (
        <div key={group.hash} className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
          <div className="bg-gradient-to-r from-red-50 to-orange-50 border-b border-gray-100 p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-red-100 rounded-lg">
                  <Copy className="text-red-600" size={20} />
                </div>
                <div>
                  <h3 className="font-semibold text-gray-800">
                    Duplicate Group #{index + 1}
                  </h3>
                  <p className="text-sm text-gray-600">
                    {group.count} identical files • {formatFileSize(group.files[0].size)} each
                  </p>
                </div>
              </div>
              <div className="text-right">
                <div className="text-sm font-medium text-red-600">
                  Wasting {formatFileSize(group.files[0].size * (group.count - 1))}
                </div>
                <div className="text-xs text-gray-500">
                  Hash: {group.hash.substring(0, 12)}...
                </div>
              </div>
            </div>
          </div>

          <div className="p-4">
            <div className="space-y-3">
              {group.files.map((file, fileIndex) => {
                const isSelected = selectedFiles.has(file.path);
                const isOriginal = fileIndex === 0;

                return (
                  <div
                    key={file.path}
                    className={`flex items-center justify-between p-3 rounded-lg border transition-all ${
                      isSelected
                        ? 'bg-red-50 border-red-200'
                        : isOriginal
                        ? 'bg-green-50 border-green-200'
                        : 'bg-gray-50 border-gray-200 hover:bg-gray-100'
                    }`}
                  >
                    <div className="flex items-center gap-3 flex-1">
                      <div className="flex-shrink-0">
                        {isOriginal ? (
                          <div className="w-6 h-6 bg-green-500 rounded-full flex items-center justify-center">
                            <Check className="text-white" size={14} />
                          </div>
                        ) : (
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={(e) => onFileSelection(file.path, e.target.checked)}
                            className="w-5 h-5 text-red-500 border-2 border-gray-300 rounded focus:ring-red-500"
                          />
                        )}
                      </div>
                      
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <FileX size={16} className={isOriginal ? 'text-green-600' : 'text-gray-500'} />
                          <span className={`font-medium truncate ${isOriginal ? 'text-green-700' : 'text-gray-700'}`}>
                            {file.name}
                          </span>
                          {isOriginal && (
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                              Keep Original
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-gray-500 truncate mt-1">
                          {file.path}
                        </p>
                      </div>

                      <div className="text-right">
                        <div className="text-sm font-medium text-gray-700">
                          {formatFileSize(file.size)}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default DuplicateList;