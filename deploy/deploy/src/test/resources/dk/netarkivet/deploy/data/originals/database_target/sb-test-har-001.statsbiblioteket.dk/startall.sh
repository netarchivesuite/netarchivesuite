#!/bin/bash
cd /home/netarkiv/TEST/conf/
echo Starting all applications on: 'sb-test-har-001.statsbiblioteket.dk'
if [ -e ./start_HarvestControllerApplication.sh ]; then 
      ./start_HarvestControllerApplication.sh
fi
