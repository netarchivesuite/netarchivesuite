echo Killing all applications at: sb-test-bar-001.statsbiblioteket.dk
#!/bin/bash
cd /home/netarkiv/TEST/conf/
if [ -e ./kill_BitarchiveApplication.sh]; then 
      ./kill_BitarchiveApplication.sh
fi
