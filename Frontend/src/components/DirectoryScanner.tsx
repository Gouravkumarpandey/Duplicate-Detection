import React, { useState } from 'react';
import { FolderOpen, Plus, Trash2, Search, Loader, Settings } from 'lucide-react';

interface DirectoryScannerProps {
  onDirectoryScan: (directories: string[]) => void;
  isScanning: boolean;
}

const DirectoryScanner: React.FC<DirectoryScannerProps> = ({ onDirectoryScan, isScanning }) => {
  const [directories, setDirectories] = useState<string[]>([
    '/Applications',
    '/Downloads',
    '/Program Files',
    '/Program Files (x86)'
  ]);
  const [newDirectory, setNewDirectory] = useState('');

  const addDirectory = () => {
    if (newDirectory.trim() && !directories.includes(newDirectory.trim())) {
      setDirectories([...directories, newDirectory.trim()]);
      setNewDirectory('');
    }
  };

  const removeDirectory = (index: number) => {
    setDirectories(directories.filter((_, i) => i !== index));
  };

  const handleScan = () => {
    if (directories.length > 0) {
      onDirectoryScan(directories);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      addDirectory();
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-lg border border-gray-100 p-8 mb-8">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 bg-gradient-to-r from-orange-500 to-red-500 rounded-lg text-white">
            <Settings size={24} />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-gray-800">Directory Scanner</h2>
            <p className="text-gray-600">Configure directories to scan for duplicate applications</p>
          </div>
        </div>
      </div>

      {/* Directory List */}
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-gray-800 mb-3">Scan Directories</h3>
        <div className="space-y-2 mb-4">
          {directories.map((dir, index) => (
            <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200">
              <div className="flex items-center gap-3">
                <FolderOpen size={16} className="text-gray-500" />
                <span className="text-gray-700 font-medium">{dir}</span>
              </div>
              <button
                onClick={() => removeDirectory(index)}
                className="p-1 text-red-500 hover:bg-red-50 rounded transition-colors"
              >
                <Trash2 size={16} />
              </button>
            </div>
          ))}
        </div>

        {/* Add Directory */}
        <div className="flex gap-2">
          <input
            type="text"
            value={newDirectory}
            onChange={(e) => setNewDirectory(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Enter directory path (e.g., /Applications)"
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={addDirectory}
            className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
          >
            <Plus size={16} />
            Add
          </button>
        </div>
      </div>

      {/* Scan Configuration */}
      <div className="bg-gradient-to-r from-blue-50 to-purple-50 p-4 rounded-lg mb-6">
        <h4 className="font-semibold text-gray-800 mb-2">Scan Configuration</h4>
        <div className="text-sm text-gray-600 space-y-1">
          <p>• Content-based duplicate detection using SHA-256 hashing</p>
          <p>• Rule-based categorization (Browsers, Media Players, Developer Tools, etc.)</p>
          <p>• Comprehensive logging of all operations</p>
          <p>• Safe deletion with confirmation dialogs</p>
        </div>
      </div>

      {/* Scan Button */}
      <div className="text-center">
        {isScanning ? (
          <div className="inline-flex items-center gap-3 px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-medium rounded-lg">
            <Loader className="animate-spin" size={20} />
            Scanning Directories...
          </div>
        ) : (
          <button
            onClick={handleScan}
            disabled={directories.length === 0}
            className="inline-flex items-center gap-3 px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-medium rounded-lg hover:from-blue-600 hover:to-purple-600 transform hover:scale-105 transition-all shadow-lg disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            <Search size={20} />
            Scan {directories.length} Director{directories.length === 1 ? 'y' : 'ies'}
          </button>
        )}
      </div>

      {/* Sample Directories Info */}
      <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
        <h4 className="font-semibold text-yellow-800 mb-2">Common Application Directories</h4>
        <div className="text-sm text-yellow-700 grid grid-cols-1 md:grid-cols-2 gap-2">
          <div>
            <strong>Windows:</strong>
            <ul className="ml-4 list-disc">
              <li>C:\Program Files</li>
              <li>C:\Program Files (x86)</li>
              <li>C:\Users\[Username]\AppData</li>
            </ul>
          </div>
          <div>
            <strong>macOS:</strong>
            <ul className="ml-4 list-disc">
              <li>/Applications</li>
              <li>/Users/[Username]/Applications</li>
              <li>/Users/[Username]/Downloads</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DirectoryScanner;