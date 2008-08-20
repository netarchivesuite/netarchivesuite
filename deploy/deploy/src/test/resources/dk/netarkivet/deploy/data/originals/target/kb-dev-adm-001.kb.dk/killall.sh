#!/bin/bash
cd /home/test/UNITTEST/conf
if [ -e ./kill_guiapplication.sh ]; then
    ./kill_guiapplication.sh 
fi
if [ -e ./kill_arcrepository.sh ]; then
    ./kill_arcrepository.sh 
fi
if [ -e ./kill_bitarchive_monitor_kb.sh ]; then
    ./kill_bitarchive_monitor_kb.sh 
fi
if [ -e ./kill_bitarchive_monitor_sb.sh ]; then
    ./kill_bitarchive_monitor_sb.sh 
fi

