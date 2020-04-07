FROM baseimage:1.0

COPY start.sh /
COPY management*.jar /jar/management.jar

VOLUME /log /conf
EXPOSE 80
WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
