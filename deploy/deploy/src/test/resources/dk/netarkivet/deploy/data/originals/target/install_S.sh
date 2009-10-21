#!/bin/bash
echo --------------------------------------------
echo INSTALLING TO MACHINE: netarkiv@sb-test-har-001.statsbiblioteket.dk
echo copying null.zip to:sb-test-har-001.statsbiblioteket.dk
scp null.zip netarkiv@sb-test-har-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping null.zip at:sb-test-har-001.statsbiblioteket.dk
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/null.zip -d /home/netarkiv/TEST
echo Creating directories.
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "cd /home/netarkiv/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d harvester_high ]; then mkdir harvester_high; fi; exit; "
echo preparing for copying of settings and scripts
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/TEST/conf/jmxremote.password ]; then chmod u+rwx /home/netarkiv/TEST/conf/jmxremote.password; fi; "
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/TEST/conf/jmxremote.access ]; then chmod u+rwx /home/netarkiv/TEST/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r sb-test-har-001.statsbiblioteket.dk/* netarkiv@sb-test-har-001.statsbiblioteket.dk:/home/netarkiv/TEST/conf/
echo make scripts executable
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "chmod 700 /home/netarkiv/TEST/conf/*.sh "
echo make password and access files readonly
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "mv -f /home/netarkiv/TEST/conf/jmxremote.access /home/netarkiv/TEST/conf/access.privileges"
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "mv -f /home/netarkiv/TEST/conf/jmxremote.password /home/netarkiv/TEST/./jmxremote.password"
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/TEST/./jmxremote.password"
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/TEST/conf/access.privileges"
echo --------------------------------------------
echo INSTALLING TO MACHINE: netarkiv@sb-test-bar-001.statsbiblioteket.dk
echo copying null.zip to:sb-test-bar-001.statsbiblioteket.dk
scp null.zip netarkiv@sb-test-bar-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping null.zip at:sb-test-bar-001.statsbiblioteket.dk
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/null.zip -d /home/netarkiv/TEST
echo Creating directories.
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "cd /home/netarkiv/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d /netarkiv ]; then mkdir /netarkiv; fi; if [ ! -d /netarkiv/0001 ]; then mkdir /netarkiv/0001; fi; if [ ! -d /netarkiv/0001/filedir ]; then mkdir /netarkiv/0001/filedir; fi; if [ ! -d /netarkiv/0001/tempdir ]; then mkdir /netarkiv/0001/tempdir; fi; if [ ! -d /netarkiv/0001/atticdir ]; then mkdir /netarkiv/0001/atticdir; fi; if [ ! -d /netarkiv ]; then mkdir /netarkiv; fi; if [ ! -d /netarkiv/0002 ]; then mkdir /netarkiv/0002; fi; if [ ! -d /netarkiv/0002/filedir ]; then mkdir /netarkiv/0002/filedir; fi; if [ ! -d /netarkiv/0002/tempdir ]; then mkdir /netarkiv/0002/tempdir; fi; if [ ! -d /netarkiv/0002/atticdir ]; then mkdir /netarkiv/0002/atticdir; fi; exit; "
echo preparing for copying of settings and scripts
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/TEST/conf/jmxremote.password ]; then chmod u+rwx /home/netarkiv/TEST/conf/jmxremote.password; fi; "
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/TEST/conf/jmxremote.access ]; then chmod u+rwx /home/netarkiv/TEST/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r sb-test-bar-001.statsbiblioteket.dk/* netarkiv@sb-test-bar-001.statsbiblioteket.dk:/home/netarkiv/TEST/conf/
echo make scripts executable
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "chmod 700 /home/netarkiv/TEST/conf/*.sh "
echo make password and access files readonly
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "mv -f /home/netarkiv/TEST/conf/jmxremote.access /home/netarkiv/TEST/conf/access.privileges"
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "mv -f /home/netarkiv/TEST/conf/jmxremote.password /home/netarkiv/TEST/./jmxremote.password"
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/TEST/./jmxremote.password"
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/TEST/conf/access.privileges"
echo --------------------------------------------
echo INSTALLING TO MACHINE: netarkiv@sb-test-acs-001.statsbiblioteket.dk
echo copying null.zip to:sb-test-acs-001.statsbiblioteket.dk
scp null.zip netarkiv@sb-test-acs-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping null.zip at:sb-test-acs-001.statsbiblioteket.dk
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/null.zip -d /home/netarkiv/TEST
echo Creating directories.
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "cd /home/netarkiv/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d viewerproxy ]; then mkdir viewerproxy; fi; exit; "
echo preparing for copying of settings and scripts
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/TEST/conf/jmxremote.password ]; then chmod u+rwx /home/netarkiv/TEST/conf/jmxremote.password; fi; "
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/TEST/conf/jmxremote.access ]; then chmod u+rwx /home/netarkiv/TEST/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r sb-test-acs-001.statsbiblioteket.dk/* netarkiv@sb-test-acs-001.statsbiblioteket.dk:/home/netarkiv/TEST/conf/
echo make scripts executable
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "chmod 700 /home/netarkiv/TEST/conf/*.sh "
echo make password and access files readonly
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "mv -f /home/netarkiv/TEST/conf/jmxremote.access /home/netarkiv/TEST/conf/access.privileges"
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "mv -f /home/netarkiv/TEST/conf/jmxremote.password /home/netarkiv/TEST/./jmxremote.password"
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/TEST/./jmxremote.password"
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/TEST/conf/access.privileges"
echo --------------------------------------------
