import React from 'react';
import { motion } from 'framer-motion';
import { 
  Server, 
  Cpu, 
  HardDrive, 
  Wifi, 
  CheckCircle, 
  AlertTriangle,
  TrendingUp,
  Activity
} from 'lucide-react';

export function RedisHealth({ data }) {
  if (!data) return null;

  const parseMemoryInfo = (info) => {
    if (!info) return null;
    
    const lines = info.split('\n');
    const memoryInfo = {};
    
    lines.forEach(line => {
      if (line.includes('used_memory_human')) {
        memoryInfo.used = line.split(':')[1]?.trim();
      }
      if (line.includes('maxmemory_human')) {
        memoryInfo.max = line.split(':')[1]?.trim();
      }
      if (line.includes('used_memory_peak_human')) {
        memoryInfo.peak = line.split(':')[1]?.trim();
      }
      if (line.includes('mem_fragmentation_ratio')) {
        memoryInfo.fragmentation = parseFloat(line.split(':')[1]?.trim());
      }
    });
    
    return memoryInfo;
  };

  const memoryInfo = parseMemoryInfo(data.info);
  const isConnected = data.connected;
  const fragmentation = memoryInfo?.fragmentation || 0;

  const getFragmentationColor = (ratio) => {
    if (ratio < 1.1) return 'text-green-600';
    if (ratio < 1.3) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getFragmentationStatus = (ratio) => {
    if (ratio < 1.1) return 'Excellent';
    if (ratio < 1.3) return 'Good';
    return 'Needs Attention';
  };

  return (
    <motion.div 
      initial={{ y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="card"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-gradient-to-r from-red-600 to-pink-600 rounded-xl flex items-center justify-center">
            <Server className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Dragonfly Health</h2>
            <p className="text-sm text-gray-600">Memory & Performance</p>
          </div>
        </div>
        <motion.div 
          className={`status-indicator ${isConnected ? 'status-healthy' : 'status-error'}`}
          whileHover={{ scale: 1.05 }}
        >
          {isConnected ? <CheckCircle className="w-4 h-4 mr-2" /> : <AlertTriangle className="w-4 h-4 mr-2" />}
          {isConnected ? 'CONNECTED' : 'DISCONNECTED'}
        </motion.div>
      </div>

      <div className="space-y-4">
        {/* Connection Status */}
        <div className="bg-gradient-to-r from-blue-50 to-cyan-50 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-gray-600">Connection Status</span>
            <Wifi className="w-4 h-4 text-blue-600" />
          </div>
          <div className="flex items-center space-x-2">
            <motion.div 
              className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'} animate-pulse`}
              animate={{ scale: [1, 1.2, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            />
            <span className="text-lg font-semibold text-gray-900">
              {isConnected ? 'Connected' : 'Disconnected'}
            </span>
          </div>
          <div className="text-xs text-gray-500 mt-1">
            Last check: {data.timestamp ? new Date(data.timestamp).toLocaleTimeString() : 'Unknown'}
          </div>
        </div>

        {/* Memory Usage */}
        {memoryInfo && (
          <div className="bg-gradient-to-r from-green-50 to-emerald-50 rounded-lg p-4">
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-medium text-gray-600">Memory Usage</span>
              <HardDrive className="w-4 h-4 text-green-600" />
            </div>
            
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Used Memory</span>
                <span className="font-semibold text-gray-900">{memoryInfo.used || 'Unknown'}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Max Memory</span>
                <span className="font-semibold text-gray-900">{memoryInfo.max || 'Unknown'}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Peak Memory</span>
                <span className="font-semibold text-gray-900">{memoryInfo.peak || 'Unknown'}</span>
              </div>
            </div>
          </div>
        )}

        {/* Fragmentation */}
        {fragmentation > 0 && (
          <div className="bg-gradient-to-r from-purple-50 to-violet-50 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-600">Memory Fragmentation</span>
              <Activity className="w-4 h-4 text-purple-600" />
            </div>
            <div className="flex items-center justify-between">
              <div>
                <div className={`text-lg font-semibold ${getFragmentationColor(fragmentation)}`}>
                  {fragmentation.toFixed(2)}x
                </div>
                <div className="text-xs text-gray-500">
                  {getFragmentationStatus(fragmentation)}
                </div>
              </div>
              <div className="w-16 h-16 relative">
                <svg className="w-16 h-16 transform -rotate-90" viewBox="0 0 36 36">
                  <path
                    className="text-gray-200"
                    strokeWidth="3"
                    fill="none"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                  <path
                    className={fragmentation < 1.3 ? 'text-green-500' : 'text-red-500'}
                    strokeWidth="3"
                    strokeLinecap="round"
                    fill="none"
                    strokeDasharray={`${Math.min(fragmentation * 20, 100)}, 100`}
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-xs font-semibold text-gray-700">
                    {Math.round(Math.min(fragmentation * 20, 100))}%
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Performance Indicator */}
        <div className="bg-gradient-to-r from-orange-50 to-red-50 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-gray-600">Performance</span>
            <TrendingUp className="w-4 h-4 text-orange-600" />
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span className="text-lg font-semibold text-gray-900">Optimized</span>
          </div>
          <div className="text-xs text-gray-500 mt-1">
            Dragonfly provides 3.8x better performance than Redis
          </div>
        </div>
      </div>
    </motion.div>
  );
}
