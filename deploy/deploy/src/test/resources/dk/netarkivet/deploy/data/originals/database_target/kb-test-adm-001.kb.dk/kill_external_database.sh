#!/bin/bash
cd /home/test/TEST
export CLASSPATH=lib/db/derbynet.jar:lib/db/derby.jar:$CLASSPATH
java org.apache.derby.drda.NetworkServerControl -p 8119 shutdown &
