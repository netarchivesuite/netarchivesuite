#!/bin/bash
if [ $# -ne 2 ]; then
    echo usage startall_kb.sh [netarchive-user] [bitarchive-user]
    exit
fi
echo --------------------------------------------
echo starting at:$1@kb-dev-adm-001.kb.dk
ssh $1@kb-dev-adm-001.kb.dk ". /etc/profile; /home/dev/UNITTEST/conf/startall.sh; sleep 5; cat /home/dev/UNITTEST/*.log"
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$2@kb-dev-bar-010.bitarkiv.kb.dk
ssh $2@kb-dev-bar-010.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\startall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$2@kb-dev-bar-011.bitarkiv.kb.dk
ssh $2@kb-dev-bar-011.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\startall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$2@kb-dev-bar-012.bitarkiv.kb.dk
ssh $2@kb-dev-bar-012.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\startall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$2@kb-dev-bar-013.bitarkiv.kb.dk
ssh $2@kb-dev-bar-013.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\startall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$2@kb-dev-bar-014.bitarkiv.kb.dk
ssh $2@kb-dev-bar-014.bitarkiv.kb.dk "cmd /c  UNITTEST\conf\startall.bat " 
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$1@kb-dev-har-001.kb.dk
ssh $1@kb-dev-har-001.kb.dk ". /etc/profile; /home/dev/UNITTEST/conf/startall.sh; sleep 5; cat /home/dev/UNITTEST/*.log"
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$1@kb-dev-har-002.kb.dk
ssh $1@kb-dev-har-002.kb.dk ". /etc/profile; /home/dev/UNITTEST/conf/startall.sh; sleep 5; cat /home/dev/UNITTEST/*.log"
echo --------------------------------------------
echo --------------------------------------------
echo starting at:$1@kb-dev-acs-001.kb.dk
ssh $1@kb-dev-acs-001.kb.dk ". /etc/profile; /home/dev/UNITTEST/conf/startall.sh; sleep 5; cat /home/dev/UNITTEST/*.log"
echo --------------------------------------------
