#!/bin/sh

# This script takes three arguments
# $1 = The NetarchiveSuite package file
# $2 = The configuration file for deployment
# $3 = The directory to deploy from
# $4 = The Heritrix3 bundle zip to use
# (1) setting up directory and variables.

if [ "$#" -ne 4 ]; then 
    echo "Usage:"
    echo "\$ bash RunNetarchiveSuite.sh NetarchiveSuite.zip deploy.xml USER\n";
    echo "\tNetarchiveSuite.zip - Release zip of NetarchiveSuite."
    echo "\tdeploy.xml - Deploy xml file, which describe the configurations of"
    echo "\t\tNetArchiveSuite."
    echo "\tUSER - RunNetArchiveSuite.sh creates a USER directory, where "
    echo "\t\tinstallation is placed.\n "
    echo "\tH3BUNDLER - The Heritrix3 bundle zip to use"
    exit 0;
fi;
echo RETRIEVING AND TESTING VARIABLES
# Retrieve command line parameters
NETARCHIVESUITE=$1
CONFIG=$2
BASEDIR=$3
H3BUNDLE=$4

# (2) create directory
echo CREATING/CLEANING DIRECTORY $BASEDIR
if [ -e $BASEDIR ]; then rm -r $BASEDIR; fi;
mkdir -p $BASEDIR

# (3) unzipping NetarchiveSuite to directory
echo UNZIPPING $NETARCHIVESUITE
unzip -q $NETARCHIVESUITE -d $BASEDIR

# (4) copy elements to directory and go there
echo COPYING FILES TO $BASEDIR
cp $NETARCHIVESUITE -d $BASEDIR
cp $H3BUNDLE -d $BASEDIR
cp $CONFIG -d $BASEDIR
cd $BASEDIR

# (3) Run deploy
echo DEPLOYING $CONFIG
java -classpath lib/netarchivesuite-deploy-core.jar dk.netarkivet.deploy.DeployApplication -C$CONFIG -Z$NETARCHIVESUITE -L./examples/logback_template.xml -S./examples/security_template.policy -O. -Eyes -B$H3BUNDLE

# (4) Make script files executable
echo MAKING FILES EXECUTABLE
chmod +x *.sh

# (5) Installing files
echo INSTALLING FILES
FILES=`ls install_*.sh`
for I in $FILES; do
  bash $I;
done
#bash install_*.sh

# (6) Starting applications
echo STARTING APPLICATIONS
FILES=`ls startall_*.sh`
for I in $FILES; do
  bash $I
done

# (7) Done
echo DONE


