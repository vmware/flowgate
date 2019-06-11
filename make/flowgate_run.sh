#!/usr/bin/bash
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#

#####define all path
CURRENTPATH=`pwd`
DOCKERMAVENBUILD=$CURRENTPATH/maven-docker-build
DOCKERCOMPOSERUNFILE=$DOCKERMAVENBUILD/docker-compose.run.images.yml

docker-compose -f $DOCKERCOMPOSERUNFILE up -d
