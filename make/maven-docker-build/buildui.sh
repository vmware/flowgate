#!/bin/bash

BUILDDIR=/flowgate-build
BUILDLOG=/log/flowgate-build-log.txt

if [ -f "$BUILDLOG" ];then
	rm -f $BUILDLOG
fi

cd $BUILDDIR/ui/
npm install --unsafe-perm
npm config set unsafe-perm true
npm install -g @angular/cli@1.6.1
ng build --prod -e prod -aot=false >> $BUILDLOG