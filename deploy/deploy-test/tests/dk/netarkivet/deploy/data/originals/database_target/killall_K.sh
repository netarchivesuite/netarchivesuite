#!/bin/bash
echo --------------------------------------------
echo KILLING MACHINE: test@kb-test-adm-001.kb.dk
ssh test@kb-test-adm-001.kb.dk ". /etc/profile; /home/test/TEST/conf/killall.sh";
echo --------------------------------------------
echo KILLING MACHINE: ba-test@kb-test-bar-010.bitarkiv.kb.dk
ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk "cmd /c  TEST\conf\killall.bat " 
echo --------------------------------------------
echo KILLING MACHINE: ba-test@kb-test-bar-011.bitarkiv.kb.dk
ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk "cmd /c  TEST\conf\killall.bat " 
echo --------------------------------------------
echo KILLING MACHINE: test@kb-test-har-001.kb.dk
ssh test@kb-test-har-001.kb.dk ". /etc/profile; /home/test/TEST/conf/killall.sh";
echo --------------------------------------------
echo KILLING MACHINE: test@kb-test-har-002.kb.dk
ssh test@kb-test-har-002.kb.dk ". /etc/profile; /home/test/TEST/conf/killall.sh";
echo --------------------------------------------
echo KILLING MACHINE: test@kb-test-acs-001.kb.dk
ssh test@kb-test-acs-001.kb.dk ". /etc/profile; /home/test/TEST/conf/killall.sh";
echo --------------------------------------------
