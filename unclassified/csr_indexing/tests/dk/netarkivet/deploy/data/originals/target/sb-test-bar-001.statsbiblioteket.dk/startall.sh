#!/bin/bash
cd /home/netarkiv/TEST/conf/
echo Starting all applications on: 'sb-test-bar-001.statsbiblioteket.dk'
if [ -e ./start_BitarchiveApplication.sh ]; then 
      ./start_BitarchiveApplication.sh
fi
