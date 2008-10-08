#!/bin/bash
cd /home/dev/UNITTEST/conf
if [ -e ./start_gui.sh ]; then
    ./start_gui.sh 
fi
if [ -e ./start_arcrepository.sh ]; then
    ./start_arcrepository.sh 
fi
if [ -e ./start_bitarchive_monitor_kb.sh ]; then
    ./start_bitarchive_monitor_kb.sh 
fi

