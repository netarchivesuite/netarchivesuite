#!/bin/bash


# Usage: ./sendHarvesterStatusMessage.sh
#                 <path to NAS installation directory : string>
#                 <application instance id : string>
#                 <job_priority [HIGHPRIORITY | LOWPRIORITY]>
#                 <available [true | false]>

NAS_HOME=$1

CLASSPATH=$CLASSPATH:$NAS_HOME/lib/dk.netarkivet.common.jar
CLASSPATH=$CLASSPATH:$NAS_HOME/lib/dk.netarkivet.harvester.jar
export CLASSPATH

DEPLOY_CMD="java -cp $CLASSPATH \
    -Ddk.netarkivet.settings.file=$NAS_HOME/conf/settings_GUIApplication.xml \
    dk.netarkivet.harvester.distribute.HarvesterStatusNotifier";

$DEPLOY_CMD $2 $3 $4;