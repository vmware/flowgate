#!/bin/sh
cp /conf/application.properties /
cp /conf/guard.jceks /jar
java -Xbootclasspath/a:/jar -jar /jar/flowgate-api.jar