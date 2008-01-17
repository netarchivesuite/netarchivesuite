#!/bin/bash
if [ $# -ne 2 ]; then
    echo usage killall_sb.sh [netarchive-user] [bitarchive-user]
    exit
fi
echo --------------------------------------------
echo kill at $2@sb-dev-bar-001.statsbiblioteket.dk
ssh $2@sb-dev-bar-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/UNITTEST/conf/killall.sh"
echo --------------------------------------------
echo --------------------------------------------
echo kill at $1@sb-dev-har-001.statsbiblioteket.dk
ssh $1@sb-dev-har-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/UNITTEST/conf/killall.sh"
echo --------------------------------------------
echo --------------------------------------------
echo kill at $1@sb-dev-acs-001.statsbiblioteket.dk
ssh $1@sb-dev-acs-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/UNITTEST/conf/killall.sh"
echo --------------------------------------------
