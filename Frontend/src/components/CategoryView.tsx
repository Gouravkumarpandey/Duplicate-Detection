import React, { useState } from 'react';
import { Folder, FolderOpen, File, ChevronDown, ChevronRight } from 'lucide-react';
import { FileInfo } from '../types/FileTypes';
import { formatFileSize } from '../utils/fileUtils';

interface CategoryViewProps {
  categories: Record<string, FileInfo[]>;
  selectedFiles: Set<string>;
  onFileSelection: (filePath: string, selected: boolean) => void;
}

const CategoryView: React.FC<CategoryViewProps> = ({ categories, selectedFiles, onFileSelection }) => {
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(new Set());

  const toggleCategory = (category: string) => {
    setExpandedCategories(prev => {
      const newSet = new Set(prev);
      if (newSet.has(category)) {
        newSet.delete(category);
      } else {
        newSet.add(category);
      }
      return newSet;
    });
  };

  const getCategoryColor = (category: string) => {
    const colors = {
      'Browsers': 'from-blue-500 to-blue-600',
      'Media Players': 'from-purple-500 to-purple-600',
      'Developer Tools': 'from-green-500 to-green-600',
      'Games': 'from-red-500 to-red-600',
      'Office': 'from-yellow-500 to-yellow-600',
      'System': 'from-gray-500 to-gray-600',
      'Uncategorized': 'from-gray-400 to-gray-500'
    };
    return colors[category as keyof typeof colors] || 'from-gray-400 to-gray-500';
  };

  return (
    <div className="space-y-4">
      {Object.entries(categories).map(([category, files]) => {
        const isExpanded = expandedCategories.has(category);
        const totalSize = files.reduce((sum, file) => sum + file.size, 0);
        
        return (
          <div key={category} className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
            <div
              onClick={() => toggleCategory(category)}
              className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-center gap-3">
                <div className={`p-2 bg-gradient-to-r ${getCategoryColor(category)} rounded-lg text-white`}>
                  {isExpanded ? <FolderOpen size={20} /> : <Folder size={20} />}
                </div>
                <div>
                  <h3 className="font-semibold text-gray-800">{category}</h3>
                  <p className="text-sm text-gray-600">
                    {files.length} files • {formatFileSize(totalSize)}
                  </p>
                </div>
              </div>
              
              <div className="flex items-center gap-3">
                {isExpanded ? (
                  <ChevronDown className="text-gray-400" size={20} />
                ) : (
                  <ChevronRight className="text-gray-400" size={20} />
                )}
              </div>
            </div>

            {isExpanded && (
              <div className="border-t border-gray-100 p-4">
                <div className="space-y-2">
                  {files.map((file) => {
                    const isSelected = selectedFiles.has(file.path);
                    
                    return (
                      <div
                        key={file.path}
                        className={`flex items-center justify-between p-3 rounded-lg border transition-all ${
                          isSelected
                            ? 'bg-blue-50 border-blue-200'
                            : 'bg-gray-50 border-gray-200 hover:bg-gray-100'
                        }`}
                      >
                        <div className="flex items-center gap-3 flex-1">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={(e) => onFileSelection(file.path, e.target.checked)}
                            className="w-4 h-4 text-blue-500 border-2 border-gray-300 rounded focus:ring-blue-500"
                          />
                          
                          <File size={16} className="text-gray-500 flex-shrink-0" />
                          
                          <div className="flex-1 min-w-0">
                            <div className="font-medium text-gray-700 truncate">
                              {file.name}
                            </div>
                            <p className="text-sm text-gray-500 truncate">
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
            )}
          </div>
        );
      })}
    </div>
  );
};

export default CategoryView;