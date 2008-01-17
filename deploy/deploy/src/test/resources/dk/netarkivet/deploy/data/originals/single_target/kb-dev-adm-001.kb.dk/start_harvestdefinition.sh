#!/bin/bash
export CLASSPATH=/home/dev/UNITTEST/lib/dk.netarkivet.harvester.jar:/home/dev/UNITTEST/lib/dk.netarkivet.archive.jar:/home/dev/UNITTEST/lib/dk.netarkivet.viewerproxy.jar:/home/dev/UNITTEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/dev/UNITTEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/dev/UNITTEST/conf/settings.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/dev/UNITTEST/conf/log_harvestdefinitionapplication.prop -Dsettings.common.jmx.port=8100 -Dsettings.common.jmx.rmiPort=8200 -Dsettings.common.jmx.passwordFile=/home/dev/UNITTEST/conf/jmxremote.password dk.netarkivet.harvester.webinterface.HarvestDefinitionApplication < /dev/null > start_harvestdefinition.sh.log 2>&1 &
