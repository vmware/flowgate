FROM photon:2.0

ENV GOSU_VERSION 1.12

COPY docker-entrypoint.sh /usr/local/bin/

RUN tdnf distro-sync -y \
    && tdnf install shadow -y \
	&& groupadd -r -g 10000 redis \
	&& useradd --no-log-init -m -r -g 10000 -u 10000 redis \
	&& tdnf install -y redis.x86_64 wget.x86_64 \
	&& mkdir -p /data \
	&& chown redis:redis /data \
	&& chmod o+x /usr/local/bin/docker-entrypoint.sh \
	&& set -ex \
	&& rm -rf /var/lib/apt/lists/* \
	&& wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64" \
	&& wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64.asc" \
	&& export GNUPGHOME="$(mktemp -d)" \
	&& command -v gpgconf && gpgconf --kill all || : \
	&& rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
	&& chmod +x /usr/local/bin/gosu \
	&& gosu nobody true \
	&& mkdir -p /var/log/redis \
	&& chown redis:redis /var/log/redis \
	&& chown redis:redis /var/lib/redis

VOLUME /data /var/log/redis /var/lib/redis

WORKDIR /data

ENTRYPOINT ["docker-entrypoint.sh"]

EXPOSE 6379
CMD ["redis-server", "/etc/redis.conf"]
