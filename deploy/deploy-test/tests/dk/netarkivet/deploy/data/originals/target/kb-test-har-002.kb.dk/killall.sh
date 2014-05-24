echo Killing all applications on: 'kb-test-har-002.kb.dk'
#!/bin/bash
cd /home/test/TEST/conf/
if [ -e ./kill_HarvestControllerApplication_low.sh ]; then 
      ./kill_HarvestControllerApplication_low.sh
fi
if [ -e ./kill_HarvestControllerApplication_high.sh ]; then 
      ./kill_HarvestControllerApplication_high.sh
fi
