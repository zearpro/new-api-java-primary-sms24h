FROM oven/bun:1 as base
WORKDIR /usr/src/app

# Install dependencies
COPY package.json bun.lockb* ./
RUN bun install --frozen-lockfile

# Copy source code
COPY . .

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1

# Start the application
CMD ["bun", "run", "index.js"]
