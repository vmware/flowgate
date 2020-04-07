FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY vc-worker*.jar /jar/vc-worker.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
