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
rm $M2_BUILD_HOME/netarchivesuite-common/src/main/java/dk/netarkivet/common
ln -s $WORKING_DIR/src/dk/netarkivet/common $M2_BUILD_HOME/netarchivesuite-common/src/main/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/common
ln -s $WORKING_DIR/tests/dk/netarkivet/common $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/testutils
ln -s $WORKING_DIR/tests/dk/netarkivet/testutils $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet

# Harvester module
rm $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/dk/netarkivet/harvester
ln -s $WORKING_DIR/src/dk/netarkivet/harvester $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-harvester/src/test/java/dk/netarkivet/harvester
ln -s $WORKING_DIR/tests/dk/netarkivet/harvester $M2_BUILD_HOME/netarchivesuite-harvester/src/test/java/dk/netarkivet
# Extra is.hi.bok.deduplicator.DigestIndexer
rm $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/is
ln -s $WORKING_DIR/src/is $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/

# Archive module
rm $M2_BUILD_HOME/netarchivesuite-archive/src/main/java/dk/netarkivet/archive
ln -s $WORKING_DIR/src/dk/netarkivet/archive $M2_BUILD_HOME/netarchivesuite-archive/src/main/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-archive/src/test/java/dk/netarkivet/archive
ln -s $WORKING_DIR/tests/dk/netarkivet/archive $M2_BUILD_HOME/netarchivesuite-archive/src/test/java/dk/netarkivet

# Monitor module
rm $M2_BUILD_HOME/netarchivesuite-monitor/src/main/java/dk/netarkivet/monitor
ln -s $WORKING_DIR/src/dk/netarkivet/monitor $M2_BUILD_HOME/netarchivesuite-monitor/src/main/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-monitor/src/test/java/dk/netarkivet/monitor
ln -s $WORKING_DIR/tests/dk/netarkivet/monitor $M2_BUILD_HOME/netarchivesuite-monitor/src/test/java/dk/netarkivet

# Viewerproxy module
rm $M2_BUILD_HOME/netarchivesuite-viewerproxy/src/main/java/dk/netarkivet/viewerproxy
ln -s $WORKING_DIR/src/dk/netarkivet/viewerproxy $M2_BUILD_HOME/netarchivesuite-viewerproxy/src/main/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-viewerproxy/src/test/java/dk/netarkivet/viewerproxy
ln -s $WORKING_DIR/tests/dk/netarkivet/viewerproxy $M2_BUILD_HOME/netarchivesuite-viewerproxy/src/test/java/dk/netarkivet

# Wayback module
rm $M2_BUILD_HOME/netarchivesuite-wayback/src/main/java/dk/netarkivet/wayback
ln -s $WORKING_DIR/src/dk/netarkivet/wayback $M2_BUILD_HOME/netarchivesuite-wayback/src/main/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-wayback/src/test/java/dk/netarkivet/wayback
ln -s $WORKING_DIR/tests/dk/netarkivet/wayback $M2_BUILD_HOME/netarchivesuite-wayback/src/test/java/dk/netarkivet

# Deploy module
rm $M2_BUILD_HOME/netarchivesuite-deploy/src/main/java/dk/netarkivet/deploy
ln -s $WORKING_DIR/src/dk/netarkivet/deploy $M2_BUILD_HOME/netarchivesuite-deploy/src/main/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet/deploy
ln -s $WORKING_DIR/tests/dk/netarkivet/deploy $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet
rm $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet/externalsoftware
ln -s $WORKING_DIR/tests/dk/netarkivet/externalsoftware $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet

## Create test resource links
rm $M2_BUILD_HOME/netarchivesuite-common/tests
ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-common/tests
rm $M2_BUILD_HOME/netarchivesuite-archive/tests
ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-archive/tests
rm $M2_BUILD_HOME/netarchivesuite-harvester/tests
ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-harvester/tests
rm $M2_BUILD_HOME/netarchivesuite-monitor/tests
ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-monitor/tests
rm $M2_BUILD_HOME/netarchivesuite-viewerproxy/tests
ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-viewerproxy/tests
rm $M2_BUILD_HOME/netarchivesuite-wayback/tests
ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-wayback/tests
rm $M2_BUILD_HOME/netarchivesuite-deploy/tests
ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-deploy/tests

cd $WORKING_DIR
