# TCSS 142 Project 3 – EmotionTester container
# Python image + headless JDK (same pattern as your old project)

FROM python:3.11-slim

# Install Java (default-jdk-headless is enough for javac/java)
RUN apt-get update \
 && apt-get install -y --no-install-recommends default-jdk-headless \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

# Install Flask + Gunicorn
RUN pip install --no-cache-dir flask gunicorn

# Optional defaults (Render also lets you set these in the dashboard)
ENV TESTER_FILE=EmotionAnalyzerTester.java
ENV RUN_TIMEOUT=30

EXPOSE 8080

# Let Render provide $PORT and run via Gunicorn
CMD gunicorn -w 2 -b 0.0.0.0:$PORT server:app
