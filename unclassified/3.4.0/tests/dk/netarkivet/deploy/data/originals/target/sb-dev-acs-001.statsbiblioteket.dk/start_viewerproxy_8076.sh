#!/bin/bash
export CLASSPATH=/home/netarkiv/UNITTEST/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/UNITTEST/lib/dk.netarkivet.archive.jar:/home/netarkiv/UNITTEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/netarkiv/UNITTEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/netarkiv/UNITTEST/conf/settings_viewerproxy_8076.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/netarkiv/UNITTEST/conf/log_viewerproxyapplication.prop -Dsettings.common.jmx.port=8100 -Dsettings.common.jmx.rmiPort=8200 -Dsettings.common.jmx.passwordFile=/home/netarkiv/UNITTEST/conf/jmxremote.password dk.netarkivet.viewerproxy.ViewerProxyApplication < /dev/null > start_viewerproxy_8076.sh.log 2>&1 &
