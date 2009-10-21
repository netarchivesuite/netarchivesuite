#!/bin/bash
echo --------------------------------------------
echo INSTALLING TO MACHINE: netarkiv@sb-test-har-001.statsbiblioteket.dk
echo copying null.zip to:sb-test-har-001.statsbiblioteket.dk
scp null.zip netarkiv@sb-test-har-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping null.zip at:sb-test-har-001.statsbiblioteket.dk
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/null.zip -d /home/netarkiv/test
echo Creating directories.
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "cd /home/netarkiv/test; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d harvester_high ]; then mkdir harvester_high; fi; exit; "
echo preparing for copying of settings and scripts
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/test/conf/jmxremote.password ]; then chmod u+rwx /home/netarkiv/test/conf/jmxremote.password; fi; "
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/test/conf/jmxremote.access ]; then chmod u+rwx /home/netarkiv/test/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r sb-test-har-001.statsbiblioteket.dk/* netarkiv@sb-test-har-001.statsbiblioteket.dk:/home/netarkiv/test/conf/
echo make scripts executable
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "chmod 700 /home/netarkiv/test/conf/*.sh "
echo make password and access files readonly
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "mv -f /home/netarkiv/test/conf/jmxremote.access /home/netarkiv/test/conf/access.privileges"
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "mv -f /home/netarkiv/test/conf/jmxremote.password /home/netarkiv/test/./jmxremote.password"
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/test/./jmxremote.password"
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/test/conf/access.privileges"
echo --------------------------------------------
echo INSTALLING TO MACHINE: netarkiv@sb-test-bar-001.statsbiblioteket.dk
echo copying null.zip to:sb-test-bar-001.statsbiblioteket.dk
scp null.zip netarkiv@sb-test-bar-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping null.zip at:sb-test-bar-001.statsbiblioteket.dk
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/null.zip -d /home/netarkiv/test
echo Creating directories.
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "cd /home/netarkiv/test; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d /netarkiv ]; then mkdir /netarkiv; fi; if [ ! -d /netarkiv/0001 ]; then mkdir /netarkiv/0001; fi; if [ ! -d /netarkiv/0001/test ]; then mkdir /netarkiv/0001/test; fi; if [ ! -d /netarkiv/0001/test/filedir ]; then mkdir /netarkiv/0001/test/filedir; fi; if [ ! -d /netarkiv/0001/test/tempdir ]; then mkdir /netarkiv/0001/test/tempdir; fi; if [ ! -d /netarkiv/0001/test/atticdir ]; then mkdir /netarkiv/0001/test/atticdir; fi; if [ ! -d /netarkiv ]; then mkdir /netarkiv; fi; if [ ! -d /netarkiv/0002 ]; then mkdir /netarkiv/0002; fi; if [ ! -d /netarkiv/0002/test ]; then mkdir /netarkiv/0002/test; fi; if [ ! -d /netarkiv/0002/test/filedir ]; then mkdir /netarkiv/0002/test/filedir; fi; if [ ! -d /netarkiv/0002/test/tempdir ]; then mkdir /netarkiv/0002/test/tempdir; fi; if [ ! -d /netarkiv/0002/test/atticdir ]; then mkdir /netarkiv/0002/test/atticdir; fi; exit; "
echo preparing for copying of settings and scripts
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/test/conf/jmxremote.password ]; then chmod u+rwx /home/netarkiv/test/conf/jmxremote.password; fi; "
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/test/conf/jmxremote.access ]; then chmod u+rwx /home/netarkiv/test/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r sb-test-bar-001.statsbiblioteket.dk/* netarkiv@sb-test-bar-001.statsbiblioteket.dk:/home/netarkiv/test/conf/
echo make scripts executable
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "chmod 700 /home/netarkiv/test/conf/*.sh "
echo make password and access files readonly
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "mv -f /home/netarkiv/test/conf/jmxremote.access /home/netarkiv/test/conf/access.privileges"
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "mv -f /home/netarkiv/test/conf/jmxremote.password /home/netarkiv/test/./jmxremote.password"
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/test/./jmxremote.password"
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/test/conf/access.privileges"
echo --------------------------------------------
echo INSTALLING TO MACHINE: netarkiv@sb-test-acs-001.statsbiblioteket.dk
echo copying null.zip to:sb-test-acs-001.statsbiblioteket.dk
scp null.zip netarkiv@sb-test-acs-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping null.zip at:sb-test-acs-001.statsbiblioteket.dk
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/null.zip -d /home/netarkiv/test
echo Creating directories.
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "cd /home/netarkiv/test; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d viewerproxy ]; then mkdir viewerproxy; fi; exit; "
echo preparing for copying of settings and scripts
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/test/conf/jmxremote.password ]; then chmod u+rwx /home/netarkiv/test/conf/jmxremote.password; fi; "
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk " cd ~; if [ -e /home/netarkiv/test/conf/jmxremote.access ]; then chmod u+rwx /home/netarkiv/test/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r sb-test-acs-001.statsbiblioteket.dk/* netarkiv@sb-test-acs-001.statsbiblioteket.dk:/home/netarkiv/test/conf/
echo make scripts executable
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "chmod 700 /home/netarkiv/test/conf/*.sh "
echo make password and access files readonly
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "mv -f /home/netarkiv/test/conf/jmxremote.access /home/netarkiv/test/conf/access.privileges"
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "mv -f /home/netarkiv/test/conf/jmxremote.password /home/netarkiv/test/./jmxremote.password"
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/test/./jmxremote.password"
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/test/conf/access.privileges"
echo --------------------------------------------
