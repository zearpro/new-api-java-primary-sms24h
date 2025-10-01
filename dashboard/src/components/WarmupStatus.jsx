import React from 'react';
import { motion } from 'framer-motion';
import { 
  Database, 
  Zap, 
  Activity, 
  AlertTriangle, 
  CheckCircle, 
  Clock,
  TrendingUp,
  Server,
  Cpu,
  HardDrive,
  Wifi
} from 'lucide-react';

export function WarmupStatus({ data }) {
  if (!data) return null;

  const getStatusColor = (status) => {
    switch (status) {
      case 'healthy': return 'text-green-600 bg-green-100';
      case 'warning': return 'text-yellow-600 bg-yellow-100';
      case 'error': return 'text-red-600 bg-red-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'healthy': return CheckCircle;
      case 'warning': return AlertTriangle;
      case 'error': return AlertTriangle;
      default: return Clock;
    }
  };

  const StatusIcon = getStatusIcon(data.status);

  return (
    <motion.div 
      initial={{ y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="card"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl flex items-center justify-center">
            <Zap className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Warmup Status</h2>
            <p className="text-sm text-gray-600">Cache warming progress</p>
          </div>
        </div>
        <motion.div 
          className={`status-indicator ${getStatusColor(data.status)}`}
          whileHover={{ scale: 1.05 }}
        >
          <StatusIcon className="w-4 h-4 mr-2" />
          {data.status?.toUpperCase()}
        </motion.div>
      </div>

      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-600">Tables Loaded</span>
              <Database className="w-4 h-4 text-blue-600" />
            </div>
            <div className="text-2xl font-bold text-gray-900">
              {Object.values(data.tables || {}).filter(t => t.count > 0).length}
            </div>
            <div className="text-xs text-gray-500">
              of {Object.keys(data.tables || {}).length} total
            </div>
          </div>

          <div className="bg-gradient-to-r from-green-50 to-emerald-50 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-600">Total Records</span>
              <HardDrive className="w-4 h-4 text-green-600" />
            </div>
            <div className="text-2xl font-bold text-gray-900">
              {Object.values(data.tables || {}).reduce((sum, t) => sum + (t.count || 0), 0).toLocaleString()}
            </div>
            <div className="text-xs text-gray-500">
              cached records
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-r from-purple-50 to-pink-50 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-gray-600">Last Update</span>
            <Clock className="w-4 h-4 text-purple-600" />
          </div>
          <div className="text-lg font-semibold text-gray-900">
            {data.timestamp ? new Date(data.timestamp).toLocaleTimeString() : 'Unknown'}
          </div>
          <div className="text-xs text-gray-500">
            {data.timestamp ? new Date(data.timestamp).toLocaleDateString() : ''}
          </div>
        </div>

        {data.velocity && (
          <div className="bg-gradient-to-r from-orange-50 to-red-50 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-600">Velocity Layer</span>
              <Activity className="w-4 h-4 text-orange-600" />
            </div>
            <div className="flex items-center space-x-2">
              <div className={`w-2 h-2 rounded-full ${data.velocity.healthy ? 'bg-green-500' : 'bg-red-500'} animate-pulse`}></div>
              <span className="text-lg font-semibold text-gray-900">
                {data.velocity.healthy ? 'Healthy' : 'Issues Detected'}
              </span>
            </div>
            {data.velocity.error && (
              <div className="text-xs text-red-600 mt-1">
                ⚠️ {data.velocity.error}
              </div>
            )}
          </div>
        )}
      </div>
    </motion.div>
  );
}
