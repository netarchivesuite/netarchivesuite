#!/bin/bash
cd /home/dev/UNITTEST/conf
if [ -e ./kill_harvestdefinition.sh ]; then
    ./kill_harvestdefinition.sh 
fi
if [ -e ./kill_arcrepository.sh ]; then
    ./kill_arcrepository.sh 
fi
if [ -e ./kill_bitarchive_monitor_kb.sh ]; then
    ./kill_bitarchive_monitor_kb.sh 
fi

