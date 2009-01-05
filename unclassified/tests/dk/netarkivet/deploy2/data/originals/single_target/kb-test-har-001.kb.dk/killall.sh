echo Killing all applications at: kb-test-har-001.kb.dk
#!/bin/bash
cd /home/dev/TEST/conf/
if [ -e ./kill_HarvestControllerApplication_8081.sh]; then 
      ./kill_HarvestControllerApplication_8081.sh
fi
if [ -e ./kill_HarvestControllerApplication_8082.sh]; then 
      ./kill_HarvestControllerApplication_8082.sh
fi
if [ -e ./kill_HarvestControllerApplication_8083.sh]; then 
      ./kill_HarvestControllerApplication_8083.sh
fi
