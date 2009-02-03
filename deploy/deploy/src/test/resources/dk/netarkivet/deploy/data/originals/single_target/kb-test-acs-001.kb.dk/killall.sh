echo Killing all applications at: kb-test-acs-001.kb.dk
#!/bin/bash
cd /home/dev/TEST/conf/
if [ -e ./kill_ViewerProxyApplication_8081.sh]; then 
      ./kill_ViewerProxyApplication_8081.sh
fi
if [ -e ./kill_ViewerProxyApplication_8082.sh]; then 
      ./kill_ViewerProxyApplication_8082.sh
fi
if [ -e ./kill_IndexServerApplication.sh]; then 
      ./kill_IndexServerApplication.sh
fi
