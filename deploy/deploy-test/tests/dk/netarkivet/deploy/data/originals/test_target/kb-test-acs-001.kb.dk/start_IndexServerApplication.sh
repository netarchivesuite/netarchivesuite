echo Starting linux application: IndexServerApplication
cd /home/test/test
PIDS=$(ps -wwfe | grep dk.netarkivet.archive.indexserver.IndexServerApplication | grep -v grep | grep /home/test/test/conf/settings_IndexServerApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    echo Application already running.
else
    export CLASSPATH=/home/test/test/lib/dk.netarkivet.archive.jar:/home/test/test/lib/dk.netarkivet.viewerproxy.jar:/home/test/test/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
    java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/test/conf/settings_IndexServerApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/test/conf/log_IndexServerApplication.prop -Djava.security.manager -Djava.security.policy=/home/test/test/conf/security.policy dk.netarkivet.archive.indexserver.IndexServerApplication < /dev/null > start_IndexServerApplication.log 2>&1 &
fi
