#!/bin/bash
cd /home/dev/UNITTEST/conf
if [ -e ./start_viewerproxy_8081.sh ]; then
    ./start_viewerproxy_8081.sh 
fi
if [ -e ./start_viewerproxy_8082.sh ]; then
    ./start_viewerproxy_8082.sh 
fi
if [ -e ./start_indexserver.sh ]; then
    ./start_indexserver.sh 
fi

