#!/bin/bash
cd /home/dev/UNITTEST/conf
if [ -e ./kill_viewerproxy_8081.sh ]; then
    ./kill_viewerproxy_8081.sh 
fi
if [ -e ./kill_viewerproxy_8082.sh ]; then
    ./kill_viewerproxy_8082.sh 
fi
if [ -e ./kill_indexserver.sh ]; then
    ./kill_indexserver.sh 
fi

