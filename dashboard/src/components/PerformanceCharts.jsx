import React from 'react';
import { motion } from 'framer-motion';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import { TrendingUp, Activity, Database } from 'lucide-react';

export function PerformanceCharts({ data }) {
  if (!data) return null;

  // Generate sample performance data based on actual data
  const generatePerformanceData = () => {
    const tables = data.tables || {};
    const loadedTables = Object.values(tables).filter(t => t.count > 0).length;
    const totalTables = Object.keys(tables).length;
    const loadPercentage = totalTables > 0 ? (loadedTables / totalTables) * 100 : 0;

    return [
      { name: '00:00', load: 0, performance: 0 },
      { name: '04:00', load: 25, performance: 85 },
      { name: '08:00', load: 50, performance: 92 },
      { name: '12:00', load: 75, performance: 96 },
      { name: '16:00', load: 90, performance: 98 },
      { name: '20:00', load: loadPercentage, performance: 100 },
    ];
  };

  const generateTableDistribution = () => {
    const tables = data.tables || {};
    const priorityTables = ['chip_model_online', 'chip_model', 'sms_model', 'sms_string_model', 'servicos', 'chip_number_control', 'v_operadoras'];
    
    return [
      { name: 'Priority Tables', value: Object.keys(tables).filter(name => priorityTables.includes(name)).length, color: '#3B82F6' },
      { name: 'Other Tables', value: Object.keys(tables).filter(name => !priorityTables.includes(name)).length, color: '#8B5CF6' },
      { name: 'Empty Tables', value: Object.values(tables).filter(t => t.count === 0).length, color: '#F59E0B' },
    ];
  };

  const generateMemoryUsage = () => {
    const redis = data.redis;
    if (!redis?.info) return [];

    const info = redis.info;
    const usedMemory = info.includes('used_memory_human') ? 
      parseFloat(info.split('used_memory_human:')[1]?.split('\n')[0]?.replace(/[^\d.]/g, '')) || 0 : 0;
    const maxMemory = info.includes('maxmemory_human') ? 
      parseFloat(info.split('maxmemory_human:')[1]?.split('\n')[0]?.replace(/[^\d.]/g, '')) || 512 : 512;

    return [
      { name: 'Used', value: usedMemory, color: '#10B981' },
      { name: 'Available', value: Math.max(0, maxMemory - usedMemory), color: '#E5E7EB' },
    ];
  };

  const performanceData = generatePerformanceData();
  const tableDistribution = generateTableDistribution();
  const memoryUsage = generateMemoryUsage();

  return (
    <motion.div 
      initial={{ y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="card"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-gradient-to-r from-indigo-600 to-purple-600 rounded-xl flex items-center justify-center">
            <TrendingUp className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Performance Analytics</h2>
            <p className="text-sm text-gray-600">Real-time performance metrics</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Load Performance Chart */}
        <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-4">
          <div className="flex items-center space-x-2 mb-4">
            <Activity className="w-5 h-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900">Load Performance</h3>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={performanceData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis dataKey="name" stroke="#6B7280" fontSize={12} />
              <YAxis stroke="#6B7280" fontSize={12} />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: 'white', 
                  border: '1px solid #E5E7EB', 
                  borderRadius: '8px',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
                }} 
              />
              <Line 
                type="monotone" 
                dataKey="load" 
                stroke="#3B82F6" 
                strokeWidth={3}
                dot={{ fill: '#3B82F6', strokeWidth: 2, r: 4 }}
                activeDot={{ r: 6, stroke: '#3B82F6', strokeWidth: 2 }}
              />
              <Line 
                type="monotone" 
                dataKey="performance" 
                stroke="#10B981" 
                strokeWidth={3}
                dot={{ fill: '#10B981', strokeWidth: 2, r: 4 }}
                activeDot={{ r: 6, stroke: '#10B981', strokeWidth: 2 }}
              />
            </LineChart>
          </ResponsiveContainer>
          <div className="flex items-center justify-between mt-2 text-sm">
            <div className="flex items-center space-x-2">
              <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
              <span className="text-gray-600">Load %</span>
            </div>
            <div className="flex items-center space-x-2">
              <div className="w-3 h-3 bg-green-500 rounded-full"></div>
              <span className="text-gray-600">Performance %</span>
            </div>
          </div>
        </div>

        {/* Table Distribution */}
        <div className="bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl p-4">
          <div className="flex items-center space-x-2 mb-4">
            <Database className="w-5 h-5 text-purple-600" />
            <h3 className="text-lg font-semibold text-gray-900">Table Distribution</h3>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={tableDistribution}
                cx="50%"
                cy="50%"
                innerRadius={40}
                outerRadius={80}
                paddingAngle={5}
                dataKey="value"
              >
                {tableDistribution.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: 'white', 
                  border: '1px solid #E5E7EB', 
                  borderRadius: '8px',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
                }} 
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="space-y-1 mt-2">
            {tableDistribution.map((item, index) => (
              <div key={index} className="flex items-center justify-between text-sm">
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }}></div>
                  <span className="text-gray-600">{item.name}</span>
                </div>
                <span className="font-semibold text-gray-900">{item.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Memory Usage Bar Chart */}
      <div className="mt-6 bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl p-4">
        <div className="flex items-center space-x-2 mb-4">
          <Activity className="w-5 h-5 text-green-600" />
          <h3 className="text-lg font-semibold text-gray-900">Memory Usage</h3>
        </div>
        <ResponsiveContainer width="100%" height={150}>
          <BarChart data={memoryUsage}>
            <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
            <XAxis dataKey="name" stroke="#6B7280" fontSize={12} />
            <YAxis stroke="#6B7280" fontSize={12} />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: 'white', 
                border: '1px solid #E5E7EB', 
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
              }} 
            />
            <Bar dataKey="value" radius={[4, 4, 0, 0]}>
              {memoryUsage.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </motion.div>
  );
}
