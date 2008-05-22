#!/bin/bash
if [ $# -ne 2 ]; then
    echo usage startall_sb.sh [netarchive-user] [bitarchive-user]
    exit
fi
echo --------------------------------------------
echo starting at:$2@sb-dev-bar-001.statsbiblioteket.dk
ssh $2@sb-dev-bar-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/UNITTEST/conf/startall.sh; sleep 5; cat /home/netarkiv/UNITTEST/*.log"
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$1@sb-dev-har-001.statsbiblioteket.dk
ssh $1@sb-dev-har-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/UNITTEST/conf/startall.sh; sleep 5; cat /home/netarkiv/UNITTEST/*.log"
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$1@sb-dev-acs-001.statsbiblioteket.dk
ssh $1@sb-dev-acs-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/UNITTEST/conf/startall.sh; sleep 5; cat /home/netarkiv/UNITTEST/*.log"
echo --------------------------------------------
