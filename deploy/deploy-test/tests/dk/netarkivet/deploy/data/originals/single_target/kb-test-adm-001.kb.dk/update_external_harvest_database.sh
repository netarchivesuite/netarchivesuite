#!/bin/bash
cd /home/test/TEST
export CLASSPATH=/home/test/TEST/lib/dk.netarkivet.archive.jar:/home/test/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/TEST/lib/dk.netarkivet.monitor.jar:
java -Ddk.netarkivet.settings.file=/home/test/TEST/conf/settings_update_external_harvest_database.xml dk.netarkivet.harvester.tools.HarvestdatabaseUpdateApplication < /dev/null >> update_external_harvest_database.log 2>&1 &
