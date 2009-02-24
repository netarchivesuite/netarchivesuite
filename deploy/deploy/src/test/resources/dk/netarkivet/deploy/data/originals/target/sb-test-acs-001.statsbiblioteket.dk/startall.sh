echo Starting all applications on: 'sb-test-acs-001.statsbiblioteket.dk'
#!/bin/bash
cd /home/netarkiv/TEST/conf/
if [ -e ./start_ViewerProxyApplication.sh ]; then 
      ./start_ViewerProxyApplication.sh
fi
