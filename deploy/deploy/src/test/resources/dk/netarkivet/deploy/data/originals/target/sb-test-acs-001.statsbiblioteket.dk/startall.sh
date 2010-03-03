#!/bin/bash
cd /home/netarkiv/TEST/conf/
echo Starting all applications on: 'sb-test-acs-001.statsbiblioteket.dk'
if [ -e ./start_ViewerProxyApplication.sh ]; then 
      ./start_ViewerProxyApplication.sh
fi
