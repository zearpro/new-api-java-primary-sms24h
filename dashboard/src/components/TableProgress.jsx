import React from 'react';
import { motion } from 'framer-motion';
import { 
  Database, 
  AlertTriangle, 
  CheckCircle, 
  Clock,
  Star
} from 'lucide-react';

const PRIORITY_TABLES = [
  'chip_model_online',
  'chip_model', 
  'sms_model',
  'sms_string_model',
  'servicos',
  'chip_number_control',
  'v_operadoras'
];

export function TableProgress({ tables }) {
  if (!tables) return null;

  const getTableStatus = (tableName) => {
    const table = tables[tableName];
    if (!table) return { status: 'not_found', percentage: 0 };
    
    const hasError = table.error;
    const count = table.count || 0;
    const percentage = count > 0 ? 100 : 0;
    
    return {
      status: hasError ? 'error' : (count > 0 ? 'loaded' : 'empty'),
      percentage,
      count,
      error: hasError,
      lastUpdated: table.last_updated
    };
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'loaded': return 'from-green-500 to-emerald-500';
      case 'error': return 'from-red-500 to-pink-500';
      case 'empty': return 'from-yellow-500 to-orange-500';
      default: return 'from-gray-400 to-gray-500';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'loaded': return CheckCircle;
      case 'error': return AlertTriangle;
      case 'empty': return Clock;
      default: return Database;
    }
  };

  const priorityTables = PRIORITY_TABLES.map(tableName => ({
    name: tableName,
    ...getTableStatus(tableName)
  }));

  const otherTables = Object.keys(tables)
    .filter(name => !PRIORITY_TABLES.includes(name))
    .map(tableName => ({
      name: tableName,
      ...getTableStatus(tableName)
    }));

  const totalLoaded = Object.values(tables).filter(t => t.count > 0).length;
  const totalTables = Object.keys(tables).length;
  const overallProgress = (totalLoaded / totalTables) * 100;

  return (
    <motion.div 
      initial={{ y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="card"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-gradient-to-r from-purple-600 to-pink-600 rounded-xl flex items-center justify-center">
            <Database className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Table Loading Progress</h2>
            <p className="text-sm text-gray-600">Real-time cache status</p>
          </div>
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-gray-900">{totalLoaded}/{totalTables}</div>
          <div className="text-sm text-gray-600">tables loaded</div>
        </div>
      </div>

      {/* Overall Progress */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-gray-600">Overall Progress</span>
          <span className="text-sm font-semibold text-gray-900">{overallProgress.toFixed(1)}%</span>
        </div>
        <div className="progress-bar">
          <motion.div 
            className={`progress-fill bg-gradient-to-r ${getStatusColor(overallProgress > 50 ? 'loaded' : 'empty')}`}
            initial={{ width: 0 }}
            animate={{ width: `${overallProgress}%` }}
            transition={{ duration: 1, ease: "easeOut" }}
          />
        </div>
      </div>

      {/* Priority Tables */}
      <div className="mb-8">
        <div className="flex items-center space-x-2 mb-4">
          <Star className="w-5 h-5 text-yellow-500" />
          <h3 className="text-lg font-semibold text-gray-900">Priority Tables</h3>
          <span className="text-sm text-gray-600">(Critical for performance)</span>
        </div>
        
        <div className="space-y-4">
          {priorityTables.map((table, index) => {
            const StatusIcon = getStatusIcon(table.status);
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
                      table.status === 'loaded' ? 'text-green-600' :
                      table.status === 'error' ? 'text-red-600' : 'text-yellow-600'
                    }`} />
                    <span className="font-semibold text-gray-900 capitalize">
                      {table.name.replace(/_/g, ' ')}
                    </span>
                    <Star className="w-4 h-4 text-yellow-500" />
                  </div>
                  <div className="text-right">
                    <div className="text-lg font-bold text-gray-900">
                      {table.count.toLocaleString()}
                    </div>
                    <div className="text-xs text-gray-600">records</div>
                  </div>
                </div>
                
                <div className="progress-bar mb-2">
                  <motion.div 
                    className={`progress-fill bg-gradient-to-r ${getStatusColor(table.status)}`}
                    initial={{ width: 0 }}
                    animate={{ width: `${table.percentage}%` }}
                    transition={{ duration: 1, delay: index * 0.1 }}
                  />
                </div>
                
                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center space-x-2">
                    <span className="text-gray-600">
                      {table.status === 'loaded' ? '✅ Loaded' :
                       table.status === 'error' ? '❌ Error' : '⏳ Empty'}
                    </span>
                    {table.lastUpdated && (
                      <span className="text-gray-500">
                        • {new Date(table.lastUpdated).toLocaleTimeString()}
                      </span>
                    )}
                  </div>
                  <span className="text-gray-600">{table.percentage}%</span>
                </div>
                
                {table.error && (
                  <motion.div 
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="mt-2 p-2 bg-red-50 border border-red-200 rounded-lg"
                  >
                    <div className="text-xs text-red-700">
                      ⚠️ {table.error}
                    </div>
                  </motion.div>
                )}
              </motion.div>
            );
          })}
        </div>
      </div>

      {/* Other Tables */}
      {otherTables.length > 0 && (
        <div>
          <div className="flex items-center space-x-2 mb-4">
            <Database className="w-5 h-5 text-gray-500" />
            <h3 className="text-lg font-semibold text-gray-900">Other Tables</h3>
            <span className="text-sm text-gray-600">({otherTables.length} tables)</span>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {otherTables.map((table, index) => {
              const StatusIcon = getStatusIcon(table.status);
              return (
                <motion.div 
                  key={table.name}
                  initial={{ y: 20, opacity: 0 }}
                  animate={{ y: 0, opacity: 1 }}
                  transition={{ delay: index * 0.05 }}
                  className="p-3 rounded-lg border border-gray-200 bg-gray-50 hover:bg-gray-100 transition-colors"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <StatusIcon className={`w-4 h-4 ${
                        table.status === 'loaded' ? 'text-green-600' :
                        table.status === 'error' ? 'text-red-600' : 'text-yellow-600'
                      }`} />
                      <span className="text-sm font-medium text-gray-900 capitalize">
                        {table.name.replace(/_/g, ' ')}
                      </span>
                    </div>
                    <span className="text-sm font-semibold text-gray-700">
                      {table.count.toLocaleString()}
                    </span>
                  </div>
                  
                  <div className="progress-bar">
                    <motion.div 
                      className={`progress-fill bg-gradient-to-r ${getStatusColor(table.status)}`}
                      initial={{ width: 0 }}
                      animate={{ width: `${table.percentage}%` }}
                      transition={{ duration: 0.8, delay: index * 0.05 }}
                    />
                  </div>
                </motion.div>
              );
            })}
          </div>
        </div>
      )}
    </motion.div>
  );
}
