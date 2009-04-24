echo Starting all applications on: 'kb-test-adm-001.kb.dk'
#!/bin/bash
cd /home/test/TEST/conf/
if [ -e ./start_GUIApplication.sh ]; then 
      ./start_GUIApplication.sh
fi
if [ -e ./start_ArcRepositoryApplication.sh ]; then 
      ./start_ArcRepositoryApplication.sh
fi
if [ -e ./start_BitarchiveMonitorApplication_KBBM.sh ]; then 
      ./start_BitarchiveMonitorApplication_KBBM.sh
fi
if [ -e ./start_BitarchiveMonitorApplication_SBBM.sh ]; then 
      ./start_BitarchiveMonitorApplication_SBBM.sh
fi
