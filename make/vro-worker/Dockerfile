FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY vro-worker*.jar /jar/vro-worker.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
