FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY labsdb-worker*.jar /jar/labsdb-worker.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
