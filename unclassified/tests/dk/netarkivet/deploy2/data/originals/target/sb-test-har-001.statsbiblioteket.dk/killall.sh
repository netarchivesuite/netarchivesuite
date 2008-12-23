echo Killing all applications at: sb-test-har-001.statsbiblioteket.dk
#!/bin/bash
cd /home/netarkiv/TEST/conf/
if [ -e ./kill_HarvestControllerApplication.sh]; then 
      ./kill_HarvestControllerApplication.sh
fi
