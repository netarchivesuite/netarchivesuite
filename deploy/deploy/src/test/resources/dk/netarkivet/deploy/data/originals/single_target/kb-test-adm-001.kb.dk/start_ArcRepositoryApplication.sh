echo Starting linux application: ArcRepositoryApplication
cd /home/test/TEST
PIDS=$(ps -wwfe | grep dk.netarkivet.archive.arcrepository.ArcRepositoryApplication | grep -v grep | grep /home/test/TEST/conf/settings_ArcRepositoryApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    echo Application already running.
else
    export CLASSPATH=/home/test/TEST/lib/dk.netarkivet.archive.jar:/home/test/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
    java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/TEST/conf/settings_ArcRepositoryApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/TEST/conf/log_ArcRepositoryApplication.prop -Djava.security.manager -Djava.security.policy=/home/test/TEST/conf/security.policy dk.netarkivet.archive.arcrepository.ArcRepositoryApplication < /dev/null > start_ArcRepositoryApplication.log 2>&1 &
fi
