import React from 'react';
import { motion } from 'framer-motion';
import { 
  Database, 
  Zap, 
  CheckCircle, 
  AlertTriangle, 
  Clock,
  TrendingUp,
  Activity,
  RefreshCw
} from 'lucide-react';

export function RedisSeedingProgress({ data }) {
  if (!data || !data.seeding_progress) return null;

  const progress = data.seeding_progress;
  const overallPercentage = progress.overall_percentage || 0;
  const tableProgress = progress.table_progress || {};
  const status = progress.status || 'empty';

  const getStatusColor = (status) => {
    switch (status) {
      case 'complete': return 'from-green-500 to-emerald-500';
      case 'partial': return 'from-yellow-500 to-orange-500';
      case 'empty': return 'from-gray-400 to-gray-500';
      case 'error': return 'from-red-500 to-pink-500';
      default: return 'from-gray-400 to-gray-500';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'complete': return CheckCircle;
      case 'partial': return Clock;
      case 'empty': return Database;
      case 'error': return AlertTriangle;
      default: return Database;
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'complete': return 'Complete';
      case 'partial': return 'In Progress';
      case 'empty': return 'Not Started';
      case 'error': return 'Error';
      default: return 'Unknown';
    }
  };

  const tables = Object.entries(tableProgress).map(([name, info]) => ({
    name: name.replace(/_/g, ' '),
    ...info
  }));

  const completedTables = tables.filter(t => t.status === 'complete').length;
  const partialTables = tables.filter(t => t.status === 'partial').length;
  const emptyTables = tables.filter(t => t.status === 'empty').length;
  const errorTables = tables.filter(t => t.status === 'error').length;

  return (
    <motion.div 
      initial={{ y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="card"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-gradient-to-r from-red-600 to-pink-600 rounded-xl flex items-center justify-center">
            <Zap className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Redis Seeding Progress</h2>
            <p className="text-sm text-gray-600">Cache population status</p>
          </div>
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-gray-900">{overallPercentage}%</div>
          <div className="text-sm text-gray-600">overall progress</div>
        </div>
      </div>

      {/* Overall Progress */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-gray-600">Overall Seeding Progress</span>
          <span className="text-sm font-semibold text-gray-900">{overallPercentage}%</span>
        </div>
        <div className="progress-bar">
          <motion.div 
            className={`progress-fill bg-gradient-to-r ${getStatusColor(status)}`}
            initial={{ width: 0 }}
            animate={{ width: `${overallPercentage}%` }}
            transition={{ duration: 1, ease: "easeOut" }}
          />
        </div>
        <div className="flex items-center justify-between mt-2 text-sm">
          <span className={`px-2 py-1 rounded-full text-xs font-medium ${
            status === 'complete' ? 'bg-green-100 text-green-800' :
            status === 'partial' ? 'bg-yellow-100 text-yellow-800' :
            status === 'error' ? 'bg-red-100 text-red-800' :
            'bg-gray-100 text-gray-800'
          }`}>
            {getStatusText(status)}
          </span>
          <span className="text-gray-500">
            {completedTables}/{tables.length} tables complete
          </span>
        </div>
      </div>

      {/* Progress Summary */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-gradient-to-r from-green-50 to-green-100 rounded-lg p-3">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-medium text-green-800">Complete</span>
            <CheckCircle className="w-4 h-4 text-green-600" />
          </div>
          <div className="text-lg font-bold text-green-900">{completedTables}</div>
          <div className="text-xs text-green-700">tables</div>
        </div>

        <div className="bg-gradient-to-r from-yellow-50 to-yellow-100 rounded-lg p-3">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-medium text-yellow-800">In Progress</span>
            <Clock className="w-4 h-4 text-yellow-600" />
          </div>
          <div className="text-lg font-bold text-yellow-900">{partialTables}</div>
          <div className="text-xs text-yellow-700">tables</div>
        </div>

        <div className="bg-gradient-to-r from-gray-50 to-gray-100 rounded-lg p-3">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-medium text-gray-800">Empty</span>
            <Database className="w-4 h-4 text-gray-600" />
          </div>
          <div className="text-lg font-bold text-gray-900">{emptyTables}</div>
          <div className="text-xs text-gray-700">tables</div>
        </div>

        <div className="bg-gradient-to-r from-red-50 to-red-100 rounded-lg p-3">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-medium text-red-800">Errors</span>
            <AlertTriangle className="w-4 h-4 text-red-600" />
          </div>
          <div className="text-lg font-bold text-red-900">{errorTables}</div>
          <div className="text-xs text-red-700">tables</div>
        </div>
      </div>

      {/* Table Progress Details */}
      <div className="space-y-4">
        <div className="flex items-center space-x-2 mb-4">
          <Activity className="w-5 h-5 text-gray-600" />
          <h3 className="text-lg font-semibold text-gray-900">Table Progress Details</h3>
        </div>
        
        {tables.map((table, index) => {
          const StatusIcon = getStatusIcon(table.status);
          const percentage = table.percentage || 0;
          
          return (
            <motion.div 
              key={table.name}
              initial={{ x: -20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: index * 0.1 }}
              className={`p-4 rounded-xl border-l-4 bg-gradient-to-r from-blue-50 to-purple-50 border-blue-500 hover:shadow-lg transition-all duration-200`}
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center space-x-3">
                  <StatusIcon className={`w-5 h-5 ${
                    table.status === 'complete' ? 'text-green-600' :
                    table.status === 'partial' ? 'text-yellow-600' :
                    table.status === 'error' ? 'text-red-600' : 'text-gray-600'
                  }`} />
                  <span className="font-semibold text-gray-900 capitalize">
                    {table.name}
                  </span>
                </div>
                <div className="text-right">
                  <div className="text-lg font-bold text-gray-900">
                    {percentage}%
                  </div>
                  <div className="text-xs text-gray-600">
                    {table.redis_count?.toLocaleString() || 0} / {table.mysql_count?.toLocaleString() || 0}
                  </div>
                </div>
              </div>
              
              <div className="progress-bar mb-2">
                <motion.div 
                  className={`progress-fill bg-gradient-to-r ${getStatusColor(table.status)}`}
                  initial={{ width: 0 }}
                  animate={{ width: `${percentage}%` }}
                  transition={{ duration: 1, delay: index * 0.1 }}
                />
              </div>
              
              <div className="flex items-center justify-between text-sm">
                <div className="flex items-center space-x-2">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    table.status === 'complete' ? 'bg-green-100 text-green-800' :
                    table.status === 'partial' ? 'bg-yellow-100 text-yellow-800' :
                    table.status === 'error' ? 'bg-red-100 text-red-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {getStatusText(table.status)}
                  </span>
                  {table.status === 'partial' && (
                    <span className="text-gray-500">
                      • {table.redis_count?.toLocaleString() || 0} cached
                    </span>
                  )}
                </div>
                <div className="flex items-center space-x-2 text-gray-500">
                  <TrendingUp className="w-3 h-3" />
                  <span>{table.mysql_count?.toLocaleString() || 0} total</span>
                </div>
              </div>
            </motion.div>
          );
        })}
      </div>

      {/* Performance Metrics */}
      {overallPercentage > 0 && (
        <div className="mt-6 bg-gradient-to-r from-blue-50 to-purple-50 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <RefreshCw className="w-5 h-5 text-blue-600" />
              <span className="text-sm font-semibold text-blue-900">Seeding Performance</span>
            </div>
            <div className="flex items-center space-x-2">
              <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
              <span className="text-sm text-blue-700">
                {overallPercentage >= 100 ? 'Complete' : 'In Progress'}
              </span>
            </div>
          </div>
          <p className="text-xs text-blue-600 mt-1">
            {overallPercentage >= 100 
              ? 'All tables have been successfully seeded to Redis cache'
              : `${completedTables} of ${tables.length} tables completed. Seeding continues...`
            }
          </p>
        </div>
      )}

      {/* Last Updated */}
      <div className="mt-4 text-center text-sm text-gray-500">
        Last updated: {progress.timestamp ? new Date(progress.timestamp).toLocaleString() : 'Never'}
      </div>
    </motion.div>
  );
}
