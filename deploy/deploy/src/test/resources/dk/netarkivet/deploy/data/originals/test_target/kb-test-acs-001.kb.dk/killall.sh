echo Killing all applications on: 'kb-test-acs-001.kb.dk'
#!/bin/bash
cd /home/test/test/conf/
if [ -e ./kill_IndexServerApplication.sh ]; then 
      ./kill_IndexServerApplication.sh
fi
if [ -e ./kill_ViewerProxyApplication.sh ]; then 
      ./kill_ViewerProxyApplication.sh
fi
