echo Killing linux application.
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.harvester.harvesting.HarvestControllerApplication | grep -v grep | grep /home/netarkiv/test/conf/settings_HarvestControllerApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
