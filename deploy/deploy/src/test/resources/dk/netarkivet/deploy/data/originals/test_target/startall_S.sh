#!/bin/bash
echo --------------------------------------------
echo STARTING MACHINE: netarkiv@sb-test-har-001.statsbiblioteket.dk
ssh netarkiv@sb-test-har-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/test/conf/startall.sh; sleep 5; cat /home/netarkiv/test/*.log"
echo --------------------------------------------
echo STARTING MACHINE: netarkiv@sb-test-bar-001.statsbiblioteket.dk
ssh netarkiv@sb-test-bar-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/test/conf/startall.sh; sleep 5; cat /home/netarkiv/test/*.log"
echo --------------------------------------------
echo STARTING MACHINE: netarkiv@sb-test-acs-001.statsbiblioteket.dk
ssh netarkiv@sb-test-acs-001.statsbiblioteket.dk ". /etc/profile; /home/netarkiv/test/conf/startall.sh; sleep 5; cat /home/netarkiv/test/*.log"
echo --------------------------------------------
