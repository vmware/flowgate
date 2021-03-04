FROM baseimage:1.0

VOLUME /log /conf
COPY start.sh /
COPY openmanage*.jar /jar/openmanage.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
