#!/bin/bash
cd /home/test/UNITTEST/conf
if [ -e ./start_harvestdefinition.sh ]; then
    ./start_harvestdefinition.sh 
fi
if [ -e ./start_arcrepository.sh ]; then
    ./start_arcrepository.sh 
fi
if [ -e ./start_bitarchive_monitor_kb.sh ]; then
    ./start_bitarchive_monitor_kb.sh 
fi
if [ -e ./start_bitarchive_monitor_sb.sh ]; then
    ./start_bitarchive_monitor_sb.sh 
fi

