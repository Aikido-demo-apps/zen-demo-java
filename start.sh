#!/bin/bash

# Set /etc/hosts
echo "169.254.169.254   evil-stored-ssrf-hostname" >> /etc/hosts

# Parse DATABASE_URL
if [ -z "$DATABASE_URL" ]; then
    echo "DATABASE_URL environment variable is required"
    exit 1
fi

# Wait for postgres to be ready
until psql "$DATABASE_URL" -c "\q"; do
    echo "Postgres is unavailable - sleeping"
    sleep 1
done

echo "Postgres is up - executing SQL script"
psql "$DATABASE_URL" -f /app/database.sql
echo "Creating tmp dir"
mkdir -pv /app/.tmp

echo "Starting application"
AIKIDO_TMP_DIR=/app/.tmp make run
