FROM photon:2.0

RUN tdnf distro-sync -y \
	&& tdnf install libstdc++ -y \
	&& tdnf install openjdk11.x86_64 -y \
	&& mkdir /log \
	&& mkdir /jar \
	&& mkdir /conf

ENV JAVA_HOME=/usr/lib/jvm/OpenJDK-11.0
ENV PATH=$PATH:$JAVA_HOME/bin

