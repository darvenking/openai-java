FROM java:8-jre
ADD target/openai-1.0.0.war app.war
RUN bash -c 'touch /app.war'
ENTRYPOINT 9915
ENTRYPOINT ["java", "-jar", "/app.war"]