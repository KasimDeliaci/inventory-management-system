@echo off
REM
REM Development Environment Shutdown Script (Windows)
REM 
REM This script stops and removes all containers from the development environment.
REM It will cleanly shut down PostgreSQL database, pgAdmin, and remove the 
REM associated Docker network to free up system resources.
REM
REM Usage: scripts\devdown.bat
REM
docker compose -f docker-compose.dev.yml down
