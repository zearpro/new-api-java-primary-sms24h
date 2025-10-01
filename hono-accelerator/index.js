import { Hono } from 'hono'
import { cors } from 'hono/cors'
import { logger } from 'hono/logger'
import { Redis } from 'ioredis'
import amqp from 'amqplib'

const app = new Hono()

// Redis connection
const redis = new Redis({
	host: process.env.REDIS_HOST || 'localhost',
	port: parseInt(process.env.REDIS_PORT || '6379'),
	password: process.env.REDIS_PASSWORD || 'store24h_redis_pass',
	retryDelayOnFailover: 100,
	maxRetriesPerRequest: 3,
})

// RabbitMQ connection
let rabbitConnection = null
let rabbitChannel = null

async function connectRabbitMQ() {
	try {
		const rabbitUrl = `amqp://${process.env.RABBITMQ_USER || 'guest'}:${
			process.env.RABBITMQ_PASSWORD || 'guest'
		}@${process.env.RABBITMQ_HOST || 'localhost'}:${
			process.env.RABBITMQ_PORT || '5672'
		}`
		rabbitConnection = await amqp.connect(rabbitUrl)
		rabbitChannel = await rabbitConnection.createChannel()

		// Declare queues
		await rabbitChannel.assertQueue('number.assigned', { durable: true })
		await rabbitChannel.assertQueue('activation.status.update', {
			durable: true,
		})
		await rabbitChannel.assertQueue('activation.completed', { durable: true })
		await rabbitChannel.assertQueue('number.released', { durable: true })

		console.log('✅ Connected to RabbitMQ')
	} catch (error) {
		console.error('❌ RabbitMQ connection error:', error)
	}
}

// Initialize RabbitMQ connection
connectRabbitMQ()

// Middleware
app.use('*', cors())
app.use('*', logger())

// Health check
app.get('/health', (c) => {
	return c.json({ status: 'ok', timestamp: new Date().toISOString() })
})

// Get Balance - Ultra-fast Redis-only implementation
app.get('/api/v2/getBalance', async (c) => {
	const startTime = Date.now()

	try {
		const apiKey = c.req.query('api_key')
		if (!apiKey) {
			return c.json({ error: 'API key required' }, 400)
		}

		// Get user data from Redis cache
		const userKey = `user:${apiKey}`
		const userData = await redis.hgetall(userKey)

		if (!userData || Object.keys(userData).length === 0) {
			return c.json({ error: 'User not found' }, 404)
		}

		const balance = parseFloat(userData.credito || '0')
		const whatsappEnabled = userData.whatsapp_enabled === 'true'

		const responseTime = Date.now() - startTime

		return c.json({
			balance: balance,
			whatsapp_enabled: whatsappEnabled,
			response_time_ms: responseTime,
			cached: true,
		})
	} catch (error) {
		console.error('Error in getBalance:', error)
		return c.json({ error: 'Internal server error' }, 500)
	}
})

// Get Prices - Redis-cached service data
app.get('/api/v2/getPrices', async (c) => {
	const startTime = Date.now()

	try {
		const apiKey = c.req.query('api_key')
		const country = c.req.query('country')
		const operator = c.req.query('operator')

		if (!apiKey || !country || !operator) {
			return c.json({ error: 'Missing required parameters' }, 400)
		}

		// Validate user
		const userKey = `user:${apiKey}`
		const userExists = await redis.exists(userKey)
		if (!userExists) {
			return c.json({ error: 'Invalid API key' }, 401)
		}

		// Get services from Redis cache
		const servicesKey = `services:${country}:${operator}`
		const services = await redis.smembers(servicesKey)

		if (!services || services.length === 0) {
			return c.json({ services: [] })
		}

		// Get service details
		const servicePromises = services.map(async (serviceId) => {
			const serviceKey = `service:${serviceId}`
			const serviceData = await redis.hgetall(serviceKey)

			if (serviceData && Object.keys(serviceData).length > 0) {
				// Get available count from Redis counter
				const countKey = `pool_count:${serviceId}:${country}:${operator}`
				const availableCount = (await redis.get(countKey)) || '0'

				return {
					service_id: serviceId,
					service_name: serviceData.name || `Service ${serviceId}`,
					price: parseFloat(serviceData.price || '0'),
					currency: serviceData.currency || 'BRL',
					available_count: parseInt(availableCount),
					country: country,
					operator: operator,
				}
			}
			return null
		})

		const serviceResults = await Promise.all(servicePromises)
		const validServices = serviceResults.filter((service) => service !== null)

		const responseTime = Date.now() - startTime

		return c.json({
			services: validServices,
			response_time_ms: responseTime,
			cached: true,
		})
	} catch (error) {
		console.error('Error in getPrices:', error)
		return c.json({ error: 'Internal server error' }, 500)
	}
})

