#!/bin/bash
if [ $# -ne 3 ]; then
    echo usage install_sb.sh [zip-file] [netarchive-user] [bitarchive-user]
    exit
fi
echo INSTALLING TO:sb-dev-bar-001.statsbiblioteket.dk
echo copying $1 to:sb-dev-bar-001.statsbiblioteket.dk
scp $1 $3@sb-dev-bar-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping $1 at:sb-dev-bar-001.statsbiblioteket.dk
ssh $3@sb-dev-bar-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/$1 -d /home/netarkiv/UNITTEST
echo copying settings and scripts
scp -r sb-dev-bar-001.statsbiblioteket.dk/* $3@sb-dev-bar-001.statsbiblioteket.dk:/home/netarkiv/UNITTEST/conf/
echo make scripts executable
ssh  $3@sb-dev-bar-001.statsbiblioteket.dk "chmod +x /home/netarkiv/UNITTEST/conf/*.sh "
echo make password files readonly
ssh $3@sb-dev-bar-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/UNITTEST/conf//jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO:sb-dev-har-001.statsbiblioteket.dk
echo copying $1 to:sb-dev-har-001.statsbiblioteket.dk
scp $1 $2@sb-dev-har-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping $1 at:sb-dev-har-001.statsbiblioteket.dk
ssh $2@sb-dev-har-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/$1 -d /home/netarkiv/UNITTEST
echo copying settings and scripts
scp -r sb-dev-har-001.statsbiblioteket.dk/* $2@sb-dev-har-001.statsbiblioteket.dk:/home/netarkiv/UNITTEST/conf/
echo make scripts executable
ssh  $2@sb-dev-har-001.statsbiblioteket.dk "chmod +x /home/netarkiv/UNITTEST/conf/*.sh "
echo make password files readonly
ssh $2@sb-dev-har-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/UNITTEST/conf//jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO:sb-dev-acs-001.statsbiblioteket.dk
echo copying $1 to:sb-dev-acs-001.statsbiblioteket.dk
scp $1 $2@sb-dev-acs-001.statsbiblioteket.dk:/home/netarkiv
echo unzipping $1 at:sb-dev-acs-001.statsbiblioteket.dk
ssh $2@sb-dev-acs-001.statsbiblioteket.dk unzip -q -o /home/netarkiv/$1 -d /home/netarkiv/UNITTEST
echo copying settings and scripts
scp -r sb-dev-acs-001.statsbiblioteket.dk/* $2@sb-dev-acs-001.statsbiblioteket.dk:/home/netarkiv/UNITTEST/conf/
echo make scripts executable
ssh  $2@sb-dev-acs-001.statsbiblioteket.dk "chmod +x /home/netarkiv/UNITTEST/conf/*.sh "
echo make password files readonly
ssh $2@sb-dev-acs-001.statsbiblioteket.dk "chmod 400 /home/netarkiv/UNITTEST/conf//jmxremote.password"
echo --------------------------------------------
