echo START LINUX APPLICATION: HarvestControllerApplication_low
#!/bin/bash
export CLASSPATH=/home/test/TEST/lib/dk.netarkivet.harvester.jar:/home/test/TEST/lib/dk.netarkivet.archive.jar:/home/test/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/TEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/TEST/conf/settings_HarvestControllerApplication_low.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/TEST/conf/log_HarvestControllerApplication_low.prop -Djava.security.manager -Djava.security.policy=/home/test/TEST/conf/security.policy dk.netarkivet.harvester.harvesting.HarvestControllerApplication < /dev/null > start_HarvestControllerApplication_low.sh.log 2>&1 &
