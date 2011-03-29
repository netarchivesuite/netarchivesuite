#!/bin/bash
cd /home/test/TEST/conf/
echo Starting all applications on: 'kb-test-har-001.kb.dk'
if [ -e ./start_HarvestControllerApplication.sh ]; then 
      ./start_HarvestControllerApplication.sh
fi
