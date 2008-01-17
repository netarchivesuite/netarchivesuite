#!/bin/bash
export CLASSPATH=/home/test/UNITTEST/lib/dk.netarkivet.archive.jar:/home/test/UNITTEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/UNITTEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/UNITTEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/UNITTEST/conf/settings_bamonitor_kb.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/UNITTEST/conf/log_bitarchivemonitorapplication.prop -Dsettings.common.jmx.port=8102 -Dsettings.common.jmx.rmiPort=8202 -Dsettings.common.jmx.passwordFile=/home/test/UNITTEST/conf/jmxremote.password dk.netarkivet.archive.bitarchive.BitarchiveMonitorApplication < /dev/null > start_bitarchive_monitor_kb.sh.log 2>&1 &
