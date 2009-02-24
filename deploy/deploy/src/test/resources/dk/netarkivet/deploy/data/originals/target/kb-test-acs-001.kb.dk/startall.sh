echo Starting all applications on: 'kb-test-acs-001.kb.dk'
#!/bin/bash
cd /home/test/TEST/conf/
if [ -e ./start_IndexServerApplication.sh ]; then 
      ./start_IndexServerApplication.sh
fi
if [ -e ./start_ViewerProxyApplication.sh ]; then 
      ./start_ViewerProxyApplication.sh
fi
