#!/bin/bash
cd /home/test/test
export CLASSPATH=/home/test/test/lib/dk.netarkivet.archive.jar:/home/test/test/lib/dk.netarkivet.viewerproxy.jar:/home/test/test/lib/dk.netarkivet.monitor.jar:
java -Ddk.netarkivet.settings.file=/home/test/test/conf/settings_update_external_harvest_database.xml dk.netarkivet.harvester.tools.HarvestdatabaseUpdateApplication < /dev/null >> update_external_harvest_database.log 2>&1 &
