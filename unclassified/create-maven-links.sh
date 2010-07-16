#!/bin/sh

# Maps the current NetarchiveSuite directories to a standard Maven2 directory structure located in the m2-build folder. This is part of the efforts to 
# validate that it is possible to convert the NetarchiveSuite project to a Maven 2 build.The scipt creates the symbolic links to the current non-maven 
# structured source files in the this folder so the structure in the M2-build folder reflects a multiproject M2 compliant structure.
# See 

PROJECT_HOME=`dirname $0`
WORKING_DIR=$PWD	

cd $PROJECT_HOME
		
M2_BUILD_HOME=./m2-build

ln -s $PWD/tests/dk/netarkivet/testutils $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/

ln -s $PWD/src/dk/netarkivet/common $M2_BUILD_HOME/netarchivesuite-common/src/main/java/dk/netarkivet/
ln -s $PWD/tests/dk/netarkivet/common $M2_BUILD_HOME/netarchivesuite-common/src/test/java/dk/netarkivet/

cd $WORKING_DIR
