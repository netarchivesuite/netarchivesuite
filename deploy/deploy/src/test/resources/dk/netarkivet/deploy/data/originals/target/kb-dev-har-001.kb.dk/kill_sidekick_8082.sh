#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.harvester.sidekick.SideKick | grep -v grep | grep /home/test/UNITTEST/conf/settings_harvester_8082.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS
fi
