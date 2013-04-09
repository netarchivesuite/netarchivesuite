#!/bin/sh

# Maps the current NetarchiveSuite directories to a standard Maven2 directory structure located in the m2-build folder. This is part of the efforts to 
# validate that it is possible to convert the NetarchiveSuite project to a Maven 2 build.The scipt creates the symbolic links to the current non-maven 
# structured source files in the this folder so the structure in the M2-build folder reflects a multiproject M2 compliant structure.
# See 

PROJECT_HOME=`dirname $0`
WORKING_DIR=$PWD
M2_BUILD_HOME=$PROJECT_HOME/m2-build

do_create() {
cd $PROJECT_HOME
# Common module
ln -s $WORKING_DIR/src/dk/netarkivet/common $M2_BUILD_HOME/netarchivesuite-common/src/main/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/common $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/testutils $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet

# Harvester module
ln -s $WORKING_DIR/src/dk/netarkivet/harvester $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/harvester $M2_BUILD_HOME/netarchivesuite-harvester/src/test/java/dk/netarkivet
# Extra is.hi.bok.deduplicator.DigestIndexer
ln -s $WORKING_DIR/src/is $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/
ln -s $WORKING_DIR/src/dk/netarkivet/viewerproxy $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/viewerproxy $M2_BUILD_HOME/netarchivesuite-harvester/src/test/java/dk/netarkivet

ln -s $WORKING_DIR/webpages $M2_BUILD_HOME/netarchivesuite-harvester/src/main/webapp/

# Archive module
ln -s $WORKING_DIR/src/dk/netarkivet/archive $M2_BUILD_HOME/netarchivesuite-archive/src/main/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/archive $M2_BUILD_HOME/netarchivesuite-archive/src/test/java/dk/netarkivet

# Monitor module
ln -s $WORKING_DIR/src/dk/netarkivet/monitor $M2_BUILD_HOME/netarchivesuite-monitor/src/main/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/monitor $M2_BUILD_HOME/netarchivesuite-monitor/src/test/java/dk/netarkivet

# Wayback module
ln -s $WORKING_DIR/src/dk/netarkivet/wayback $M2_BUILD_HOME/netarchivesuite-wayback/src/main/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/wayback $M2_BUILD_HOME/netarchivesuite-wayback/src/test/java/dk/netarkivet

# Deploy module
ln -s $WORKING_DIR/src/dk/netarkivet/deploy $M2_BUILD_HOME/netarchivesuite-deploy/src/main/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/deploy $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet
ln -s $WORKING_DIR/tests/dk/netarkivet/externalsoftware $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet

## Create test resource links (disabled, recursive linking)
#ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-common/tests
#ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-archive/tests
#ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-harvester/tests
#ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-monitor/tests
#ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-wayback/tests
#ln -s $WORKING_DIR/tests $M2_BUILD_HOME/netarchivesuite-deploy/tests

cd $WORKING_DIR
}

do_clean() {
cd $PROJECT_HOME
# Common module
rm $M2_BUILD_HOME/netarchivesuite-common/src/main/java/dk/netarkivet/common
rm $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/common
rm $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/testutils

# Harvester module
rm $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/dk/netarkivet/harvester
rm $M2_BUILD_HOME/netarchivesuite-harvester/src/test/java/dk/netarkivet/harvester
# Extra is.hi.bok.deduplicator.DigestIndexer
rm $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/is
rm $M2_BUILD_HOME/netarchivesuite-harvester/src/main/java/dk/netarkivet/viewerproxy
rm $M2_BUILD_HOME/netarchivesuite-harvester/src/test/java/dk/netarkivet/viewerproxy

# Archive module
rm $M2_BUILD_HOME/netarchivesuite-archive/src/main/java/dk/netarkivet/archive
rm $M2_BUILD_HOME/netarchivesuite-archive/src/test/java/dk/netarkivet/archive

# Monitor module
rm $M2_BUILD_HOME/netarchivesuite-monitor/src/main/java/dk/netarkivet/monitor
rm $M2_BUILD_HOME/netarchivesuite-monitor/src/test/java/dk/netarkivet/monitor

# Wayback module
rm $M2_BUILD_HOME/netarchivesuite-wayback/src/main/java/dk/netarkivet/wayback
rm $M2_BUILD_HOME/netarchivesuite-wayback/src/test/java/dk/netarkivet/wayback

# Deploy module
rm $M2_BUILD_HOME/netarchivesuite-deploy/src/main/java/dk/netarkivet/deploy
rm $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet/deploy
rm $M2_BUILD_HOME/netarchivesuite-deploy/src/test/java/dk/netarkivet/externalsoftware

## Test resource links
rm $M2_BUILD_HOME/netarchivesuite-common/tests
rm $M2_BUILD_HOME/netarchivesuite-archive/tests
rm $M2_BUILD_HOME/netarchivesuite-harvester/tests
rm $M2_BUILD_HOME/netarchivesuite-monitor/tests
rm $M2_BUILD_HOME/netarchivesuite-wayback/tests
rm $M2_BUILD_HOME/netarchivesuite-deploy/tests

cd $WORKING_DIR
}

case "$1" in
  create)
	do_create
	;;
  clean)
	do_clean
	;;
  cleancreate)
	do_clean
	do_create
	;;
  *)
	echo "Usage: $SCRIPTNAME {create|clean|cleancreate}"
	>&2
	exit 4
	;;
esac
