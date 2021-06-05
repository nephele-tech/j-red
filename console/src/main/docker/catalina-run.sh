#!/bin/sh

# start docker
service docker start

# start tomcat
./bin/catalina.sh run
exit $?