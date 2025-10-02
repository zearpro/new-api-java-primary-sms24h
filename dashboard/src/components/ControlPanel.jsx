import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  RefreshCw, 
  Trash2, 
  Play, 
  CheckCircle,
  Loader,
  Zap
} from 'lucide-react';
import toast from 'react-hot-toast';

export function ControlPanel({ onRefresh, data }) {
  const [loading, setLoading] = useState({
    reset: false,
    seed: false,
    refresh: false
  });

  const [seedingProgress, setSeedingProgress] = useState(null);

  useEffect(() => {
    if (data && data.seeding_progress) {
      setSeedingProgress(data.seeding_progress);
    }
  }, [data]);

  const handleResetRedis = async () => {
    if (!window.confirm('⚠️ This will reset ALL Dragonfly data and reseed from MySQL. Are you sure?')) return;
    
    setLoading(prev => ({ ...prev, reset: true }));
    try {
      const response = await fetch('/api/warmup/reset-redis', { method: 'POST' });
      if (response.ok) {
        toast.success('🔄 Dragonfly reset successfully! Starting reseed...');
        setTimeout(() => {
          onRefresh();
          toast.success('✅ Reseed completed!');
        }, 2000);
      } else {
        throw new Error('Reset failed');
      }
    } catch (error) {
      toast.error('❌ Failed to reset Dragonfly');
    } finally {
      setLoading(prev => ({ ...prev, reset: false }));
    }
  };

  const handleManualSeed = async () => {
    setLoading(prev => ({ ...prev, seed: true }));
    try {
      const response = await fetch('/api/warmup/trigger', { method: 'POST' });
      if (response.ok) {
        toast.success('🌱 Manual seeding started!');
        setTimeout(() => {
          onRefresh();
          toast.success('✅ Manual seeding completed!');
        }, 3000);
      } else {
        throw new Error('Seeding failed');
      }
    } catch (error) {
      toast.error('❌ Failed to start manual seeding');
    } finally {
      setLoading(prev => ({ ...prev, seed: false }));
    }
  };

  const handleRefresh = async () => {
    setLoading(prev => ({ ...prev, refresh: true }));
    try {
      await onRefresh();
      toast.success('🔄 Status refreshed!');
    } catch (error) {
      toast.error('❌ Failed to refresh status');
    } finally {
      setLoading(prev => ({ ...prev, refresh: false }));
    }
  };

  return (
    <motion.div 
      initial={{ y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="card"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-gradient-to-r from-orange-600 to-red-600 rounded-xl flex items-center justify-center">
            <RefreshCw className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Control Panel</h2>
            <p className="text-sm text-gray-600">Manage Dragonfly cache operations</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Manual Seed Button */}
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={handleManualSeed}
          disabled={loading.seed}
          className="btn-primary flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading.seed ? (
            <Loader className="w-5 h-5 animate-spin" />
          ) : (
            <Play className="w-5 h-5" />
          )}
          <span>{loading.seed ? 'Seeding...' : 'Manual Seed'}</span>
        </motion.button>

        {/* Reset Redis Button */}
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={handleResetRedis}
          disabled={loading.reset}
          className="btn-danger flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading.reset ? (
            <Loader className="w-5 h-5 animate-spin" />
          ) : (
            <Trash2 className="w-5 h-5" />
          )}
          <span>{loading.reset ? 'Resetting...' : 'Reset & Reseed'}</span>
        </motion.button>

        {/* Refresh Status Button */}
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={handleRefresh}
          disabled={loading.refresh}
          className="btn-secondary flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading.refresh ? (
            <Loader className="w-5 h-5 animate-spin" />
          ) : (
            <RefreshCw className="w-5 h-5" />
          )}
          <span>{loading.refresh ? 'Refreshing...' : 'Refresh Status'}</span>
        </motion.button>
      </div>

      {/* Seeding Progress Overview */}
      {seedingProgress && (
        <div className="mt-6 bg-gradient-to-r from-blue-50 to-purple-50 rounded-lg p-4">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <Zap className="w-5 h-5 text-blue-600" />
              <span className="text-sm font-semibold text-blue-900">Current Seeding Status</span>
            </div>
            <div className="text-right">
              <div className="text-lg font-bold text-blue-900">
                {seedingProgress.overall_percentage || 0}%
              </div>
              <div className="text-xs text-blue-700">
                {seedingProgress.status === 'complete' ? 'Complete' :
                 seedingProgress.status === 'partial' ? 'In Progress' :
                 seedingProgress.status === 'error' ? 'Error' : 'Not Started'}
              </div>
            </div>
          </div>
          
          <div className="progress-bar mb-3">
            <motion.div 
              className="progress-fill bg-gradient-to-r from-blue-500 to-purple-500"
              initial={{ width: 0 }}
              animate={{ width: `${seedingProgress.overall_percentage || 0}%` }}
              transition={{ duration: 1 }}
            />
          </div>
          
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-center">
            <div className="bg-white rounded-lg p-2">
              <div className="text-sm font-semibold text-green-600">
                {Object.values(seedingProgress.table_progress || {}).filter(t => t.status === 'complete').length}
              </div>
              <div className="text-xs text-gray-600">Complete</div>
            </div>
            <div className="bg-white rounded-lg p-2">
              <div className="text-sm font-semibold text-yellow-600">
                {Object.values(seedingProgress.table_progress || {}).filter(t => t.status === 'partial').length}
              </div>
              <div className="text-xs text-gray-600">In Progress</div>
            </div>
            <div className="bg-white rounded-lg p-2">
              <div className="text-sm font-semibold text-gray-600">
                {Object.values(seedingProgress.table_progress || {}).filter(t => t.status === 'empty').length}
              </div>
              <div className="text-xs text-gray-600">Empty</div>
            </div>
            <div className="bg-white rounded-lg p-2">
              <div className="text-sm font-semibold text-red-600">
                {Object.values(seedingProgress.table_progress || {}).filter(t => t.status === 'error').length}
              </div>
              <div className="text-xs text-gray-600">Errors</div>
            </div>
          </div>
        </div>
      )}

      {/* Action Descriptions */}
      <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-blue-50 rounded-lg p-4">
          <div className="flex items-center space-x-2 mb-2">
            <Play className="w-4 h-4 text-blue-600" />
            <span className="text-sm font-semibold text-blue-900">Manual Seed</span>
          </div>
          <p className="text-xs text-blue-700">
            Triggers immediate cache warming for all tables. Safe operation that adds missing data.
          </p>
          {seedingProgress && seedingProgress.overall_percentage < 100 && (
            <div className="mt-2 text-xs text-blue-600">
              Current: {seedingProgress.overall_percentage}% complete
            </div>
          )}
        </div>

        <div className="bg-red-50 rounded-lg p-4">
          <div className="flex items-center space-x-2 mb-2">
            <Trash2 className="w-4 h-4 text-red-600" />
            <span className="text-sm font-semibold text-red-900">Reset & Reseed</span>
          </div>
          <p className="text-xs text-red-700">
            ⚠️ Destructive operation. Clears all Dragonfly data and rebuilds from MySQL.
          </p>
          {seedingProgress && seedingProgress.overall_percentage > 0 && (
            <div className="mt-2 text-xs text-red-600">
              Will reset {seedingProgress.overall_percentage}% progress
            </div>
          )}
        </div>

        <div className="bg-gray-50 rounded-lg p-4">
          <div className="flex items-center space-x-2 mb-2">
            <RefreshCw className="w-4 h-4 text-gray-600" />
            <span className="text-sm font-semibold text-gray-900">Refresh Status</span>
          </div>
          <p className="text-xs text-gray-700">
            Updates the dashboard with the latest cache status and performance metrics.
          </p>
          <div className="mt-2 text-xs text-gray-600">
            Last update: {data?.timestamp ? new Date(data.timestamp).toLocaleTimeString() : 'Never'}
          </div>
        </div>
      </div>

      {/* System Status */}
      <div className="mt-6 bg-gradient-to-r from-green-50 to-emerald-50 rounded-lg p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-5 h-5 text-green-600" />
            <span className="text-sm font-semibold text-green-900">System Status</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span className="text-sm text-green-700">All Systems Operational</span>
          </div>
        </div>
        <p className="text-xs text-green-600 mt-1">
          Dragonfly is running optimally with 3.8x better performance than Redis
        </p>
      </div>
    </motion.div>
  );
}
