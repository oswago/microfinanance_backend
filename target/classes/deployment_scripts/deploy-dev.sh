#!/bin/bash
echo "🚀 Deploying Microfinance System - Development"

# Build the application
mvn clean package -DskipTests

# Start services
docker-compose down
docker-compose up --build -d

echo "✅ Development deployment completed!"
echo "📱 Application: http://localhost:8080/api"
echo "🗄️  Database: localhost:5432"
echo "📊 H2 Console: http://localhost:8080/api/h2-console"