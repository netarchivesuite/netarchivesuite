#!/bin/bash

## $Id$
## $Revision$
## $Date$
## $Author$
##
## The Netarchive Suite - Software to harvest and preserve websites
## Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
##
## This library is free software; you can redistribute it and/or
## modify it under the terms of the GNU Lesser General Public
## License as published by the Free Software Foundation; either
## version 2.1 of the License, or (at your option) any later version.
##
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
## Lesser General Public License for more details.
##
## You should have received a copy of the GNU Lesser General Public
## License along with this library; if not, write to the Free Software
## Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
##


## This script can be used to start up a clean "local" functioning
## NetarchiveSuite with all applications running on the local machine. The only
## configuration which should be necessary is setting the paths to the JMS
## broker, JRE, and NetarchiveSuite project below.

## Path for Java 1.6.0_07 or higher, can be overridden by $JAVA or $JAVA_HOME
JAVA=${JAVA:=${JAVA_HOME:=/usr/java}}

## Path for the JMS broker, can be overriden by $IMQ
IMQ=${IMQ:=/opt/sun/mq/bin/imqbrokerd}

## Path for the Java executable, can be overridden by $JAVA_CMD
JAVA_CMD=${JAVA_CMD:=$JAVA_HOME/bin/java}

## ------------ The following settings normally work --------------

## The home directory for this local NetarchiveSuite relative to base dir
ARCREP_HOME=${ARCREP_HOME:=$( cd $( dirname $0 ) && pwd )}

## Path for the NetarchiveSuite base dir, can be overridden by $NETARCHIVEDIR,
## but that's usually not necessary.
NETARCHIVEDIR=${NETARCHIVEDIR:=$( cd $ARCREP_HOME/../.. && pwd )}

## How far to offset the xterms horizontally.  If you make the xterms smaller,
## change this parameter correspondingly
XTERM_HOFFSET=${XTERM_HOFFSET:=550}
## How far to offset the xterms vertically.  If you make the xterms smaller,
## change this parameter correspondingly
XTERM_VOFFSET=${XTERM_VOFFSET:=350}

## The command used to start the xterms.  By defauly, extra save lines are
## set.  If you want the windows to be smaller, you may be able to use -fs
## to change font size, if your xterm is compiled with freetype.  Otherwise,
## you might be able to use another implementation, as long as the -geometry,
## -title, -e, and -hold arguments are supported.
XTERM_CMD=${XTERM_CMD:='xterm -sl 1000'}

## ---------- No changes should be required below this line ---------------

## The below ports are only used internally on the same machine, but you
## should make sure nothing else is using them.

## Initial JMXPORT, must be ++'d after each application started, incl. SideKick
## Ports 8100-8110 are used
export JMXPORT=8100

## Initial HTTPPORT, must be ++'d after each application started
## Ports 8070-8078 are used
export HTTPPORT=8070

## Initial FILETRANSFERPORT, must be ++'d after each application started
## Ports 8040-8048 are used
export FILETRANSFERPORT=8040

## Initial Heritrix GUI port (with JMX port one higher), must be += 2'ed after
## each application started.
## Ports 8090-8093 are used.
export HERITRIXPORT=8090

## Set $KEEPDATA to non-empty to avoid cleaning data at the start of each run.
## This will make the startup process more complex, but will allow you to reuse
## what you did last time.

## Whether or not to use -hold argument
## Set $HOLD to the empty string to have windows automatically close when
## the process dies.
if [ -z "${!HOLD*}" ]; then
    HOLD=-hold
fi

## ----------- No interesting information below this line --------------

# Utility functions
function makeCommonOptions {
    echo "-Dsettings.common.jmx.port=$JMXPORT \
      -Dsettings.common.jmx.rmiPort=$(( $JMXPORT + 100 )) \
      -Dsettings.common.jmx.passwordFile=$ARCREP_HOME/quickstart.jmxremote.password \
      -Dsettings.common.jmx.accessFile=$ARCREP_HOME/quickstart.jmxremote.access \
      -Dcom.sun.management.jmxremote \
      -Ddk.netarkivet.quickstart.basedir=$NETARCHIVEDIR \
      -Djava.security.manager \
      -Djava.security.policy=$ARCREP_HOME/quickstart.security.policy";
}

