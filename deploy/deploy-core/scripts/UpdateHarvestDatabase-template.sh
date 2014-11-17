#!/bin/bash
export INSTALLDIR=/home/test/QUICKSTART
cd $INSTALLDIR
export CLASSPATH=$INSTALLDIR/lib/dk.netarkivet.harvester.jar:$INSTALLDIR/lib/dk.netarkivet.archive.jar:$INSTALLDIR/lib/dk.netarkivet.viewerproxy.jar:$INSTALLDIR/lib/dk.netarkivet.monitor.jar:$INSTALLDIR/lib/dk.netarkivet.wayback.jar
java -Ddk.netarkivet.settings.file=$INSTALLDIR/conf/settings_GUIApplication.xml dk.netarkivet.harvester.tools.HarvestdatabaseUpdateApplication