// Get Number - Atomic Redis reservation
app.post('/api/v2/getNumber', async (c) => {
	const startTime = Date.now()

	try {
		const body = await c.req.json()
		const { api_key, service_id, country, operator } = body

		if (!api_key || !service_id || !country || !operator) {
			return c.json({ error: 'Missing required parameters' }, 400)
		}

		// Validate user
		const userKey = `user:${api_key}`
		const userExists = await redis.exists(userKey)
		if (!userExists) {
			return c.json({ error: 'Invalid API key' }, 401)
		}

		// Check if service is available
		const serviceKey = `service:${service_id}`
		const serviceExists = await redis.exists(serviceKey)
		if (!serviceExists) {
			return c.json({ error: 'Service not found' }, 404)
		}

		// Atomic number reservation using Lua script
		const luaScript = `
      local availableKey = 'available_numbers:' .. ARGV[1] .. ':' .. ARGV[2] .. ':' .. ARGV[3]
      local usedKey = 'used_numbers:' .. ARGV[1]
      local countKey = 'pool_count:' .. ARGV[1] .. ':' .. ARGV[2] .. ':' .. ARGV[3]
      
      local available = redis.call('SPOP', availableKey)
      if available then
        redis.call('SADD', usedKey, available)
        redis.call('DECR', countKey)
        return available
      else
        return nil
      end
    `

		const reservedNumber = await redis.eval(
			luaScript,
			0,
			service_id,
			country,
			operator
		)

		if (!reservedNumber) {
			return c.json({ error: 'No numbers available' }, 404)
		}

		// Publish assignment message to RabbitMQ (async)
		const assignmentMessage = {
			serviceId: service_id,
			number: reservedNumber,
			country: country,
			operator: operator,
			userId: await redis.hget(userKey, 'user_id'),
			apiKey: api_key,
			timestamp: new Date().toISOString(),
		}

		// Publish to RabbitMQ for async MySQL writes
		if (rabbitChannel) {
			try {
				await rabbitChannel.sendToQueue(
					'number.assigned',
					Buffer.from(JSON.stringify(assignmentMessage)),
					{ persistent: true }
				)
				console.log('✅ Assignment message published to RabbitMQ')
			} catch (error) {
				console.error('❌ Error publishing to RabbitMQ:', error)
			}
		} else {
			console.warn(
				'⚠️ RabbitMQ channel not available, logging assignment:',
				assignmentMessage
			)
		}

		const responseTime = Date.now() - startTime

		return c.json({
			number: reservedNumber,
			service_id: service_id,
			country: country,
			operator: operator,
			response_time_ms: responseTime,
			cached: true,
		})
	} catch (error) {
		console.error('Error in getNumber:', error)
		return c.json({ error: 'Internal server error' }, 500)
	}
})

// Get Extra Activation - Redis-only check
app.get('/api/v2/getExtraActivation', async (c) => {
	const startTime = Date.now()

	try {
		const apiKey = c.req.query('api_key')
		const serviceId = c.req.query('service_id')
		const number = c.req.query('number')
		const country = c.req.query('country')
		const operator = c.req.query('operator')

		if (!apiKey || !serviceId || !number || !country || !operator) {
			return c.json({ error: 'Missing required parameters' }, 400)
		}

		// Validate user
		const userKey = `user:${api_key}`
		const userExists = await redis.exists(userKey)
		if (!userExists) {
			return c.json({ error: 'Invalid API key' }, 401)
		}

		// Check activation status in Redis
		const activationKey = `activated:${serviceId}:${number}:${country}:${operator}`
		const isActivated = await redis.exists(activationKey)

		const responseTime = Date.now() - startTime

		return c.json({
			activated: isActivated === 1,
			service_id: serviceId,
			number: number,
			country: country,
			operator: operator,
			response_time_ms: responseTime,
			cached: true,
		})
	} catch (error) {
		console.error('Error in getExtraActivation:', error)
		return c.json({ error: 'Internal server error' }, 500)
	}
})

