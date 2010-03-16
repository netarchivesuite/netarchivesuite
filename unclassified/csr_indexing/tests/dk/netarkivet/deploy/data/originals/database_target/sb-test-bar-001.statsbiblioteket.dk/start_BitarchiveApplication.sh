echo Starting linux application: BitarchiveApplication
cd /home/netarkiv/TEST
PIDS=$(ps -wwfe | grep dk.netarkivet.archive.bitarchive.BitarchiveApplication | grep -v grep | grep /home/netarkiv/TEST/conf/settings_BitarchiveApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    echo Application already running.
else
    export CLASSPATH=/home/netarkiv/TEST/lib/dk.netarkivet.archive.jar:/home/netarkiv/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
    java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/netarkiv/TEST/conf/settings_BitarchiveApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/netarkiv/TEST/conf/log_BitarchiveApplication.prop -Djava.security.manager -Djava.security.policy=/home/netarkiv/TEST/conf/security.policy dk.netarkivet.archive.bitarchive.BitarchiveApplication < /dev/null > start_BitarchiveApplication.log 2>&1 &
fi
