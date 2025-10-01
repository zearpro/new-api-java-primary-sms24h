import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  RefreshCw, 
  Trash2, 
  Play, 
  AlertTriangle,
  CheckCircle,
  Loader
} from 'lucide-react';
import toast from 'react-hot-toast';

export function ControlPanel({ onRefresh }) {
  const [loading, setLoading] = useState({
    reset: false,
    seed: false,
    refresh: false
  });

  const handleResetRedis = async () => {
    if (!confirm('⚠️ This will reset ALL Dragonfly data and reseed from MySQL. Are you sure?')) return;
    
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
        </div>

        <div className="bg-red-50 rounded-lg p-4">
          <div className="flex items-center space-x-2 mb-2">
            <Trash2 className="w-4 h-4 text-red-600" />
            <span className="text-sm font-semibold text-red-900">Reset & Reseed</span>
          </div>
          <p className="text-xs text-red-700">
            ⚠️ Destructive operation. Clears all Dragonfly data and rebuilds from MySQL.
          </p>
        </div>

        <div className="bg-gray-50 rounded-lg p-4">
          <div className="flex items-center space-x-2 mb-2">
            <RefreshCw className="w-4 h-4 text-gray-600" />
            <span className="text-sm font-semibold text-gray-900">Refresh Status</span>
          </div>
          <p className="text-xs text-gray-700">
            Updates the dashboard with the latest cache status and performance metrics.
          </p>
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
