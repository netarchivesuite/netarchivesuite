#!/bin/bash
cd /home/test/UNITTEST/conf
if [ -e ./kill_viewerproxy_8076.sh ]; then
    ./kill_viewerproxy_8076.sh 
fi
if [ -e ./kill_indexserver.sh ]; then
    ./kill_indexserver.sh 
fi

