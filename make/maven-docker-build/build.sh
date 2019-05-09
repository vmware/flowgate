#!/bin/bash

BUILDDIR=/flowgate-build
BUILDLOG=/log/flowgate-build-log.txt
BUILDERRORLOG=/log/flowgate-build-error-log.txt
SOURCEDIR=$BUILDDIR/flowgate
MAKEDIR=$BUILDDIR/flowgate/make
OUTPUTJARPATH=$MAKEDIR/jar-output
OUTPUTJARNAME=-0.0.1-SNAPSHOT.jar

commonproject=("flowgate-common" "common-restclient" "worker-jobs")
serviceproject=("nlyte-worker" "poweriq-worker" "management" "infoblox-worker" "aggregator" "labsdb-worker")
specialproject=("vc-worker" "flowgate-api" "vro-worker")
databaseangredis=("database" "redis")

alldir=(${serviceproject[*]} ${specialproject[*]})


if [ -f "$BUILDLOG" ];then
	rm -f $BUILDLOG
fi
if [ -f "$BUILDERRORLOG" ];then
	rm -f $BUILDERRORLOG 
fi

## make conf tar
for n in "${alldir[@]}"
do
	mkdir -p $MAKEDIR/conf/$n/
	cp $SOURCEDIR/$n/src/main/resources/application.properties $MAKEDIR/conf/$n/
done

for m in "${databaseangredis[@]}"
do
	mkdir -p $MAKEDIR/conf/$m/
	cp $MAKEDIR/$m/*.conf $MAKEDIR/conf/$m/
done

mkdir $MAKEDIR/conf/cert
cp $MAKEDIR/management/Flowgate.cnf $MAKEDIR/conf/cert/
cp $MAKEDIR/database/init.sh $MAKEDIR/conf/database/
cp $MAKEDIR/database/initData.sh $MAKEDIR/conf/database/
cd $MAKEDIR
tar cvf conf.tar.gz conf

## copy ui to management
mkdir $SOURCEDIR/management/src/main/resources/static
cp $SOURCEDIR/ui/dist/* $SOURCEDIR/management/src/main/resources/static -rf

echo "Build common model." >> $BUILDLOG
for i in "${commonproject[@]}"
do
	cd $SOURCEDIR/$i
	mvn clean install >> $BUILDLOG
done

echo "Build special model." >> $BUILDLOG
for k in "${specialproject[@]}"
do
	cd $SOURCEDIR/$k
	mvn clean initialize  >> $BUILDLOG
	mvn package  >> $BUILDLOG
	if [ -f "target/$k$OUTPUTJARNAME" ];then
		cp target/*.jar $OUTPUTJARPATH/
	else
		echo "build $k$OUTPUTJARNAME failure" >> $BUILDERRORLOG
		mvn clean initialize  >> $BUILDERRORLOG
		mvn package  >> $BUILDERRORLOG
		continue
	fi
done

## copy flowgate-api's docs to management
mkdir $SOURCEDIR/management/src/main/resources/static/apidoc
cp $SOURCEDIR/flowgate-api/target/generated-docs/api-guide.html $SOURCEDIR/management/src/main/resources/static/apidoc/index.html

echo "Build service model." >> $BUILDLOG
for j in "${serviceproject[@]}"
do
	cd $SOURCEDIR/$j
	mvn clean package >> $BUILDLOG
	if [ -f "target/$j$OUTPUTJARNAME" ];then
		cp target/*.jar $OUTPUTJARPATH/
	else
		echo "build $j$OUTPUTJARNAME failure" >> $BUILDERRORLOG
		mvn clean package >> $BUILDERRORLOG
		continue
	fi
done
