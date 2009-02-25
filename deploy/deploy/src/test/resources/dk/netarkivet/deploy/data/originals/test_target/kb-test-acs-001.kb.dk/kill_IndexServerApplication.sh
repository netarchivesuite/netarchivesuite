echo Killing linux application: IndexServerApplication
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.archive.indexserver.IndexServerApplication | grep -v grep | grep /home/test/test/conf/settings_IndexServerApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
