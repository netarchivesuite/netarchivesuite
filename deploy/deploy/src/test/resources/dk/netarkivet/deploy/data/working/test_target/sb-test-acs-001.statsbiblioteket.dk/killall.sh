echo Killing all applications on: 'sb-test-acs-001.statsbiblioteket.dk'
#!/bin/bash
cd /home/netarkiv/test/conf/
if [ -e ./kill_ViewerProxyApplication.sh ]; then 
      ./kill_ViewerProxyApplication.sh
fi
