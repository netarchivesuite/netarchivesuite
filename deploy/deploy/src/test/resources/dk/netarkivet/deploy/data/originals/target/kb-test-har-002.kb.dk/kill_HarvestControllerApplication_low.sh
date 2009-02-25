echo Killing linux application: HarvestControllerApplication_low
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.harvester.harvesting.HarvestControllerApplication | grep -v grep | grep /home/test/TEST/conf/settings_HarvestControllerApplication_low.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
PIDS=$(ps -wwfe | grep org.archive.crawler.Heritrix | grep -v grep | grep /home/test/TEST/conf/settings_HarvestControllerApplication_low.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS
fi
