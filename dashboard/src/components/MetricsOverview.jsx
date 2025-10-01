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
  HardDrive
} from 'lucide-react';

export function MetricsOverview({ data }) {
  if (!data) return null;

  const metrics = [
    {
      title: 'System Status',
      value: data.status === 'healthy' ? 'Healthy' : 'Warning',
      icon: data.status === 'healthy' ? CheckCircle : AlertTriangle,
      color: data.status === 'healthy' ? 'green' : 'yellow',
      change: '+2.5%',
      description: 'Overall system health'
    },
    {
      title: 'Redis Connection',
      value: data.redis?.connected ? 'Connected' : 'Disconnected',
      icon: data.redis?.connected ? CheckCircle : AlertTriangle,
      color: data.redis?.connected ? 'green' : 'red',
      change: data.redis?.connected ? '+0.1%' : '-5.2%',
      description: 'Dragonfly connectivity'
    },
    {
      title: 'Tables Loaded',
      value: Object.values(data.tables || {}).filter(t => t.count > 0).length,
      icon: Database,
      color: 'blue',
      change: '+12',
      description: 'Active cached tables'
    },
    {
      title: 'Total Records',
      value: Object.values(data.tables || {}).reduce((sum, t) => sum + (t.count || 0), 0).toLocaleString(),
      icon: HardDrive,
      color: 'purple',
      change: '+1.2K',
      description: 'Cached records'
    }
  ];

  const getColorClasses = (color) => {
    const colors = {
      green: 'from-green-500 to-emerald-500',
      yellow: 'from-yellow-500 to-orange-500',
      red: 'from-red-500 to-pink-500',
      blue: 'from-blue-500 to-cyan-500',
      purple: 'from-purple-500 to-violet-500'
    };
    return colors[color] || colors.blue;
  };

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      {metrics.map((metric, index) => {
        const IconComponent = metric.icon;
        return (
          <motion.div
            key={metric.title}
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: index * 0.1 }}
            whileHover={{ y: -5, scale: 1.02 }}
            className="metric-card group"
          >
            <div className="flex items-center justify-between mb-4">
              <motion.div 
                className={`w-12 h-12 bg-gradient-to-r ${getColorClasses(metric.color)} rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform duration-200`}
                whileHover={{ rotate: 5 }}
              >
                <IconComponent className="w-6 h-6 text-white" />
              </motion.div>
              <motion.div 
                className="text-right"
                initial={{ x: 20, opacity: 0 }}
                animate={{ x: 0, opacity: 1 }}
                transition={{ delay: index * 0.1 + 0.2 }}
              >
                <div className="flex items-center space-x-1 text-sm">
                  <TrendingUp className="w-3 h-3 text-green-500" />
                  <span className="text-green-600 font-medium">{metric.change}</span>
                </div>
              </motion.div>
            </div>
            
            <div className="space-y-2">
              <h3 className="text-sm font-medium text-gray-600">{metric.title}</h3>
              <motion.div 
                className="text-2xl font-bold text-gray-900"
                initial={{ scale: 0.8 }}
                animate={{ scale: 1 }}
                transition={{ delay: index * 0.1 + 0.3 }}
              >
                {metric.value}
              </motion.div>
              <p className="text-xs text-gray-500">{metric.description}</p>
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}
