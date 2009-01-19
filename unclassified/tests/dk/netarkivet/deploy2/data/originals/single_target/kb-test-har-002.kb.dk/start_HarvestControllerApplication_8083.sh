echo Starting linux application.
cd /home/dev/TEST
PIDS=$(ps -wwfe | grep dk.netarkivet.harvester.harvesting.HarvestControllerApplication | grep -v grep | grep /home/dev/TEST/conf/settings_HarvestControllerApplication_8083.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    echo Application already running.
else
    export CLASSPATH=/home/dev/TEST/lib/dk.netarkivet.harvester.jar:/home/dev/TEST/lib/dk.netarkivet.archive.jar:/home/dev/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/dev/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
    java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/dev/TEST/conf/settings_HarvestControllerApplication_8083.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/dev/TEST/conf/log_HarvestControllerApplication_8083.prop -Djava.security.manager -Djava.security.policy=/home/dev/TEST/conf/security.policy dk.netarkivet.harvester.harvesting.HarvestControllerApplication < /dev/null > start_HarvestControllerApplication_8083.sh.log 2>&1 &
fi
