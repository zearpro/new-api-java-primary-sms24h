import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Toaster } from 'react-hot-toast'
import {
	Database,
	RefreshCw,
	Clock,
} from 'lucide-react'

import { WarmupStatus } from './components/WarmupStatus'
import { RedisHealth } from './components/RedisHealth'
import { TableProgress } from './components/TableProgress'
import { ControlPanel } from './components/ControlPanel'
import { MetricsOverview } from './components/MetricsOverview'
import { PerformanceCharts } from './components/PerformanceCharts'

function App() {
	const [warmupData, setWarmupData] = useState(null)
	const [loading, setLoading] = useState(true)
	const [lastUpdate, setLastUpdate] = useState(new Date())

	const fetchWarmupStatus = async () => {
		try {
			const response = await fetch('/api/warmup/status')
			const data = await response.json()
			setWarmupData(data)
			setLastUpdate(new Date())
		} catch (error) {
			console.error('Failed to fetch warmup status:', error)
		} finally {
			setLoading(false)
		}
	}

	useEffect(() => {
		fetchWarmupStatus()
		const interval = setInterval(fetchWarmupStatus, 5000) // Update every 5 seconds
		return () => clearInterval(interval)
	}, [])

	if (loading) {
		return (
			<div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50 flex items-center justify-center">
				<motion.div
					initial={{ scale: 0.8, opacity: 0 }}
					animate={{ scale: 1, opacity: 1 }}
					className="text-center"
				>
					<motion.div
						animate={{ rotate: 360 }}
						transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
						className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full mx-auto mb-4"
					/>
					<motion.h2
						initial={{ y: 20, opacity: 0 }}
						animate={{ y: 0, opacity: 1 }}
						transition={{ delay: 0.2 }}
						className="text-2xl font-bold text-gray-800"
					>
						🚀 Loading Store24h Dashboard
					</motion.h2>
					<motion.p
						initial={{ y: 20, opacity: 0 }}
						animate={{ y: 0, opacity: 1 }}
						transition={{ delay: 0.4 }}
						className="text-gray-600 mt-2"
					>
						Connecting to Dragonfly and warming up caches...
					</motion.p>
				</motion.div>
			</div>
		)
	}

	return (
		<div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
			<Toaster
				position="top-right"
				toastOptions={{
					duration: 4000,
					style: {
						background: '#fff',
						color: '#374151',
						boxShadow:
							'0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
						borderRadius: '12px',
						border: '1px solid #e5e7eb',
					},
				}}
			/>

			{/* Header */}
			<motion.header
				initial={{ y: -100, opacity: 0 }}
				animate={{ y: 0, opacity: 1 }}
				className="bg-white/80 backdrop-blur-lg border-b border-gray-200 sticky top-0 z-50"
			>
				<div className="container mx-auto px-6 py-4">
					<div className="flex items-center justify-between">
						<motion.div
							initial={{ x: -20, opacity: 0 }}
							animate={{ x: 0, opacity: 1 }}
							className="flex items-center space-x-3"
						>
							<div className="w-10 h-10 bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl flex items-center justify-center">
								<Database className="w-6 h-6 text-white" />
							</div>
							<div>
								<h1 className="text-2xl font-bold text-gray-900">
									Store24h Warmup Dashboard
								</h1>
								<p className="text-sm text-gray-600">
									Dragonfly Performance Monitor
								</p>
							</div>
						</motion.div>

						<motion.div
							initial={{ x: 20, opacity: 0 }}
							animate={{ x: 0, opacity: 1 }}
							className="flex items-center space-x-4"
						>
							<div className="flex items-center space-x-2 text-sm text-gray-600">
								<Clock className="w-4 h-4" />
								<span>Last update: {lastUpdate.toLocaleTimeString()}</span>
							</div>
							<motion.button
								whileHover={{ scale: 1.05 }}
								whileTap={{ scale: 0.95 }}
								onClick={fetchWarmupStatus}
								className="p-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
							>
								<RefreshCw className="w-4 h-4" />
							</motion.button>
						</motion.div>
					</div>
				</div>
			</motion.header>

			{/* Main Content */}
			<main className="container mx-auto px-6 py-8">
				<AnimatePresence>
					{/* Metrics Overview */}
					<motion.div
						initial={{ y: 20, opacity: 0 }}
						animate={{ y: 0, opacity: 1 }}
						transition={{ delay: 0.1 }}
						className="mb-8"
					>
						<MetricsOverview data={warmupData} />
					</motion.div>

					{/* Charts Row */}
					<motion.div
						initial={{ y: 20, opacity: 0 }}
						animate={{ y: 0, opacity: 1 }}
						transition={{ delay: 0.2 }}
						className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8"
					>
						<WarmupStatus data={warmupData} />
						<RedisHealth data={warmupData?.redis} />
					</motion.div>

					{/* Performance Charts */}
					<motion.div
						initial={{ y: 20, opacity: 0 }}
						animate={{ y: 0, opacity: 1 }}
						transition={{ delay: 0.3 }}
						className="mb-8"
					>
						<PerformanceCharts data={warmupData} />
					</motion.div>

					{/* Table Progress */}
					<motion.div
						initial={{ y: 20, opacity: 0 }}
						animate={{ y: 0, opacity: 1 }}
						transition={{ delay: 0.4 }}
						className="mb-8"
					>
						<TableProgress tables={warmupData?.tables} />
					</motion.div>

					{/* Control Panel */}
					<motion.div
						initial={{ y: 20, opacity: 0 }}
						animate={{ y: 0, opacity: 1 }}
						transition={{ delay: 0.5 }}
					>
						<ControlPanel onRefresh={fetchWarmupStatus} />
					</motion.div>
				</AnimatePresence>
			</main>

			{/* Footer */}
			<motion.footer
				initial={{ y: 20, opacity: 0 }}
				animate={{ y: 0, opacity: 1 }}
				transition={{ delay: 0.6 }}
				className="bg-white/80 backdrop-blur-lg border-t border-gray-200 mt-16"
			>
				<div className="container mx-auto px-6 py-6">
					<div className="flex items-center justify-between text-sm text-gray-600">
						<div className="flex items-center space-x-4">
							<span>Powered by DragonflyDB</span>
							<span>•</span>
							<span>Optimized for t3.2xlarge</span>
						</div>
						<div className="flex items-center space-x-2">
							<div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
							<span>System Healthy</span>
						</div>
					</div>
				</div>
			</motion.footer>
		</div>
	)
}

export default App
