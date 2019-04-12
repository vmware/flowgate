#!/usr/bin/bash
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#

#####define all path
CURRENTPATH=`pwd` #make
DOCKERMAVENBUILD=$CURRENTPATH/maven-docker-build
FLOWGATEOPTDIR=/opt/vmware/flowgate
DOCKERCOMPOSERUNFILE=$DOCKERMAVENBUILD/docker-compose.run.images.yml
CONFTAR=$CURRENTPATH/conf.tar.gz

SERVICEPROJECT=("flowgate-api" "vro-worker" "nlyte-worker" "poweriq-worker" "management" "infoblox-worker" "aggregator" "mongodb" "redis" "vc-worker" "labsdb-worker")

if [ -d "$FLOWGATEOPTDIR" ];then
    rm $FLOWGATEOPTDIR -rf
fi
mkdir -p $FLOWGATEOPTDIR
mkdir $FLOWGATEOPTDIR/conf
mkdir $FLOWGATEOPTDIR/log
mkdir $FLOWGATEOPTDIR/data
mkdir $FLOWGATEOPTDIR/data/mongodb
mkdir $FLOWGATEOPTDIR/data/redis

for i in "${SERVICEPROJECT[@]}"
do
   mkdir $FLOWGATEOPTDIR/log/$i
done

tar xvf $CONFTAR -C $FLOWGATEOPTDIR

groupadd -r -g 10000 flowgate
useradd --no-log-init -m -r -g 10000 -u 10000 flowgate
chown -R 10000:10000 $FLOWGATEOPTDIR/log/redis
chown -R 10000:10000 $FLOWGATEOPTDIR/data/redis
chmod o+rx $FLOWGATEOPTDIR/conf/mongodb/initdb.js

SEDPROJECT=("flowgate-api" "vro-worker" "nlyte-worker" "poweriq-worker" "management" "infoblox-worker" "aggregator" "vc-worker" "labsdb-worker")
for j in "${SEDPROJECT[@]}"
do
    sed -i -e 's/spring.data.mongodb.host=localhost/spring.data.mongodb.host=mongodb/' $FLOWGATEOPTDIR/conf/$j/application.properties
    sed -i -e 's/spring.redis.host=localhost/spring.redis.host=redis/' $FLOWGATEOPTDIR/conf/$j/application.properties
    sed -i -e 's/apiserver.url=http:\/\/localhost/apiserver.url=http:\/\/flowgate-api/' $FLOWGATEOPTDIR/conf/$j/application.properties
done

ADMINPASSWD=$(openssl rand 32|sha256sum|head -c 30)
sed -i -e "s/ADMINPASSWD_CHANGE/$ADMINPASSWD/g" $FLOWGATEOPTDIR/conf/mongodb/initdb.js
sed -i -e "s/ADMINPASSWD_CHANGE/$ADMINPASSWD/g" $FLOWGATEOPTDIR/conf/mongodb/mongod.env

USERPASSWD=$(openssl rand 32|sha256sum|head -c 30)
sed -i -e "s/USERPASSWD_CHANGE/$USERPASSWD/" $FLOWGATEOPTDIR/conf/mongodb/initdb.js
sed -i -e "s/USERPASSWD_CHANGE/$USERPASSWD/" $FLOWGATEOPTDIR/conf/flowgate-api/application.properties

REDISPASSWD=$(openssl rand 32|sha256sum|head -c 64)
SEDREDISPASSWD=("flowgate-api" "vro-worker" "nlyte-worker" "poweriq-worker" "infoblox-worker" "aggregator" "vc-worker")
for i in "${SEDREDISPASSWD[@]}"
do
    sed -i -e "s/REDISPASSWD_CHANGE/$REDISPASSWD/" $FLOWGATEOPTDIR/conf/redis/redis.conf
    sed -i -e "s/REDISPASSWD_CHANGE/$REDISPASSWD/" $FLOWGATEOPTDIR/conf/$i/application.properties
done

SERVICEKEY=$(openssl rand 32|sha256sum|head -c 64)
sed -i -e "s/serviceKey/$SERVICEKEY/" $DOCKERCOMPOSERUNFILE

docker-compose -f $DOCKERCOMPOSERUNFILE up -d
