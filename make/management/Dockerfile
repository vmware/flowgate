FROM photon:2.0

RUN tdnf install openjre8.x86_64 -y \
	&& mkdir /jar \
	&& mkdir /log \
	&& mkdir /conf

COPY start.sh /
COPY management*.jar /jar/management.jar

VOLUME /log /conf
EXPOSE 80
WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
