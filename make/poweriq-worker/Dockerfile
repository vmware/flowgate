FROM photon:2.0

RUN tdnf distro-sync -y \
    && tdnf install openjre8.x86_64 -y \
	&& mkdir /log \
	&& mkdir /jar \
	&& mkdir /conf

VOLUME /log /conf
COPY start.sh /
COPY poweriq-worker*.jar /jar/poweriq-worker.jar

WORKDIR /
ENTRYPOINT [ "sh", "start.sh" ]
