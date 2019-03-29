FROM photon:2.0

RUN tdnf install openjre8.x86_64 -y \
	&& mkdir /log \
	&& mkdir /jar \
	&& mkdir /conf

VOLUME /log /conf
COPY start.sh /
COPY vc-worker*.jar /jar/vc-worker.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
