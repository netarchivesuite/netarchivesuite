#!/bin/bash
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-adm-001.kb.dk
ssh dev@kb-test-adm-001.kb.dk ". /etc/profile; /home/dev/TEST/conf/startall.sh; sleep 5; cat /home/dev/TEST/*.log"
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-bar-010.bitarkiv.kb.dk
ssh dev@kb-test-bar-010.bitarkiv.kb.dk "cmd /c  TEST\conf\startall.bat " 
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-bar-011.bitarkiv.kb.dk
ssh dev@kb-test-bar-011.bitarkiv.kb.dk "cmd /c  TEST\conf\startall.bat " 
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-bar-012.bitarkiv.kb.dk
ssh dev@kb-test-bar-012.bitarkiv.kb.dk "cmd /c  TEST\conf\startall.bat " 
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-bar-013.bitarkiv.kb.dk
ssh dev@kb-test-bar-013.bitarkiv.kb.dk "cmd /c  TEST\conf\startall.bat " 
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-bar-014.bitarkiv.kb.dk
ssh dev@kb-test-bar-014.bitarkiv.kb.dk "cmd /c  TEST\conf\startall.bat " 
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-har-001.kb.dk
ssh dev@kb-test-har-001.kb.dk ". /etc/profile; /home/dev/TEST/conf/startall.sh; sleep 5; cat /home/dev/TEST/*.log"
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-har-002.kb.dk
ssh dev@kb-test-har-002.kb.dk ". /etc/profile; /home/dev/TEST/conf/startall.sh; sleep 5; cat /home/dev/TEST/*.log"
echo --------------------------------------------
echo STARTING MACHINE: dev@kb-test-acs-001.kb.dk
ssh dev@kb-test-acs-001.kb.dk ". /etc/profile; /home/dev/TEST/conf/startall.sh; sleep 5; cat /home/dev/TEST/*.log"
echo --------------------------------------------
