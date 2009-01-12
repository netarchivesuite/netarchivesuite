echo START LINUX APPLICATION: GUIApplication
#!/bin/bash
export CLASSPATH=/home/test/TEST/lib/dk.netarkivet.harvester.jar:/home/test/TEST/lib/dk.netarkivet.archive.jar:/home/test/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/TEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/TEST/conf/settings_GUIApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/TEST/conf/log_GUIApplication.prop -Djava.security.manager -Djava.security.policy=/home/test/TEST/conf/security.policy dk.netarkivet.common.webinterface.GUIApplication < /dev/null > start_GUIApplication.sh.log 2>&1 &
