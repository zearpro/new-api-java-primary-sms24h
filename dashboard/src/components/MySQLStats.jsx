import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Database, 
  BarChart3, 
  TrendingUp,
  Server,
  HardDrive,
  Activity
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

export function MySQLStats({ data }) {
  const [mysqlData, setMysqlData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchMysqlStats = async () => {
      try {
        const response = await fetch('/api/warmup/mysql-stats');
        const stats = await response.json();
        setMysqlData(stats);
      } catch (error) {
        console.error('Failed to fetch MySQL stats:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchMysqlStats();
    const interval = setInterval(fetchMysqlStats, 10000); // Update every 10 seconds
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <motion.div 
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="card"
      >
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      </motion.div>
    );
  }

  if (!mysqlData || !mysqlData.tables) {
    return (
      <motion.div 
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="card"
      >
        <div className="text-center text-gray-500">
          <Database className="w-12 h-12 mx-auto mb-4 text-gray-400" />
          <p>No MySQL data available</p>
        </div>
      </motion.div>
    );
  }

  const tables = Object.entries(mysqlData.tables).map(([name, info]) => ({
    name: name.replace(/_/g, ' '),
    count: info.row_count,
    lastUpdated: info.last_updated
  }));

  const totalRows = mysqlData.total_rows || 0;
  const tableCount = mysqlData.table_count || 0;

  // Prepare data for charts
  const chartData = tables.map(table => ({
    name: table.name,
    count: table.count,
    percentage: totalRows > 0 ? ((table.count / totalRows) * 100).toFixed(1) : 0
  }));

  const pieData = tables.map(table => ({
    name: table.name,
    value: table.count,
    color: getTableColor(table.name)
  }));

  function getTableColor(tableName) {
    const colors = {
      'chip model': '#3B82F6',
      'servicos': '#10B981', 
      'operadoras': '#F59E0B',
      'chip number control': '#EF4444',
      'usuario': '#8B5CF6',
      'activation': '#06B6D4'
    };
    return colors[tableName] || '#6B7280';
  }

  return (
    <motion.div 
      initial={{ y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="card"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-gradient-to-r from-green-600 to-emerald-600 rounded-xl flex items-center justify-center">
            <Database className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">MySQL Database Statistics</h2>
            <p className="text-sm text-gray-600">Real-time table row counts</p>
          </div>
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-gray-900">{totalRows.toLocaleString()}</div>
          <div className="text-sm text-gray-600">total rows</div>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-gradient-to-r from-blue-50 to-blue-100 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-blue-800">Tables</span>
            <Server className="w-4 h-4 text-blue-600" />
          </div>
          <div className="text-2xl font-bold text-blue-900">{tableCount}</div>
          <div className="text-xs text-blue-700">active tables</div>
        </div>

        <div className="bg-gradient-to-r from-green-50 to-green-100 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-green-800">Total Rows</span>
            <HardDrive className="w-4 h-4 text-green-600" />
          </div>
          <div className="text-2xl font-bold text-green-900">{totalRows.toLocaleString()}</div>
          <div className="text-xs text-green-700">database records</div>
        </div>

        <div className="bg-gradient-to-r from-purple-50 to-purple-100 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-purple-800">Avg per Table</span>
            <BarChart3 className="w-4 h-4 text-purple-600" />
          </div>
          <div className="text-2xl font-bold text-purple-900">
            {tableCount > 0 ? Math.round(totalRows / tableCount).toLocaleString() : 0}
          </div>
          <div className="text-xs text-purple-700">rows per table</div>
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Bar Chart */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="flex items-center space-x-2 mb-4">
            <BarChart3 className="w-5 h-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900">Table Row Distribution</h3>
          </div>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis 
                dataKey="name" 
                tick={{ fontSize: 12 }}
                angle={-45}
                textAnchor="end"
                height={80}
              />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip 
                formatter={(value) => [value.toLocaleString(), 'Rows']}
                labelStyle={{ color: '#374151' }}
              />
              <Bar dataKey="count" fill="#3B82F6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Pie Chart */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="flex items-center space-x-2 mb-4">
            <Activity className="w-5 h-5 text-green-600" />
            <h3 className="text-lg font-semibold text-gray-900">Data Distribution</h3>
          </div>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percentage }) => `${name}: ${percentage}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {pieData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip formatter={(value) => [value.toLocaleString(), 'Rows']} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Table Details */}
      <div className="bg-gray-50 rounded-lg p-4">
        <div className="flex items-center space-x-2 mb-4">
          <TrendingUp className="w-5 h-5 text-gray-600" />
          <h3 className="text-lg font-semibold text-gray-900">Table Details</h3>
        </div>
        
        <div className="space-y-3">
          {tables.map((table, index) => (
            <motion.div 
              key={table.name}
              initial={{ x: -20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: index * 0.1 }}
              className="flex items-center justify-between p-3 bg-white rounded-lg border border-gray-200 hover:shadow-sm transition-shadow"
            >
              <div className="flex items-center space-x-3">
                <div 
                  className="w-3 h-3 rounded-full"
                  style={{ backgroundColor: getTableColor(table.name) }}
                />
                <span className="font-medium text-gray-900 capitalize">
                  {table.name}
                </span>
              </div>
              <div className="text-right">
                <div className="text-lg font-bold text-gray-900">
                  {table.count.toLocaleString()}
                </div>
                <div className="text-xs text-gray-500">
                  {totalRows > 0 ? ((table.count / totalRows) * 100).toFixed(1) : 0}%
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Last Updated */}
      <div className="mt-4 text-center text-sm text-gray-500">
        Last updated: {mysqlData.timestamp ? new Date(mysqlData.timestamp).toLocaleString() : 'Never'}
      </div>
    </motion.div>
  );
}
