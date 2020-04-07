FROM photon:2.0

RUN tdnf distro-sync -y \
	&& tdnf install openjre8.x86_64 -y \
	&& mkdir /log \
	&& mkdir /jar \
	&& mkdir /conf
