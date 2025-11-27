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
FROM amazoncorretto:17-alpine

# Install make and postgresql-client
RUN apk update && \
    apk add make postgresql-client bash && \
    rm -rf /var/cache/apk/*

# Set working directory
WORKDIR /app

# Copy the built application and SQL file from builder stage
COPY --from=builder /app .
COPY database.sql /app/database.sql

# Set new entry for stored ssrf in /etc/hosts
RUN echo "169.254.169.254   evil-stored-ssrf-hostname" >> /etc/hosts

# Copy startup script
COPY start.sh /app/start.sh
RUN chmod 755 /app/start.sh

ENTRYPOINT ["/app/start.sh"]
