echo Killing linux application: BitarchiveMonitorApplication_SBBM
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.archive.bitarchive.BitarchiveMonitorApplication | grep -v grep | grep /home/test/test/conf/settings_BitarchiveMonitorApplication_SBBM.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
