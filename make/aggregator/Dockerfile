FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY aggregator*.jar /jar/aggregator.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
