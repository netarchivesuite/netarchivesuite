#!/bin/bash
echo --------------------------------------------
echo KILLING MACHINE: netarkiv@sb-test-har-001.statsbiblioteket.dk
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/test/conf/killall.sh";
echo --------------------------------------------
echo KILLING MACHINE: netarkiv@sb-test-bar-001.statsbiblioteket.dk
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/test/conf/killall.sh";
echo --------------------------------------------
echo KILLING MACHINE: netarkiv@sb-test-acs-001.statsbiblioteket.dk
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/test/conf/killall.sh";
echo --------------------------------------------
