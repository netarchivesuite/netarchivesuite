echo START LINUX APPLICATION: HarvestControllerApplication
#!/bin/bash
export CLASSPATH=/home/test/test/lib/dk.netarkivet.harvester.jar:/home/test/test/lib/dk.netarkivet.archive.jar:/home/test/test/lib/dk.netarkivet.viewerproxy.jar:/home/test/test/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/test
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/test/conf/settings_HarvestControllerApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/test/conf/log_HarvestControllerApplication.prop -Djava.security.manager -Djava.security.policy=/home/test/test/conf/security.policy dk.netarkivet.harvester.harvesting.HarvestControllerApplication < /dev/null > start_HarvestControllerApplication.sh.log 2>&1 &
