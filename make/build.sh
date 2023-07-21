#!/bin/sh
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#

#####define all path 
CURRENTPATH=`pwd` #make

OUTPUTJARFILESNUM=10
UIDISTFILENUM=18
TIMEOUTSECOND=900
BUILDLOG=$CURRENTPATH/build-log
SOURCECODEDIR=$CURRENTPATH/../
OUTPUTJARPATH=$CURRENTPATH/jar-output
OUTPUTIMAGEPATH=$CURRENTPATH/docker-images-output
DOCKERMAVENBUILD=$CURRENTPATH/maven-docker-build

FLOWGATEIMAGESTAR=$OUTPUTIMAGEPATH/flowgate.tar
DOCKERCOMPOSEBUILDIMAGESFILE=$DOCKERMAVENBUILD/docker-compose.build.images.yml
DOCKERCOMPOSEBUILDJARFILE=$DOCKERMAVENBUILD/docker-compose.build.jar.yml
DOCKERCOMPOSERUNFILE=$DOCKERMAVENBUILD/docker-compose.run.images.yml
DOCKERCOMPOSEBUILDUIFILE=$DOCKERMAVENBUILD/buildui.yml

CONFTAR=$CURRENTPATH/conf.tar.gz


#####set version
if [[ $2 == '-version' ]]; then
	INPUT=$3
	FLOWGATE_VERSION=${INPUT:=v1.0}
	echo $FLOWGATE_VERSION
else
	echo "eg. 'bash build.sh ( ui | jar | image | save | all ) -version v1.0'"
	exit 0
fi
RELEASES_VERSION=flowgate-$FLOWGATE_VERSION.tar.gz
sed -i -e "s/FLOWGATE_VERSION/$FLOWGATE_VERSION/g" $DOCKERCOMPOSEBUILDIMAGESFILE
sed -i -e "s/FLOWGATE_VERSION/$FLOWGATE_VERSION/g" $DOCKERCOMPOSERUNFILE
sed -i -e "s/FLOWGATE_VERSION/$FLOWGATE_VERSION/g" $DOCKERCOMPOSEBUILDJARFILE

buildUi(){

	echo "build UI..."
	cd $CURRENTPATH
    docker rm nodejs-build-container
    docker-compose -f $DOCKERCOMPOSEBUILDUIFILE build --force-rm --no-cache
    docker-compose -f $DOCKERCOMPOSEBUILDUIFILE up

}

buildAllJars(){

	echo "build all project jar..."

	if [ ! -d "$OUTPUTJARPATH" ];then
		mkdir $OUTPUTJARPATH
	else
		rm $OUTPUTJARPATH/* -rf
	fi

	cd $CURRENTPATH
	chmod a+x $DOCKERMAVENBUILD/database-build/entrypoint.sh
	chmod a+x $DOCKERMAVENBUILD/database-build/init.sh
	sed -i -e "s/localhost/database-build/g" $SOURCECODEDIR/flowgate-api/src/test/resources/application.properties
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

	docker rm maven-build-container database-build-container -f
	docker rmi flowgate/vro-worker:$FLOWGATE_VERSION flowgate/vc-worker:$FLOWGATE_VERSION flowgate/nlyte-worker:$FLOWGATE_VERSION \
    flowgate/management:$FLOWGATE_VERSION flowgate/infoblox-worker:$FLOWGATE_VERSION flowgate/labsdb-worker:$FLOWGATE_VERSION \
    flowgate/poweriq-worker:$FLOWGATE_VERSION flowgate/aggregator:$FLOWGATE_VERSION flowgate/api:$FLOWGATE_VERSION \
    flowgate/redis:$FLOWGATE_VERSION flowgate/database:$FLOWGATE_VERSION maven-build:$FLOWGATE_VERSION database-build:$FLOWGATE_VERSION \
    flowgate/openmanage:$FLOWGATE_VERSION

	if [ ! -d "$OUTPUTIMAGEPATH" ];then
		mkdir $OUTPUTIMAGEPATH
	else
		rm $OUTPUTIMAGEPATH/* -rf
	fi

	jarname=("flowgate-api" "vro-worker" "nlyte-worker" "poweriq-worker" "management" "infoblox-worker" "aggregator" "vc-worker" "labsdb-worker" "openmanage")
	for j in "${jarname[@]}"
	do
		cp $OUTPUTJARPATH/$j-*.jar $CURRENTPATH/$j
	done

	docker-compose -f $DOCKERCOMPOSEBUILDIMAGESFILE build --force-rm --no-cache
}

saveDockerImages(){

	echo "save docker images..."

	if [ -f "$FLOWGATEIMAGESTAR" ];then
		rm -f $FLOWGATEIMAGESTAR
	fi
	
	docker pull alberthaoyu/database:v1.0
	docker tag alberthaoyu/database:v1.0 flowgate/database:$FLOWGATE_VERSION
	docker save flowgate/vro-worker:$FLOWGATE_VERSION flowgate/vc-worker:$FLOWGATE_VERSION flowgate/nlyte-worker:$FLOWGATE_VERSION \
	flowgate/management:$FLOWGATE_VERSION flowgate/infoblox-worker:$FLOWGATE_VERSION flowgate/labsdb-worker:$FLOWGATE_VERSION \
	flowgate/poweriq-worker:$FLOWGATE_VERSION flowgate/aggregator:$FLOWGATE_VERSION flowgate/api:$FLOWGATE_VERSION \
	flowgate/redis:$FLOWGATE_VERSION flowgate/database:$FLOWGATE_VERSION flowgate/openmanage:$FLOWGATE_VERSION >> $FLOWGATEIMAGESTAR
	mkdir flowgate
	mkdir -p flowgate/docker-images-output
	mkdir -p flowgate/maven-docker-build
	mkdir -p flowgate/script/1.2.1
	cp flowgate_run.sh flowgate_init.sh conf.tar.gz flowgate
	cp maven-docker-build/docker-compose.run.images.yml flowgate/maven-docker-build
	cp docker-images-output/flowgate.tar flowgate/docker-images-output
	chmod +x upgrade.sh
	cp upgrade.sh flowgate/script/1.2.1
	cp database/upgradeFrom1.1.2To1.2.sql flowgate/script/1.2.1
	tar -cvzf $RELEASES_VERSION flowgate
	rm flowgate -rf
}

buildDatabaseImage(){
	
	echo "build database image..."
	
	cd $CURRENTPATH/database
	chmod a+x entrypoint.sh
	chmod a+x init.sh
	chmod a+x initData.sh
	
	docker build -t flowgate/database:$FLOWGATE_VERSION .
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
	"database")
		buildDatabaseImage
	;;
	"all")
		buildUi
		buildAllJars
		buildDockerImages
		saveDockerImages
		echo "build success."
	;;
	*)
		echo "bash build.sh ( ui | jar | image | save | database | all ) -version v1.0"
	;;
esac
