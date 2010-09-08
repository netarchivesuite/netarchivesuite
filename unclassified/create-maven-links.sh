#!/bin/sh

# Maps the current NetarchiveSuite directories to a standard Maven2 directory structure located in the m2-build folder. This is part of the efforts to 
# validate that it is possible to convert the NetarchiveSuite project to a Maven 2 build.The scipt creates the symbolic links to the current non-maven 
# structured source files in the this folder so the structure in the M2-build folder reflects a multiproject M2 compliant structure.
# See 

PROJECT_HOME=`dirname $0`
WORKING_DIR=$PWD	

cd $PROJECT_HOME
		
M2_BUILD_HOME=./m2-build

# Common module
rm $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/testutils
ln -s $PWD/tests/dk/netarkivet/testutils $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/

rm $M2_BUILD_HOME/netarchivesuite-common/src/main/java/dk/netarkivet/common
ln -s $PWD/src/dk/netarkivet/common $M2_BUILD_HOME/netarchivesuite-common/src/main/java/dk/netarkivet/
rm $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/common
ln -s $PWD/tests/dk/netarkivet/common $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/

# Archive module
ln -s $PWD/src/dk/netarkivet/archive $M2_BUILD_HOME/netarchivesuite-archive/src/main/java/dk/netarkivet/
ln -s $PWD/tests/dk/netarkivet/archive $M2_BUILD_HOME/netarchivesuite-archive/src/test/java/dk/netarkivet/

# Harvester module
ln -s $PWD/src/dk/netarkivet/harvester $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/dk/netarkivet/
ln -s $PWD/tests/dk/netarkivet/harvester $M2_BUILD_HOME/netarchivesuite-harvester/src/test/java/dk/netarkivet/

# Monitor module
ln -s $PWD/src/dk/netarkivet/monitor $M2_BUILD_HOME/netarchivesuite-monitor/src/main/java/dk/netarkivet/
ln -s $PWD/tests/dk/netarkivet/monitor $M2_BUILD_HOME/netarchivesuite-monitor/src/test/java/dk/netarkivet/

# Deploy module
ln -s $PWD/src/dk/netarkivet/deploy $M2_BUILD_HOME/netarchivesuite-deploy/src/main/java/dk/netarkivet/
ln -s $PWD/tests/dk/netarkivet/deploy $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet/

# Viewerproxy module
ln -s $PWD/src/dk/netarkivet/viewerproxy $M2_BUILD_HOME/netarchivesuite-viewerproxy/src/main/java/dk/netarkivet/
ln -s $PWD/tests/dk/netarkivet/viewerproxy $M2_BUILD_HOME/netarchivesuite-viewerproxy/src/test/java/dk/netarkivet/

# Wayback module
ln -s $PWD/src/dk/netarkivet/wayback $M2_BUILD_HOME/netarchivesuite-wayback/src/main/java/dk/netarkivet/
ln -s $PWD/tests/dk/netarkivet/wayback $M2_BUILD_HOME/netarchivesuite-wayback/src/test/java/dk/netarkivet/

cd $WORKING_DIR
