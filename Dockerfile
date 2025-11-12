# TCSS 142 Project 3 – EmotionTester container
# Python image plus headless JDK, same pattern as your old project

FROM python:3.11-slim

# Install Java (enough for javac/java)
RUN apt-get update \
 && apt-get install -y --no-install-recommends default-jdk-headless \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

# Install Flask and Gunicorn
RUN pip install --no-cache-dir flask gunicorn

# Defaults, Render will also provide PORT
ENV TESTER_FILE=EmotionAnalyzerTester.java
ENV RUN_TIMEOUT=30

EXPOSE 8080

# Use Gunicorn to serve Flask app
CMD gunicorn -w 2 -b 0.0.0.0:$PORT server:app
