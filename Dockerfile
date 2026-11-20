# TCSS 142 Project 3 – EmotionTester container
# Python image plus headless JDK to compile/run Java tester

FROM python:3.11-slim

# Install Java (JDK headless) for javac/java
RUN apt-get update \
 && apt-get install -y --no-install-recommends default-jdk-headless \
 && rm -rf /var/lib/apt/lists/*

# App directory
WORKDIR /app

# Copy all repo files into the image
COPY . .

# Python dependencies: Flask + Gunicorn web server
RUN pip install --no-cache-dir flask gunicorn

# Environment variables for the tester
ENV TESTER_FILE=EmotionAnalyzerTester.java
ENV RUN_TIMEOUT=30

# Render will inject PORT, but we expose for local clarity
EXPOSE 8080

# Start Flask app via Gunicorn
# Run as an unprivileged user so student code can't modify the app or system files
RUN useradd --create-home --uid 10001 runner
USER runner

CMD gunicorn -w 2 -b 0.0.0.0:$PORT server:app
