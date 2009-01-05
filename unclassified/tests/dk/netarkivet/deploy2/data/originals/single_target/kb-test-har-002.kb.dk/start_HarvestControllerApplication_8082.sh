echo START LINUX APPLICATION: HarvestControllerApplication_8082
#!/bin/bash
export CLASSPATH=/home/dev/TEST/lib/dk.netarkivet.harvester.jar:/home/dev/TEST/lib/dk.netarkivet.archive.jar:/home/dev/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/dev/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/dev/TEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/dev/TEST/conf/settings_HarvestControllerApplication_8082.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/dev/TEST/conf/log_HarvestControllerApplication_8082.prop -Djava.security.manager -Djava.security.policy=/home/dev/TEST/conf/security.policy dk.netarkivet.harvester.harvesting.HarvestControllerApplication < /dev/null > start_HarvestControllerApplication_8082.sh.log 2>&1 &
