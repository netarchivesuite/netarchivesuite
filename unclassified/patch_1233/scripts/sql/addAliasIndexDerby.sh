#!/bin/sh

java -Xmx2048m org.apache.derby.tools.ij <<EOF
connect 'jdbc:derby:fullhddb';
   create index aliasindex on domains(alias);
EOF
