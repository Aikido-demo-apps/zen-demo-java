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

# Runtime stage
FROM openjdk:17-slim

# Install make
RUN apt-get update && apt-get install -y make

# Set working directory
WORKDIR /app

# Copy the built application and SQL file from builder stage
COPY --from=builder /app .
COPY database.sql /app/database.sql

# Create startup script
RUN echo '#!/bin/bash\n\
# Wait for postgres to be ready\n\
until PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -U $POSTGRES_USER -d $POSTGRES_DB -c "\q"; do\n\
  echo "Postgres is unavailable - sleeping"\n\
  sleep 1\n\
done\n\
\n\
echo "Postgres is up - executing SQL script"\n\
PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -U $POSTGRES_USER -d $POSTGRES_DB -f /app/database.sql\n\
\n\
echo "Starting application"\n\
make run' > /app/start.sh

RUN chmod +x /app/start.sh

ENTRYPOINT ["/app/start.sh"]
