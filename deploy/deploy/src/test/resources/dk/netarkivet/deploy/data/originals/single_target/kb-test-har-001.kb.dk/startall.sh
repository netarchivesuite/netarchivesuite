echo Starting all applications at: kb-test-har-001.kb.dk
#!/bin/bash
cd /home/dev/TEST/conf/
if [ -e ./start_HarvestControllerApplication_8081.sh ]; then 
      ./start_HarvestControllerApplication_8081.sh
fi
if [ -e ./start_HarvestControllerApplication_8082.sh ]; then 
      ./start_HarvestControllerApplication_8082.sh
fi
if [ -e ./start_HarvestControllerApplication_8083.sh ]; then 
      ./start_HarvestControllerApplication_8083.sh
fi
