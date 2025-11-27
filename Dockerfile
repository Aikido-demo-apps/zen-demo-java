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

# /etc/hosts stage
FROM amazoncorretto:17-alpine as etchosts
WORKDIR /tmp
RUN cp /etc/hosts hostsfile
RUN echo "127.0.0.1   my-custom-hostname" >> hostsfile

# Runtime stage
FROM amazoncorretto:17-alpine

# Set new entry for stored ssrf in /etc/hosts
COPY --from=etchosts /tmp/hostsfile /etc/hosts

# Install make and postgresql-client
RUN apk update && \
    apk add make postgresql-client bash && \
    rm -rf /var/cache/apk/*

# Set working directory
WORKDIR /app

# Copy the built application and SQL file from builder stage
COPY --from=builder /app .
COPY database.sql /app/database.sql



# Copy startup script
COPY start.sh /app/start.sh
RUN chmod 755 /app/start.sh

ENTRYPOINT ["/app/start.sh"]
