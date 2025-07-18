# Build stage
FROM gradle:7.6.1-jdk17 AS builder

# Install make
RUN apt-get update && apt-get install -y make

# Set working directory
WORKDIR /app

# Copy your source code, including Makefile
COPY . .

# Run make download
RUN make download
RUN make build

# Runtime stage
FROM openjdk:17-slim

# Install make and postgresql-client
RUN apt-get update && \
    apt-get install -y make postgresql-client && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built application and SQL file from builder stage
COPY --from=builder /app .
COPY database.sql /app/database.sql

# Create startup script
RUN echo '#!/bin/bash\n\
\n\
# Parse DATABASE_URL\n\
if [ -z "$DATABASE_URL" ]; then\n\
    echo "DATABASE_URL environment variable is required"\n\
    exit 1\n\
fi\n\
\n\
# Wait for postgres to be ready\n\
until psql "$DATABASE_URL" -c "\q"; do\n\
    echo "Postgres is unavailable - sleeping"\n\
    sleep 1\n\
done\n\
\n\
echo "Postgres is up - executing SQL script"\n\
psql "$DATABASE_URL" -f /app/database.sql\n\
echo "Creating tmp dir"\n\
mkdir -pv /app/.tmp\n\
\n\
echo "Starting application"\n\
AIKIDO_TMP_DIR=/app/.tmp make run' > /app/start.sh

RUN chmod 755 /app/start.sh

ENTRYPOINT ["/app/start.sh"]
