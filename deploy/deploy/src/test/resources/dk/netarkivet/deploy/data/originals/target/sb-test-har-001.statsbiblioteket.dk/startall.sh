echo Starting all applications on: 'sb-test-har-001.statsbiblioteket.dk'
#!/bin/bash
cd /home/netarkiv/TEST/conf/
if [ -e ./start_HarvestControllerApplication.sh ]; then 
      ./start_HarvestControllerApplication.sh
fi
