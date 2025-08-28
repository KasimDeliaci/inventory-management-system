#!/bin/bash
#
# Development Environment Shutdown Script
# 
# This script stops and removes all containers from the development environment.
# It will cleanly shut down PostgreSQL database, pgAdmin, and remove the 
# associated Docker network to free up system resources.
#
# Usage: ./scripts/devdown.sh
#
docker compose -f docker-compose.dev.yml down
