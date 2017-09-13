#!/bin/bash

# http://www.scala-sbt.org/release/docs/Installing-sbt-on-Linux.html

if which sbt > /dev/null; then
    echo "Found sbt installation"
else
    echo "sbt installation"
    if [ ! -f /etc/yum.repos.d/bintray-sbt-rpm.repo ]; then
        curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo
    fi
    sudo yum install -y sbt
fi
