#!/bin/bash
export INSTALLDIR=/home/test/QUICKSTART
cd $INSTALLDIR
export CLASSPATH=$INSTALLDIR/lib/netarchivesuite-harvester-core.jar:$INSTALLDIR/lib/netarchivesuite-archive-core.jar:$INSTALLDIR/lib/netarchivesuite-dk.netarkivet.monitor.jar:$INSTALLDIR/lib/netarchivesuite-wayback-indexer.jar

java -Ddk.netarkivet.settings.file=$INSTALLDIR/conf/settings_GUIApplication.xml dk.netarkivet.harvester.tools.HarvestdatabaseUpdateApplication

