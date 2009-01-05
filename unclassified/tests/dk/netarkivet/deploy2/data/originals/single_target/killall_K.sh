#!/bin/bash
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-adm-001.kb.dk
ssh dev@kb-test-adm-001.kb.dk ". /etc/profile; /home/dev/TEST/conf/killall.sh";
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-bar-010.bitarkiv.kb.dk
ssh dev@kb-test-bar-010.bitarkiv.kb.dk "cmd /c  c:\Documents and Settings\dev\TEST\conf\killall.bat " 
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-bar-011.bitarkiv.kb.dk
ssh dev@kb-test-bar-011.bitarkiv.kb.dk "cmd /c  c:\Documents and Settings\dev\TEST\conf\killall.bat " 
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-bar-012.bitarkiv.kb.dk
ssh dev@kb-test-bar-012.bitarkiv.kb.dk "cmd /c  c:\Documents and Settings\dev\TEST\conf\killall.bat " 
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-bar-013.bitarkiv.kb.dk
ssh dev@kb-test-bar-013.bitarkiv.kb.dk "cmd /c  c:\Documents and Settings\dev\TEST\conf\killall.bat " 
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-bar-014.bitarkiv.kb.dk
ssh dev@kb-test-bar-014.bitarkiv.kb.dk "cmd /c  c:\Documents and Settings\dev\TEST\conf\killall.bat " 
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-har-001.kb.dk
ssh dev@kb-test-har-001.kb.dk ". /etc/profile; /home/dev/TEST/conf/killall.sh";
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-har-002.kb.dk
ssh dev@kb-test-har-002.kb.dk ". /etc/profile; /home/dev/TEST/conf/killall.sh";
echo --------------------------------------------
echo KILLING MACHINE: dev@kb-test-acs-001.kb.dk
ssh dev@kb-test-acs-001.kb.dk ". /etc/profile; /home/dev/TEST/conf/killall.sh";
echo --------------------------------------------
