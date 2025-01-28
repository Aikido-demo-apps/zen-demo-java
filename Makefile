# Define variables
GRADLEW = ./gradlew
JAR_FILE = build/libs/JavalinPostgres-1.0-SNAPSHOT-all.jar
JAVA_AGENT = zen_by_aikido/dist/agent.jar

# Default target
.PHONY: all
all: build

# Download zen : 
.PHONY: download
download: clean
	curl -L -O https://github.com/AikidoSec/firewall-java/releases/download/v1.0.3/zen.zip
	unzip zen.zip -d zen_by_aikido
	rm zen.zip
# Build the project
.PHONY: build
build:
	@echo "Building the project..."
	chmod +x $(GRADLEW)
	$(GRADLEW) shadowJar

# Run the application with the Java agent
.PHONY: run
run: build
	@echo "Running JavalinPostgres with Zen & ENV (http://localhost:8080)"
	AIKIDO_BLOCK=1 java -javaagent:$(JAVA_AGENT) -DportNumber=8080 -jar $(JAR_FILE)

# Clean the project
.PHONY: clean
clean:
	@echo "Cleaning the project..."
	$(GRADLEW) clean
	rm -r zen_by_aikido
