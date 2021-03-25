#!/bin/bash
#
#Copyright 2021 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#
FLOWGATEUPGRAFEPATH="/root/flowgate"

echo 'Step1 : Importing new docker images'
if [ -f "$FLOWGATEUPGRAFEPATH/docker-images-output/flowgate.tar" ]
then
	docker load -i $FLOWGATEUPGRAFEPATH/docker-images-output/flowgate.tar
else
	echo "The file $FLOWGATEUPGRAFEPATH/docker-images-output/flowgate.tar is not exists"
	exit 1
fi

echo 'Step2 : Unziping new Configuration file'
if [ -f "$FLOWGATEUPGRAFEPATH/conf.tar.gz" ]
then
	tar -xvf $FLOWGATEUPGRAFEPATH/conf.tar.gz -C ~/flowgate/
	cp -r $FLOWGATEUPGRAFEPATH/conf/openmanage /opt/vmware/flowgate/conf
else
	echo "The file flowgate/conf.tar.gz is not exists"
	exit 1
fi

echo 'Step3 : Configing the redis for openmanage'
APICONFIEFILE="/opt/vmware/flowgate/conf/flowgate-api/application.properties"
OPENMANAGECONFIGFILE="/opt/vmware/flowgate/conf/openmanage/application.properties"
REDISPASSWD=$(grep "^spring.redis.password" $APICONFIEFILE)
REDISHOST=$(grep "^spring.redis.host" $APICONFIEFILE)
if [[ ${#REDISPASSWD} > 22 && ${#REDISHOST} > 18 ]]
then
   REDISPASSWD=${REDISPASSWD:22}
   REDISHOST="spring.redis.host="${REDISHOST:18}""
   sed -i -e "s/REDISPASSWD_CHANGE/$REDISPASSWD/" $OPENMANAGECONFIGFILE
   sed -i -e "s/spring.redis.host=localhost/$REDISHOST/" $OPENMANAGECONFIGFILE
   sed -i -e "s/apiserver.url=http:\/\/localhost/apiserver.url=http:\/\/flowgate-api/" $OPENMANAGECONFIGFILE
else
   echo "Invalid Configuration file: $OPENMANAGECONFIGFILE"
   exit 1
fi

echo 'Step4 : Upgrading old data'
PASSWORD=$(grep "^spring.couchbase.bucket.password" $APICONFIEFILE)
BUCKETNAME=$(grep "^spring.couchbase.bucket.name" $APICONFIEFILE)
if [[ ${#PASSWORD} > 33 && ${#BUCKETNAME} > 29 ]]
then
   PASSWORD=${PASSWORD:33}
   BUCKETNAME=${BUCKETNAME:29}
   docker-compose -f /opt/vmware/flowgate/docker-compose.run.images.yml up -d database
   docker cp upgrade.sql flowgate-database-container:/tmp/
   CONNECTION_ERROR_MSG="ERROR 107"
   UPGRADE_DATABASE_LOG="flowgateupgrade.log"
   while [[ ! -f $UPGRADE_DATABASE_LOG || `grep -c "$CONNECTION_ERROR_MSG" $UPGRADE_DATABASE_LOG` -ne '0' ]];do
       sleep 3
	   docker exec flowgate-database-container cbq -u $BUCKETNAME -p $PASSWORD -f /tmp/upgrade.sql > flowgateupgrade.log
       if [ `grep -c "$CONNECTION_ERROR_MSG" $UPGRADE_DATABASE_LOG` -eq '0' ];then
           echo "Upgrade data success."
           break
       fi
   done
else
   echo "invalid authentication"
   exit 1
fi

echo "Step5 : Rewriting docker-compose.run.images.yml file"
SERVICEKEY=$(grep "FLOWGATESERVICEKEY" /opt/vmware/flowgate/docker-compose.run.images.yml | head -1)
DOCKER_COMPOSE_YML="$FLOWGATEUPGRAFEPATH/maven-docker-build/docker-compose.run.images.yml"
DOCKER_COMPOSE_YML_1_1_2="/opt/vmware/flowgate/docker-compose.run.images.yml"
FIRST_BACKUP="1.2.0-docker-compose-yml-file.tar"

if [ ${#SERVICEKEY} > 30 ]
then
   SERVICEKEY=${SERVICEKEY:30} 
   CURRENT_TIMESTAMP=$(($(date +%s%N)/1000000))
   if [ -f $DOCKER_COMPOSE_YML ];then
	  if [ -f $FIRST_BACKUP ];then
	     tar -cvf 1.2.0-docker-compose-yml-file-$CURRENT_TIMESTAMP.tar -P $DOCKER_COMPOSE_YML
	  else
	     tar -cvf $FIRST_BACKUP -P $DOCKER_COMPOSE_YML
	  fi
   else
	  echo "No found "$DOCKER_COMPOSE_YML
	  exit 1
   fi
   sed -i -e "s/serviceKey/$SERVICEKEY/" $DOCKER_COMPOSE_YML
   cp $DOCKER_COMPOSE_YML $DOCKER_COMPOSE_YML_1_1_2
else
   echo "Invalid service key"
   exit 1
fi

echo "Step6 : Restarting flowgate"
systemctl start flowgate