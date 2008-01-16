#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.harvester.webinterface.HarvestDefinitionApplication | grep -v grep | grep /home/test/UNITTEST/conf/settings.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS
fi
