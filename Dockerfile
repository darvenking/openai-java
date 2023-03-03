FROM openjdk:17

# 设置固定的项目路径
ENV WORKDIR /home/chat
ENV APPNAME app.jar

ADD target/openai-1.0.0.jar $WORKDIR/$APPNAME
RUN chmod +x $WORKDIR/$APPNAME

ENTRYPOINT 9915
ENTRYPOINT ["java", "-jar", "/$APPNAME"]