// Async MySQL Writer - Handle activation status updates
app.post('/api/v2/activation/status', async (c) => {
	const startTime = Date.now()

	try {
		const body = await c.req.json()
		const { activationId, status, smsCode } = body

		if (!activationId || !status) {
			return c.json({ error: 'Missing required parameters' }, 400)
		}

		// Publish status update to RabbitMQ
		const statusMessage = {
			activationId: activationId,
			status: status,
			smsCode: smsCode || null,
			timestamp: new Date().toISOString(),
		}

		if (rabbitChannel) {
			try {
				await rabbitChannel.sendToQueue(
					'activation.status.update',
					Buffer.from(JSON.stringify(statusMessage)),
					{ persistent: true }
				)
				console.log('✅ Status update published to RabbitMQ')
			} catch (error) {
				console.error('❌ Error publishing status update:', error)
			}
		}

		const responseTime = Date.now() - startTime

		return c.json({
			success: true,
			activationId: activationId,
			status: status,
			response_time_ms: responseTime,
		})
	} catch (error) {
		console.error('Error in activation status update:', error)
		return c.json({ error: 'Internal server error' }, 500)
	}
})

// Async MySQL Writer - Handle activation completion
app.post('/api/v2/activation/complete', async (c) => {
	const startTime = Date.now()

	try {
		const body = await c.req.json()
		const { activationId, smsCode, finalStatus } = body

		if (!activationId || !finalStatus) {
			return c.json({ error: 'Missing required parameters' }, 400)
		}

		// Publish completion to RabbitMQ
		const completionMessage = {
			activationId: activationId,
			smsCode: smsCode || null,
			finalStatus: finalStatus,
			timestamp: new Date().toISOString(),
		}

		if (rabbitChannel) {
			try {
				await rabbitChannel.sendToQueue(
					'activation.completed',
					Buffer.from(JSON.stringify(completionMessage)),
					{ persistent: true }
				)
				console.log('✅ Activation completion published to RabbitMQ')
			} catch (error) {
				console.error('❌ Error publishing completion:', error)
			}
		}

		const responseTime = Date.now() - startTime

		return c.json({
			success: true,
			activationId: activationId,
			finalStatus: finalStatus,
			response_time_ms: responseTime,
		})
	} catch (error) {
		console.error('Error in activation completion:', error)
		return c.json({ error: 'Internal server error' }, 500)
	}
})

// Async MySQL Writer - Handle number release
app.post('/api/v2/number/release', async (c) => {
	const startTime = Date.now()

	try {
		const body = await c.req.json()
		const { number, reason } = body

		if (!number) {
			return c.json({ error: 'Missing required parameters' }, 400)
		}

		// Publish release to RabbitMQ
		const releaseMessage = {
			number: number,
			reason: reason || 'RELEASED',
			timestamp: new Date().toISOString(),
		}

		if (rabbitChannel) {
			try {
				await rabbitChannel.sendToQueue(
					'number.released',
					Buffer.from(JSON.stringify(releaseMessage)),
					{ persistent: true }
				)
				console.log('✅ Number release published to RabbitMQ')
			} catch (error) {
				console.error('❌ Error publishing release:', error)
			}
		}

		const responseTime = Date.now() - startTime

		return c.json({
			success: true,
			number: number,
			reason: reason || 'RELEASED',
			response_time_ms: responseTime,
		})
	} catch (error) {
		console.error('Error in number release:', error)
		return c.json({ error: 'Internal server error' }, 500)
	}
})

// Redis connection error handling
redis.on('error', (err) => {
	console.error('Redis connection error:', err)
})

redis.on('connect', () => {
	console.log('Connected to Redis')
})

// Start server
const port = parseInt(process.env.PORT || '3000')
console.log(`🚀 Hono.js Accelerator Service starting on port ${port}`)

export default {
	port: port,
	fetch: app.fetch,
}
