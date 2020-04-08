FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY nlyte-worker*.jar /jar/nlyte-worker.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
