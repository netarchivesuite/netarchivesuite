echo START LINUX APPLICATION: GUIApplication
#!/bin/bash
export CLASSPATH=/home/test/test/lib/dk.netarkivet.harvester.jar:/home/test/test/lib/dk.netarkivet.archive.jar:/home/test/test/lib/dk.netarkivet.viewerproxy.jar:/home/test/test/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/test
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/test/conf/settings_GUIApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/test/conf/log_GUIApplication.prop -Djava.security.manager -Djava.security.policy=/home/test/test/conf/security.policy dk.netarkivet.common.webinterface.GUIApplication < /dev/null > start_GUIApplication.sh.log 2>&1 &
