#!/bin/bash
echo "🚀 Deploying Microfinance System - Production"

# Load environment variables
set -a
source .env
set +a

# Build with production profile
mvn clean package -Pprod -DskipTests

# Build Docker image
docker build -t your-registry/microfinance:latest .

# Deploy to production
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d

echo "✅ Production deployment completed!"
echo "📱 Application: http://your-domain.com"
echo "📈 Health Check: http://your-domain.com/api/health"