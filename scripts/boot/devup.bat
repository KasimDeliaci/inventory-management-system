@echo off
REM
REM Development Environment Startup Script (Windows)
REM 
REM This script starts the development environment using Docker Compose.
REM It will start all required services (PostgreSQL database, pgAdmin) 
REM in detached mode, allowing you to continue working in the command prompt.
REM
REM Usage: scripts\boot\devup.bat
REM
docker compose -f docker-compose.dev.yml up -d
