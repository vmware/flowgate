FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY poweriq-worker*.jar /jar/poweriq-worker.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
