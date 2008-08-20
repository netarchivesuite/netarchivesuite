#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.common.webinterface.GUIApplication | grep -v grep | grep /home/dev/UNITTEST/conf/settings.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS
fi
