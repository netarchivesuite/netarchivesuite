echo Starting all applications at: sb-test-bar-001.statsbiblioteket.dk
#!/bin/bash
cd /home/netarkiv/TEST/conf/
if [ -e ./start_BitarchiveApplication.sh ]; then 
      ./start_BitarchiveApplication.sh
fi
