#!/bin/bash

BUILDDIR=/flowgate-build
BUILDLOG=/log/flowgate-build-log.txt

if [ -f "$BUILDLOG" ];then
	rm -f $BUILDLOG
fi

cd $BUILDDIR/ui/
npm install --unsafe-perm
npm config set unsafe-perm true
npm install -g @angular/cli@10.2.0
ng build --prod >> $BUILDLOG