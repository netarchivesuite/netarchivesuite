echo Starting all applications at: kb-test-acs-001.kb.dk
#!/bin/bash
cd /home/dev/TEST/conf/
if [ -e ./start_ViewerProxyApplication_8081.sh ]; then 
      ./start_ViewerProxyApplication_8081.sh
fi
if [ -e ./start_ViewerProxyApplication_8082.sh ]; then 
      ./start_ViewerProxyApplication_8082.sh
fi
if [ -e ./start_IndexServerApplication.sh ]; then 
      ./start_IndexServerApplication.sh
fi
