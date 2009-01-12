echo START LINUX APPLICATION: HarvestControllerApplication
#!/bin/bash
export CLASSPATH=/home/netarkiv/test/lib/dk.netarkivet.harvester.jar:/home/netarkiv/test/lib/dk.netarkivet.archive.jar:/home/netarkiv/test/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/test/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/netarkiv/test
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/netarkiv/test/conf/settings_HarvestControllerApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/netarkiv/test/conf/log_HarvestControllerApplication.prop -Djava.security.manager -Djava.security.policy=/home/netarkiv/test/conf/security.policy dk.netarkivet.harvester.harvesting.HarvestControllerApplication < /dev/null > start_HarvestControllerApplication.sh.log 2>&1 &
