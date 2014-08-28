echo Killing all applications on: 'kb-test-har-001.kb.dk'
#!/bin/bash
cd /home/test/TEST/conf/
if [ -e ./kill_HarvestControllerApplication.sh ]; then 
      ./kill_HarvestControllerApplication.sh
fi
