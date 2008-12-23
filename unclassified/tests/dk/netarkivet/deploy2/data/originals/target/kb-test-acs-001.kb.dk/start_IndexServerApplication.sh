echo START LINUX APPLICATION: IndexServerApplication
#!/bin/bash
export CLASSPATH=/home/test/TEST/lib/dk.netarkivet.archive.jar:/home/test/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/TEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/TEST/conf/settings_IndexServerApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/TEST/conf/log_IndexServerApplication.prop -Djava.security.manager -Djava.security.policy=/home/test/TEST/conf/security.policy dk.netarkivet.archive.indexserver.IndexServerApplication < /dev/null > start_IndexServerApplication.sh.log 2>&1 &
