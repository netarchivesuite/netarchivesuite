#!/bin/bash
if [ $# -ne 2 ]; then
    echo usage killall_kb.sh [netarchive-user] [bitarchive-user]
    exit
fi
echo --------------------------------------------
echo kill at $1@kb-dev-adm-001.kb.dk
ssh $1@kb-dev-adm-001.kb.dk ". /etc/profile; /home/dev/UNITTEST/conf/killall.sh"
echo --------------------------------------------
echo --------------------------------------------
echo kill at $2@kb-dev-bar-010.bitarkiv.kb.dk
ssh $2@kb-dev-bar-010.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\killall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo kill at $2@kb-dev-bar-011.bitarkiv.kb.dk
ssh $2@kb-dev-bar-011.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\killall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo kill at $2@kb-dev-bar-012.bitarkiv.kb.dk
ssh $2@kb-dev-bar-012.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\killall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo kill at $2@kb-dev-bar-013.bitarkiv.kb.dk
ssh $2@kb-dev-bar-013.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\killall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo kill at $2@kb-dev-bar-014.bitarkiv.kb.dk
ssh $2@kb-dev-bar-014.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\killall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo kill at $1@kb-dev-har-001.kb.dk
ssh $1@kb-dev-har-001.kb.dk ". /etc/profile; /home/dev/UNITTEST/conf/killall.sh"
echo --------------------------------------------
echo --------------------------------------------
echo kill at $1@kb-dev-har-002.kb.dk
ssh $1@kb-dev-har-002.kb.dk ". /etc/profile; /home/dev/UNITTEST/conf/killall.sh"
echo --------------------------------------------
echo --------------------------------------------
echo kill at $1@kb-dev-acs-001.kb.dk
ssh $1@kb-dev-acs-001.kb.dk ". /etc/profile; /home/dev/UNITTEST/conf/killall.sh"
echo --------------------------------------------
