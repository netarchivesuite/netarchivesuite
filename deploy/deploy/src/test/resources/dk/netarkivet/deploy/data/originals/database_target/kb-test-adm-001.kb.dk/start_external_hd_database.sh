#!/bin/bash
cd /home/test/TEST
java -Xmx1536m  -cp /home/test/TEST/lib/db/derbynet.jar:/home/test/TEST/lib/db/derby.jar org.apache.derby.drda.NetworkServerControl -p 8118 start < /dev/null > start_external_hd_database.log 2>&1 &
