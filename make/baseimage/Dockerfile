FROM photon:2.0

RUN tdnf distro-sync -y \
	&& tdnf install openjdk11.x86_64 -y \
	&& mkdir /log \
	&& mkdir /jar \
	&& mkdir /conf
