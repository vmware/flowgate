#!/bin/bash
#
#Copyright 2021 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#
CURRENTPATH=`pwd`
SOURCEDIR=$CURRENTPATH/../../
is_root() {
	if [ $(id -u) -ne 0 ]; 
	then
		echo "Please run as root user"
		exit 1
	fi
}
is_root
echo 'Step 1/4 : Importing new docker images'
if [ -f "$SOURCEDIR/docker-images-output/flowgate.tar" ]
then
	docker load -i $SOURCEDIR/docker-images-output/flowgate.tar
else
	echo "The file $SOURCEDIR/docker-images-output/flowgate.tar is not exists"
	exit 1
fi

echo 'Step 2/4 : Reconfigure flowgate'
APICONFIEFILE="/opt/vmware/flowgate/conf/flowgate-api/application.properties"
OPENMANAGECONFIGFILE="/opt/vmware/flowgate/conf/openmanage/application.properties"
REDISPASSWD=$(grep "^spring.redis.password=" $APICONFIEFILE | cut -d'=' -f2 | xargs)
SERVICEKEY=$(grep "FLOWGATESERVICEKEY" /opt/vmware/flowgate/docker-compose.run.images.yml | head -1 | cut -d':' -f2 | xargs)
DOCKER_COMPOSE_YML="$SOURCEDIR/maven-docker-build/docker-compose.run.images.yml"
DOCKER_COMPOSE_YML_1_1_2="/opt/vmware/flowgate/docker-compose.run.images.yml"

if [ -f "$SOURCEDIR/conf.tar.gz" ]
then
	tar -xvf $SOURCEDIR/conf.tar.gz -C $SOURCEDIR
	cp -r $SOURCEDIR/conf/openmanage /opt/vmware/flowgate/conf
	sed -i -e "s/REDISPASSWD_CHANGE/$REDISPASSWD/" $OPENMANAGECONFIGFILE
	if [ -f $DOCKER_COMPOSE_YML_1_1_2 ];then
		CURRENT_TIMESTAMP=$(($(date +%s%N)/1000000))
		cp $DOCKER_COMPOSE_YML_1_1_2 /opt/vmware/flowgate/docker-compose.run.images-backup-$CURRENT_TIMESTAMP.yml
		cp $DOCKER_COMPOSE_YML $DOCKER_COMPOSE_YML_1_1_2
		sed -i -e "s/serviceKey/$SERVICEKEY/" $DOCKER_COMPOSE_YML_1_1_2
	else
		echo "Not found $DOCKER_COMPOSE_YML_1_1_2"
	fi
else
	echo "Reconfigure fail"
	exit 1
fi

echo 'Step 3/4 : Upgrading database'
DBPASSWORD=$(grep "^spring.couchbase.bucket.password" $APICONFIEFILE  | cut -d'=' -f2 | xargs)
BUCKETNAME=$(grep "^spring.couchbase.bucket.name" $APICONFIEFILE  | cut -d'=' -f2 | xargs)

docker-compose -f /opt/vmware/flowgate/docker-compose.run.images.yml up -d database
sleep 10
docker cp upgradeFrom1.1.2To1.2.sql flowgate-database-container:/tmp/
COUNTER=0
while [[ $COUNTER -lt 20 ]];do
   docker exec -it flowgate-database-container sh -c "curl --silent http://localhost:8091/pools || exit 1"
   if [ $? -eq 0 ]; then
	  echo "Database container is ready."
	  docker exec flowgate-database-container cbq -u $BUCKETNAME -p $DBPASSWORD -f /tmp/upgradeFrom1.1.2To1.2.sql >> flowgateupgrade.log
      break
   else
	  echo "Database is not ready, retry Counter: $COUNTER" >> flowgateupgrade.log
   fi
   COUNTER=$((COUNTER+1))
   sleep 5
done

echo "Step 4/4 : Restarting flowgate"
systemctl daemon-reload
systemctl start flowgate
echo "Flowgate started"
