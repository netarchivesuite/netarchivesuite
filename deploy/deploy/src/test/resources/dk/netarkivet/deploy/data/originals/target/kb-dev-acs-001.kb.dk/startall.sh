#!/bin/bash
cd /home/test/UNITTEST/conf
if [ -e ./start_viewerproxy_8076.sh ]; then
    ./start_viewerproxy_8076.sh 
fi
if [ -e ./start_indexserver.sh ]; then
    ./start_indexserver.sh 
fi

