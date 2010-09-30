echo Killing linux application: BitarchiveMonitorApplication_KBBM
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.archive.bitarchive.BitarchiveMonitorApplication | grep -v grep | grep /home/test/TEST/conf/settings_BitarchiveMonitorApplication_KBBM.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
