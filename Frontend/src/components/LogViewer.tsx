import React, { useState } from 'react';
import { FileText, Filter, Calendar, Hash, Trash2, Search, Eye } from 'lucide-react';
import { LogEntry } from '../types/FileTypes';

interface LogViewerProps {
  logs: LogEntry[];
}

const LogViewer: React.FC<LogViewerProps> = ({ logs }) => {
  const [filterType, setFilterType] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const filteredLogs = logs.filter(log => {
    const matchesType = filterType === 'ALL' || log.type === filterType;
    const matchesSearch = searchTerm === '' || 
      log.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (log.filePath && log.filePath.toLowerCase().includes(searchTerm.toLowerCase()));
    
    return matchesType && matchesSearch;
  });

  const getLogTypeColor = (type: LogEntry['type']) => {
    switch (type) {
      case 'SCAN': return 'bg-blue-100 text-blue-800';
      case 'DUPLICATE': return 'bg-red-100 text-red-800';
      case 'CATEGORY': return 'bg-purple-100 text-purple-800';
      case 'DELETE': return 'bg-orange-100 text-orange-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getLogTypeIcon = (type: LogEntry['type']) => {
    switch (type) {
      case 'SCAN': return <Search size={14} />;
      case 'DUPLICATE': return <Eye size={14} />;
      case 'CATEGORY': return <Filter size={14} />;
      case 'DELETE': return <Trash2 size={14} />;
      default: return <FileText size={14} />;
    }
  };

  const logTypeCounts = logs.reduce((acc, log) => {
    acc[log.type] = (acc[log.type] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div className="space-y-6">
      {/* Log Statistics */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
          <div className="text-2xl font-bold text-gray-600">{logs.length}</div>
          <div className="text-gray-500 text-sm">Total Logs</div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
          <div className="text-2xl font-bold text-blue-600">{logTypeCounts.SCAN || 0}</div>
          <div className="text-gray-500 text-sm">Scans</div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
          <div className="text-2xl font-bold text-red-600">{logTypeCounts.DUPLICATE || 0}</div>
          <div className="text-gray-500 text-sm">Duplicates</div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
          <div className="text-2xl font-bold text-purple-600">{logTypeCounts.CATEGORY || 0}</div>
          <div className="text-gray-500 text-sm">Categories</div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
          <div className="text-2xl font-bold text-orange-600">{logTypeCounts.DELETE || 0}</div>
          <div className="text-gray-500 text-sm">Deletions</div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <Filter size={16} className="text-gray-500" />
            <span className="text-sm font-medium text-gray-700">Filter by type:</span>
          </div>
          
          <div className="flex gap-2">
            {['ALL', 'SCAN', 'DUPLICATE', 'CATEGORY', 'DELETE'].map(type => (
              <button
                key={type}
                onClick={() => setFilterType(type)}
                className={`px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                  filterType === type
                    ? 'bg-blue-500 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {type} {type !== 'ALL' && `(${logTypeCounts[type] || 0})`}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2 ml-auto">
            <Search size={16} className="text-gray-500" />
            <input
              type="text"
              placeholder="Search logs..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="px-3 py-1 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>
      </div>

      {/* Log Entries */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
        <div className="bg-gradient-to-r from-gray-50 to-gray-100 border-b border-gray-200 p-4">
          <div className="flex items-center gap-3">
            <FileText className="text-gray-600" size={20} />
            <h3 className="font-semibold text-gray-800">
              Activity Logs ({filteredLogs.length})
            </h3>
          </div>
        </div>

        <div className="max-h-96 overflow-y-auto">
          {filteredLogs.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              <FileText size={48} className="mx-auto mb-4 text-gray-300" />
              <p>No logs match your current filters.</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {filteredLogs.map((log, index) => (
                <div key={index} className="p-4 hover:bg-gray-50 transition-colors">
                  <div className="flex items-start gap-3">
                    <div className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${getLogTypeColor(log.type)}`}>
                      {getLogTypeIcon(log.type)}
                      {log.type}
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <Calendar size={14} className="text-gray-400" />
                        <span className="text-sm text-gray-500">
                          {new Date(log.timestamp).toLocaleString()}
                        </span>
                      </div>
                      
                      <p className="text-gray-800 font-medium mb-1">{log.message}</p>
                      
                      {log.filePath && (
                        <p className="text-sm text-gray-600 truncate">
                          📁 {log.filePath}
                        </p>
                      )}
                      
                      {log.hash && (
                        <div className="flex items-center gap-1 mt-1">
                          <Hash size={12} className="text-gray-400" />
                          <span className="text-xs text-gray-500 font-mono">
                            {log.hash.substring(0, 16)}...
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default LogViewer;