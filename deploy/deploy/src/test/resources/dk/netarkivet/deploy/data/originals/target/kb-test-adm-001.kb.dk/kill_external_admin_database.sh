#!/bin/bash
cd /home/test/TEST
java -cp /home/test/TEST/lib/db/derbynet.jar:/home/test/TEST/lib/db/derby.jar org.apache.derby.drda.NetworkServerControl -p 8010 shutdown < /dev/null >> start_external_admin_database.log 2>&1 &
