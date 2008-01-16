#!/bin/bash
export CLASSPATH=/home/test/UNITTEST/lib/dk.netarkivet.archive.jar:/home/test/UNITTEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/UNITTEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/UNITTEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/UNITTEST/conf/settings.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/UNITTEST/conf/log_arcrepositoryapplication.prop -Dsettings.common.jmx.port=8101 -Dsettings.common.jmx.rmiPort=8201 -Dsettings.common.jmx.passwordFile=/home/test/UNITTEST/conf/jmxremote.password dk.netarkivet.archive.arcrepository.ArcRepositoryApplication < /dev/null > start_arcrepository.sh.log 2>&1 &
