echo Killing linux application:HarvestControllerApplication_high
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.harvester.harvesting.HarvestControllerApplication | grep -v grep | grep /home/test/TEST/conf/settings_HarvestControllerApplication_high.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
