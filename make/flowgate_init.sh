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

SERVICEPROJECT=("flowgate-api" "vro-worker" "nlyte-worker" "poweriq-worker" "management" "infoblox-worker" "aggregator" "database" "redis" "vc-worker" "labsdb-worker" "openmanage")

if [ -d "$FLOWGATEOPTDIR" ];then
    rm $FLOWGATEOPTDIR -rf
fi
mkdir -p $FLOWGATEOPTDIR
mkdir $FLOWGATEOPTDIR/conf
mkdir $FLOWGATEOPTDIR/log
mkdir $FLOWGATEOPTDIR/data
mkdir $FLOWGATEOPTDIR/data/database
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
chmod a+x $FLOWGATEOPTDIR/conf/database/init.sh
chmod a+x $FLOWGATEOPTDIR/conf/database/initData.sh

ADMINPASSWD=$(openssl rand 32|sha256sum|head -c 24)
sed -i -e "s/ADMINPASSWD_CHANGE/$ADMINPASSWD/g" $FLOWGATEOPTDIR/conf/database/init.sh

USERPASSWD=$(openssl rand 32|sha256sum|head -c 24)
sed -i -e "s/USERPASSWD_CHANGE/$USERPASSWD/" $FLOWGATEOPTDIR/conf/database/init.sh
sed -i -e "s/COUCHBASEPASSWD_CHANGE/$USERPASSWD/" $FLOWGATEOPTDIR/conf/flowgate-api/application.properties

REDISPASSWD=$(openssl rand 32|sha256sum|head -c 64)
SEDREDISPASSWD=("flowgate-api" "vro-worker" "nlyte-worker" "poweriq-worker" "infoblox-worker" "aggregator" "vc-worker" "labsdb-worker" "openmanage")
for i in "${SEDREDISPASSWD[@]}"
do
    sed -i -e "s/REDISPASSWD_CHANGE/$REDISPASSWD/" $FLOWGATEOPTDIR/conf/redis/redis.conf
    sed -i -e "s/REDISPASSWD_CHANGE/$REDISPASSWD/" $FLOWGATEOPTDIR/conf/$i/application.properties
done

SERVICEKEY=$(openssl rand 32|sha256sum|head -c 64)
sed -i -e "s/serviceKey/$SERVICEKEY/" $DOCKERCOMPOSERUNFILE

docker load < docker-images-output/flowgate.tar

docker-compose -f $DOCKERCOMPOSERUNFILE up -d database
while [ ! -f "/opt/vmware/flowgate/data/database/data/initDataComplete" ];do
    sleep 1
    if [ -f "/opt/vmware/flowgate/data/database/data/initDataFailed" ];then
        echo "Flowgate Initialize Data Failed."
        docker-compose -f $DOCKERCOMPOSERUNFILE down
        exit
    fi
done
docker-compose -f $DOCKERCOMPOSERUNFILE down
echo "Flowgate Initialize Success."