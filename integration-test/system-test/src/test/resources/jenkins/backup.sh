#!/usr/bin/env bash

user=$1
password=$2

pushd ${BASH_SOURCE%/*} > /dev/null
if [[ -s settings.conf ]]; then
    source settings.conf
fi


for build in $BUILDS; do
    url=https://sbforge.org/jenkins/view/NetarchiveSuite/job/${build}/config.xml
    curl -n $url > ${build}.xml
done