echo Killing all applications on: 'kb-test-adm-001.kb.dk'
#!/bin/bash
cd /home/test/test/conf/
if [ -e ./kill_GUIApplication.sh ]; then 
      ./kill_GUIApplication.sh
fi
if [ -e ./kill_ArcRepositoryApplication.sh ]; then 
      ./kill_ArcRepositoryApplication.sh
fi
if [ -e ./kill_BitarchiveMonitorApplication_KBBM.sh ]; then 
      ./kill_BitarchiveMonitorApplication_KBBM.sh
fi
if [ -e ./kill_BitarchiveMonitorApplication_SBBM.sh ]; then 
      ./kill_BitarchiveMonitorApplication_SBBM.sh
fi
