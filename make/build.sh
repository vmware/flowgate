#!/bin/sh
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#

#####define all path 
CURRENTPATH=`pwd` #make

OUTPUTJARFILESNUM=8
UIDISTFILENUM=18
TIMEOUTSECOND=240
BUILDLOG=$CURRENTPATH/build-log
SOURCECODEDIR=$CURRENTPATH/../../flowgate
OUTPUTJARPATH=$CURRENTPATH/jar-output
OUTPUTIMAGEPATH=$CURRENTPATH/docker-images-output
DOCKERMAVENBUILD=$CURRENTPATH/maven-docker-build

FLOWGATEIMAGESTAR=$OUTPUTIMAGEPATH/flowgate.tar
DOCKERCOMPOSEBUILDIMAGESFILE=$DOCKERMAVENBUILD/docker-compose.build.images.yml
DOCKERCOMPOSEBUILDJARFILE=$DOCKERMAVENBUILD/docker-compose.build.jar.yml
DOCKERCOMPOSERUNFILE=$DOCKERMAVENBUILD/docker-compose.run.images.yml
CONFTAR=$CURRENTPATH/conf.tar.gz

APACHERESOURCEDIR=/var/www/html

#####set version
if [[ $2 == '-version' ]]; then
	INPUT=$3
	FLOWGATE_VERSION=${INPUT:=v1.0}
	echo $FLOWGATE_VERSION
else
	echo "eg. 'bash build.sh ( ui | jar | image | save | copy2server | all ) -version v1.0'"
	exit 0
fi
sed -i -e "s/FLOWGATE_VERSION/$FLOWGATE_VERSION/g" $DOCKERCOMPOSEBUILDIMAGESFILE
sed -i -e "s/FLOWGATE_VERSION/$FLOWGATE_VERSION/g" $DOCKERCOMPOSERUNFILE
sed -i -e "s/FLOWGATE_VERSION/$FLOWGATE_VERSION/g" $DOCKERCOMPOSEBUILDJARFILE

buildUi(){

	echo "build UI..."

	cd $SOURCECODEDIR/ui/
	wget https://nodejs.org/dist/v11.2.0/node-v11.2.0-linux-x64.tar.gz
	tar zxvf node-v11.2.0-linux-x64.tar.gz
	export PATH=$PATH:$PWD/node-v11.2.0-linux-x64/bin
	npm install -g @angular/cli@latest
	npm install --unsafe-perm
	ng build -prod -aot=false
}

buildAllJars(){

	echo "build all project jar..."

	if [ ! -d "$OUTPUTJARPATH" ];then
		mkdir $OUTPUTJARPATH
	else
		rm $OUTPUTJARPATH/* -rf
	fi

	cd $CURRENTPATH

	docker-compose -f $DOCKERCOMPOSEBUILDJARFILE build --force-rm --no-cache
    docker-compose -f $DOCKERCOMPOSEBUILDJARFILE up -d

	#####check jar-output finish or not

	filesnum=`ls $OUTPUTJARPATH | wc -w`
	currtime=`date "+%s"`

	while(( $filesnum < $OUTPUTJARFILESNUM ))
	do
	    sleep 1
	    filesnum=`ls $OUTPUTJARPATH | wc -w`
	    nowtime=`date "+%s"`
	    if [ $((nowtime-currtime)) -gt $TIMEOUTSECOND ]
	    then
	        echo "error. timeout. maybe build jar fault"
	        exit -1
	    fi
	done
}

buildDockerImages(){

	echo "build docker images..."

	docker rm maven-build-container -f
	docker rmi flowgate/vro-worker:$FLOWGATE_VERSION flowgate/vc-worker:$FLOWGATE_VERSION flowgate/nlyte-worker:$FLOWGATE_VERSION \
    flowgate/management:$FLOWGATE_VERSION flowgate/infoblox-worker:$FLOWGATE_VERSION flowgate/labsdb-worker:$FLOWGATE_VERSION \
    flowgate/poweriq-worker:$FLOWGATE_VERSION flowgate/aggregator:$FLOWGATE_VERSION flowgate/api:$FLOWGATE_VERSION \
    flowgate/redis:$FLOWGATE_VERSION flowgate/mongodb:$FLOWGATE_VERSION maven-build:$FLOWGATE_VERSION

	if [ ! -d "$OUTPUTIMAGEPATH" ];then
		mkdir $OUTPUTIMAGEPATH
	else
		rm $OUTPUTIMAGEPATH/* -rf
	fi

	jarname=("flowgate-api" "vro-worker" "nlyte-worker" "poweriq-worker" "management" "infoblox-worker" "aggregator" "vc-worker")
	for j in "${jarname[@]}"
	do
		cp $OUTPUTJARPATH/$j-0.0.1-SNAPSHOT.jar $CURRENTPATH/$j
	done

	cd $CURRENTPATH
	docker-compose -f $DOCKERCOMPOSEBUILDIMAGESFILE build --force-rm --no-cache
}

saveDockerImages(){

	echo "save docker images..."

	if [ -f "$FLOWGATEIMAGESTAR" ];then
		rm -f $FLOWGATEIMAGESTAR
	fi

	docker save flowgate/vro-worker:$FLOWGATE_VERSION flowgate/vc-worker:$FLOWGATE_VERSION flowgate/nlyte-worker:$FLOWGATE_VERSION \
	flowgate/management:$FLOWGATE_VERSION flowgate/infoblox-worker:$FLOWGATE_VERSION flowgate/labsdb-worker:$FLOWGATE_VERSION \
	flowgate/poweriq-worker:$FLOWGATE_VERSION flowgate/aggregator:$FLOWGATE_VERSION flowgate/api:$FLOWGATE_VERSION \
	flowgate/redis:$FLOWGATE_VERSION flowgate/mongodb:$FLOWGATE_VERSION >> $FLOWGATEIMAGESTAR
}

copyResourceToServer(){

	echo "copy all resource to apache server..."

	cp $DOCKERCOMPOSERUNFILE $FLOWGATEIMAGESTAR $CONFTAR $APACHERESOURCEDIR
}

case $1 in
	"ui")
		buildUi
	;;
	"jar")
		buildAllJars
	;;
	"image")
		buildDockerImages
	;;
	"save")
		saveDockerImages
	;;
	"copy2server")
		copyResourceToServer
	;;
	"all")
		buildUi
		buildAllJars
		buildDockerImages
		saveDockerImages
		copyResourceToServer
		echo "build success."
	;;
	*)
		echo "bash build.sh ( ui | jar | image | save | copy2server | all ) -version v1.0"
	;;
esac
