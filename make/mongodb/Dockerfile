FROM photon:2.0

# grab gosu for easy step-down from root (https://github.com/tianon/gosu/releases)
ENV GOSU_VERSION 1.10
# grab "js-yaml" for parsing mongod's YAML config files (https://github.com/nodeca/js-yaml/releases)
ENV JSYAML_VERSION 3.10.0

COPY docker-entrypoint.sh /usr/local/bin/

RUN tdnf install -y shadow gzip postgresql \
	&& tdnf install -y mongodb.x86_64 wget.x86_64 jq\
	&& groupadd -r mongodb && useradd -r -g mongodb mongodb \
	&& mkdir /data /log /mongodconf \
	&& mkdir /docker-entrypoint-initdb.d \
	&& mkdir -p /data/db /data/configdb \
	&& chown -R mongodb:mongodb /data/db /data/configdb \
	&& set -ex \
    && rm -rf /var/lib/apt/lists/* \
    && wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64" \
    && wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64.asc" \
    && export GNUPGHOME="$(mktemp -d)" \
    && command -v gpgconf && gpgconf --kill all || : \
    && rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
    && chmod +x /usr/local/bin/gosu \
    && gosu nobody true \
    && wget -O /js-yaml.js "https://github.com/nodeca/js-yaml/raw/${JSYAML_VERSION}/dist/js-yaml.js" \
	&& ln -s usr/local/bin/docker-entrypoint.sh /entrypoint.sh \
	&& chmod o+x /usr/local/bin/docker-entrypoint.sh


VOLUME /data/db /data/configdb /log

ENTRYPOINT ["docker-entrypoint.sh"]
EXPOSE 27017
CMD ["mongod"]