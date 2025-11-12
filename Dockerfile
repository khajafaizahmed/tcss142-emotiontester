FROM openjdk:21-slim
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y python3 python3-pip && pip install flask
EXPOSE 8080
CMD ["python3", "server.py"]
