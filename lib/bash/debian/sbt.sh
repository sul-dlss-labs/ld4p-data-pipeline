#!/bin/bash

if which sbt > /dev/null; then
    echo "Found sbt installation"
else
    echo "sbt installation"
    if [ ! -f /etc/apt/sources.list.d/sbt.list ]; then
        echo "deb https://dl.bintray.com/sbt/debian /" > /etc/apt/sources.list.d/sbt.list
        apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
        apt-get -y -qq update
    fi
    apt-get install -y -qq sbt
fi
