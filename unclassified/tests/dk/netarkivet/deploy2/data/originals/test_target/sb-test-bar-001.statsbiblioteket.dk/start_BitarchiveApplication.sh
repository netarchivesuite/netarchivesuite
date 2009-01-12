echo START LINUX APPLICATION: BitarchiveApplication
#!/bin/bash
export CLASSPATH=/home/netarkiv/test/lib/dk.netarkivet.archive.jar:/home/netarkiv/test/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/test/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/netarkiv/test
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/netarkiv/test/conf/settings_BitarchiveApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/netarkiv/test/conf/log_BitarchiveApplication.prop -Djava.security.manager -Djava.security.policy=/home/netarkiv/test/conf/security.policy dk.netarkivet.archive.bitarchive.BitarchiveApplication < /dev/null > start_BitarchiveApplication.sh.log 2>&1 &
