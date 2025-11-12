# TCSS 142 Project 3 – EmotionTester container
FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY . .

# Install Python + Flask
RUN apt-get update && apt-get install -y python3 python3-pip \
    && python3 -m pip install --no-cache-dir flask

EXPOSE 8080
CMD ["python3", "server.py"]
