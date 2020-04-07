FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY infoblox-worker*.jar /jar/infoblox-worker.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
