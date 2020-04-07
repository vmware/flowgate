FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY flowgate-api*.jar /jar/flowgate-api.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
