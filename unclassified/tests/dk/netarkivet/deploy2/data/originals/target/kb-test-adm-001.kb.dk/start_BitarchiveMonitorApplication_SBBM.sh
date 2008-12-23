echo START LINUX APPLICATION: BitarchiveMonitorApplication_SBBM
#!/bin/bash
export CLASSPATH=/home/test/TEST/lib/dk.netarkivet.archive.jar:/home/test/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/TEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/TEST/conf/settings_BitarchiveMonitorApplication_SBBM.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/TEST/conf/log_BitarchiveMonitorApplication_SBBM.prop -Djava.security.manager -Djava.security.policy=/home/test/TEST/conf/security.policy dk.netarkivet.archive.bitarchive.BitarchiveMonitorApplication < /dev/null > start_BitarchiveMonitorApplication_SBBM.sh.log 2>&1 &