function makeXtermOffset {
    echo +$(( $XTERM_HOFFSET * ($WINDOWPOS / 3) ))+$(( $XTERM_VOFFSET * ($WINDOWPOS % 3) ));
}

# Start a "normal" application (non-harvest).
# Arg 1 is the XTerm's title
# Arg 2 is the class name
# Arg 3 is any other args that need to be passed to Java.
function startApp {
    local title app termgeom otherargs;
    title=$1;
    app=$2;
    otherargs=$3;
    termgeom=$TERMSIZE`makeXtermOffset`;
    logpropfile="$ARCREP_HOME/log/log.prop.$title-$HTTPPORT"
    sed 's!\(java.util.logging.FileHandler.pattern=\).*!\1'$ARCREP_HOME'/log/'$title'-'$HTTPPORT'.log!;' <$ARCREP_HOME/log.prop > "$logpropfile"
    $XTERM_CMD $HOLD -geometry $termgeom -title "$title" -e $JAVA_CMD \
        -Djava.util.logging.config.file="$logpropfile" \
        $JVM_ARGS `makeCommonOptions` -Dsettings.common.http.port=$HTTPPORT \
        -Dsettings.common.remoteFile.port=$FILETRANSFERPORT \
        -classpath $CLASSPATH $otherargs dk.netarkivet.$app &
    WINDOWPOS=$(( $WINDOWPOS + 1 ));
    JMXPORT=$(( $JMXPORT + 1 ));
    HTTPPORT=$(( $HTTPPORT + 1 ));
    FILETRANSFERPORT=$(( $FILETRANSFERPORT + 1 ))
}

## Start HarvestController
## The HarvestControllerServer takes some specific parameters
# Arg 1 is the consecutive number of this HarvestController
# Arg 2 is the priority (HIGH or LOW, corresponding to selective or snapshot)
function startHarvestApp {
    local hcsid priority prioritysetting portsetting runsetting title;
    local dirsetting hcstart startscript scriptfile;
    hcsid=$1;
    priority=$2;
    priority_settings=-Dsettings.harvester.harvesting.harvestControllerPriority=${priority}PRIORITY;
    portsetting="-Dsettings.harvester.harvesting.queuePriority=${priority}PRIORITY \
             -Dsettings.common.applicationInstanceId=$2 \
             -Dsettings.common.http.port=$HTTPPORT \
             -Dsettings.harvester.harvesting.heritrix.guiPort=$HERITRIXPORT \
             -Dsettings.harvester.harvesting.heritrix.jmxPort=$(( $HERITRIXPORT + 1 ))";
    title="Harvest Controller (Priority ${priority})";
    logpropfile="$ARCREP_HOME/log/log.prop.HarvestController-$HTTPPORT"
    sed 's!\(java.util.logging.FileHandler.pattern=\).*!\1'$ARCREP_HOME'/log/HarvestController-'$HTTPPORT'.log!;' <$ARCREP_HOME/log.prop > "$logpropfile"
    dirsetting="-Dsettings.harvester.harvesting.serverDir=server$hcsid \
      -Dsettings.harvester.harvesting.oldjobsDir=oldjobs$hcsid";

    hcstart="$JAVA_CMD `makeCommonOptions` $JVM_ARGS -classpath $CLASSPATH \
      -Djava.util.logging.config.file=\"$logpropfile\" \
      $prioritysetting $portsetting $dirsetting \
      dk.netarkivet.harvester.harvesting.HarvestControllerApplication";
    scriptfile=./hcs${hcsid}.sh;
    startscript="$XTERM_CMD $XTERM_ARGS -geometry $TERM_SIZE`makeXtermOffset` -title \"$title\" \
      -e $hcstart &";
    echo "$startscript" > $scriptfile;
    chmod 755 $scriptfile;
    $scriptfile;
    JMXPORT=$(( $JMXPORT + 1 ));
    HERITRIXPORT=$(( $HERITRIXPORT + 2 ));
    WINDOWPOS=$(( $WINDOWPOS + 1 ));
    HTTPPORT=$(( $HTTPPORT + 1 ));
}

