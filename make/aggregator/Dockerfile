FROM photon:2.0

RUN tdnf install openjre8.x86_64 -y \
	&& mkdir /jar \
	&& mkdir /log \
	&& mkdir /conf

VOLUME /log /conf
COPY start.sh /
COPY aggregator*.jar /jar/aggregator.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
