echo Starting all applications on: 'kb-test-adm-001.kb.dk'
#!/bin/bash
cd /home/dev/TEST/conf/
if [ -e ./start_GUIApplication.sh ]; then 
      ./start_GUIApplication.sh
fi
if [ -e ./start_ArcRepositoryApplication.sh ]; then 
      ./start_ArcRepositoryApplication.sh
fi
if [ -e ./start_BitarchiveMonitorApplication.sh ]; then 
      ./start_BitarchiveMonitorApplication.sh
fi
if [ -e ./start_HarvestControllerApplication.sh ]; then 
      ./start_HarvestControllerApplication.sh
fi
