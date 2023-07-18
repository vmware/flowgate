#!/bin/bash

BUILDDIR=/flowgate-build
BUILDLOG=/log/flowgate-build-log.txt
BUILDERRORLOG=/log/flowgate-build-error-log.txt
SOURCEDIR=$BUILDDIR
MAKEDIR=$BUILDDIR/make
OUTPUTJARPATH=$MAKEDIR/jar-output
OUTPUTJARNAME=-*.jar

commonproject=("flowgate-common" "common-restclient" "worker-jobs")
serviceproject=("nlyte-worker" "poweriq-worker" "management" "infoblox-worker" "aggregator" "labsdb-worker" "openmanage")
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
	sed -i -e 's/spring.couchbase.bootstrap-hosts=localhost/spring.couchbase.bootstrap-hosts=database/' $MAKEDIR/conf/$n/application.properties
	sed -i -e 's/spring.redis.host=localhost/spring.redis.host=redis/' $MAKEDIR/conf/$n/application.properties
	sed -i -e 's/apiserver.url=http:\/\/localhost/apiserver.url=http:\/\/flowgate-api/' $MAKEDIR/conf/$n/application.properties
done
cp $SOURCEDIR/flowgate-api/src/main/resources/guard.jceks $MAKEDIR/conf/flowgate-api/
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
	if ls target/$k$OUTPUTJARNAME 1> /dev/null 2>&1;then
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
	if ls target/$j$OUTPUTJARNAME 1> /dev/null 2>&1;then
		cp target/*.jar $OUTPUTJARPATH/
	else
		echo "build $j$OUTPUTJARNAME failure" >> $BUILDERRORLOG
		mvn clean package >> $BUILDERRORLOG
		continue
	fi
done
