#!/bin/bash
#
# Development Environment Startup Script
# 
# This script starts the development environment using Docker Compose.
# It will start all required services (PostgreSQL database, pgAdmin) 
# in detached mode, allowing you to continue working in the terminal.
#
# Usage: ./scripts/devup.sh
#
docker compose -f docker-compose.dev.yml up -d