## Clean up and copy harvest definition data

## Remove and recopy various settings etc.
## Not done if KEEPDATA is set and we have already run at least once.
if [ -z "$KEEPDATA" -o ! -e $ARCREP_HOME/data/working ]; then
    rm -rf $ARCREP_HOME/data/working
    cp -r $ARCREP_HOME/data/originals $ARCREP_HOME/data/working
    mkdir -p $ARCREP_HOME/lib
    cp -r $NETARCHIVEDIR/lib/heritrix $ARCREP_HOME/lib
    
    ## Remove and unzip the embedded database
    rm -rf $ARCREP_HOME/data/working/harvestdefinitionbasedir/fullhddb
    unzip -qou -d $ARCREP_HOME/data/working/harvestdefinitionbasedir/ \
	$NETARCHIVEDIR/harvestdefinitionbasedir/fullhddb.jar

    ## Clean up other stuff left behind

    ## Clean up old logs
    rm -rf $ARCREP_HOME/log/* $ARCREP_HOME/derby.log

    ## Clean up everything else left behind by old harvests
    rm -rf $ARCREP_HOME/admin.data $ARCREP_HOME/server* \
        $ARCREP_HOME/bitarchive* $ARCREP_HOME/oldjobs* $ARCREP_HOME/cache
fi

chmod 600 $ARCREP_HOME/quickstart.jmxremote.password
mkdir -p $ARCREP_HOME/log

## Clean up log locks
rm -f $ARCREP_HOME/log/*.lck

## Clean up temporary things left behind
rm -rf $ARCREP_HOME/hcs*.sh

## JVM arguments for all processes
## Includes a simple indicator of the fact that this is a simple_harvest process
JVM_ARGS="-Xmx1512m -Ddk.netarkivet.settings.file=$ARCREP_HOME/settings.xml \
   -Dsimple.harvest.indicator=0"

## Classpath
CLASSPATH=:$NETARCHIVEDIR/lib/dk.netarkivet.archive.jar:$NETARCHIVEDIR/lib/dk.netarkivet.viewerproxy.jar:$NETARCHIVEDIR/lib/dk.netarkivet.harvester.jar:$NETARCHIVEDIR/lib/dk.netarkivet.monitor.jar

export CLASSPATH

## Term size
TERM_SIZE=80x24

## Initial window placement count, must be ++'d to get a new window position
export WINDOWPOS=0

## Restart broker
##
killall -q -9 `basename $IMQ`
sleep 2
$XTERM_CMD $HOLD -geometry $TERM_SIZE+`makeXtermOffset` -title " JMS Broker" \
  -e $IMQ -reset store -tty &
WINDOWPOS=$(( $WINDOWPOS + 1 ))
echo Waiting for IMQ broker to start, please ignore messages.
while ! telnet localhost 7676 2>&1 | grep 'portmapper tcp' ; do
   sleep 1
done

## Start Bitarchive
startApp Bitarchive archive.bitarchive.BitarchiveApplication

## Start ArcRepository
startApp ArcRepository archive.arcrepository.ArcRepositoryApplication

## Start IndexServer
startApp IndexServer archive.indexserver.IndexServerApplication

## Start GUIApplication
startApp GUIApplication common.webinterface.GUIApplication

# Start viewerproxy
startApp Viewerproxy viewerproxy.ViewerProxyApplication

## Start two harvesters
startHarvestApp 1 LOW

startHarvestApp 2 HIGH

## Start BitarchiveMonitor
startApp BitarchiveMonitor archive.bitarchive.BitarchiveMonitorApplication
