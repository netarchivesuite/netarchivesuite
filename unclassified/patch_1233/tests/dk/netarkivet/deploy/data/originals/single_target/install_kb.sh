#!/bin/bash
if [ $# -ne 3 ]; then
    echo usage install_kb.sh [zip-file] [netarchive-user] [bitarchive-user]
    exit
fi
echo INSTALLING TO:kb-dev-adm-001.kb.dk
echo copying $1 to:kb-dev-adm-001.kb.dk
scp $1 $2@kb-dev-adm-001.kb.dk:/home/dev
echo unzipping $1 at:kb-dev-adm-001.kb.dk
ssh $2@kb-dev-adm-001.kb.dk unzip -q -o /home/dev/$1 -d /home/dev/UNITTEST
echo copying settings and scripts
scp -r kb-dev-adm-001.kb.dk/* $2@kb-dev-adm-001.kb.dk:/home/dev/UNITTEST/conf/
echo make scripts executable
ssh  $2@kb-dev-adm-001.kb.dk "chmod +x /home/dev/UNITTEST/conf/*.sh "
echo make password files readonly
ssh $2@kb-dev-adm-001.kb.dk "chmod 400 /home/dev/UNITTEST/conf//jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO:kb-dev-bar-010.bitarkiv.kb.dk
echo copying $1 to:kb-dev-bar-010.bitarkiv.kb.dk
scp $1 $3@kb-dev-bar-010.bitarkiv.kb.dk:
echo unzipping $1 at:kb-dev-bar-010.bitarkiv.kb.dk
ssh $3@kb-dev-bar-010.bitarkiv.kb.dk cmd /c unzip.exe -q -d UNITTEST -o $1
echo copying settings and scripts
scp -r kb-dev-bar-010.bitarkiv.kb.dk/* $3@kb-dev-bar-010.bitarkiv.kb.dk:UNITTEST\\conf\\
echo make scripts executable
echo make password files readonly
echo Y | ssh $3@kb-dev-bar-010.bitarkiv.kb.dk cmd /c cacls UNITTEST\\conf\\jmxremote.password /P BITARKIV\\$3:R
echo --------------------------------------------
echo INSTALLING TO:kb-dev-bar-011.bitarkiv.kb.dk
echo copying $1 to:kb-dev-bar-011.bitarkiv.kb.dk
scp $1 $3@kb-dev-bar-011.bitarkiv.kb.dk:
echo unzipping $1 at:kb-dev-bar-011.bitarkiv.kb.dk
ssh $3@kb-dev-bar-011.bitarkiv.kb.dk cmd /c unzip.exe -q -d UNITTEST -o $1
echo copying settings and scripts
scp -r kb-dev-bar-011.bitarkiv.kb.dk/* $3@kb-dev-bar-011.bitarkiv.kb.dk:UNITTEST\\conf\\
echo make scripts executable
echo make password files readonly
echo Y | ssh $3@kb-dev-bar-011.bitarkiv.kb.dk cmd /c cacls UNITTEST\\conf\\jmxremote.password /P BITARKIV\\$3:R
echo --------------------------------------------
echo INSTALLING TO:kb-dev-bar-012.bitarkiv.kb.dk
echo copying $1 to:kb-dev-bar-012.bitarkiv.kb.dk
scp $1 $3@kb-dev-bar-012.bitarkiv.kb.dk:
echo unzipping $1 at:kb-dev-bar-012.bitarkiv.kb.dk
ssh $3@kb-dev-bar-012.bitarkiv.kb.dk cmd /c unzip.exe -q -d UNITTEST -o $1
echo copying settings and scripts
scp -r kb-dev-bar-012.bitarkiv.kb.dk/* $3@kb-dev-bar-012.bitarkiv.kb.dk:UNITTEST\\conf\\
echo make scripts executable
echo make password files readonly
echo Y | ssh $3@kb-dev-bar-012.bitarkiv.kb.dk cmd /c cacls UNITTEST\\conf\\jmxremote.password /P BITARKIV\\$3:R
echo --------------------------------------------
echo INSTALLING TO:kb-dev-bar-013.bitarkiv.kb.dk
echo copying $1 to:kb-dev-bar-013.bitarkiv.kb.dk
scp $1 $3@kb-dev-bar-013.bitarkiv.kb.dk:
echo unzipping $1 at:kb-dev-bar-013.bitarkiv.kb.dk
ssh $3@kb-dev-bar-013.bitarkiv.kb.dk cmd /c unzip.exe -q -d UNITTEST -o $1
echo copying settings and scripts
scp -r kb-dev-bar-013.bitarkiv.kb.dk/* $3@kb-dev-bar-013.bitarkiv.kb.dk:UNITTEST\\conf\\
echo make scripts executable
echo make password files readonly
echo Y | ssh $3@kb-dev-bar-013.bitarkiv.kb.dk cmd /c cacls UNITTEST\\conf\\jmxremote.password /P BITARKIV\\$3:R
echo --------------------------------------------
echo INSTALLING TO:kb-dev-bar-014.bitarkiv.kb.dk
echo copying $1 to:kb-dev-bar-014.bitarkiv.kb.dk
scp $1 $3@kb-dev-bar-014.bitarkiv.kb.dk:
echo unzipping $1 at:kb-dev-bar-014.bitarkiv.kb.dk
ssh $3@kb-dev-bar-014.bitarkiv.kb.dk cmd /c unzip.exe -q -d UNITTEST -o $1
echo copying settings and scripts
scp -r kb-dev-bar-014.bitarkiv.kb.dk/* $3@kb-dev-bar-014.bitarkiv.kb.dk:UNITTEST\\conf\\
echo make scripts executable
echo make password files readonly
echo Y | ssh $3@kb-dev-bar-014.bitarkiv.kb.dk cmd /c cacls UNITTEST\\conf\\jmxremote.password /P BITARKIV\\$3:R
echo --------------------------------------------
echo INSTALLING TO:kb-dev-har-001.kb.dk
echo copying $1 to:kb-dev-har-001.kb.dk
scp $1 $2@kb-dev-har-001.kb.dk:/home/dev
echo unzipping $1 at:kb-dev-har-001.kb.dk
ssh $2@kb-dev-har-001.kb.dk unzip -q -o /home/dev/$1 -d /home/dev/UNITTEST
echo copying settings and scripts
scp -r kb-dev-har-001.kb.dk/* $2@kb-dev-har-001.kb.dk:/home/dev/UNITTEST/conf/
echo make scripts executable
ssh  $2@kb-dev-har-001.kb.dk "chmod +x /home/dev/UNITTEST/conf/*.sh "
echo make password files readonly
ssh $2@kb-dev-har-001.kb.dk "chmod 400 /home/dev/UNITTEST/conf//jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO:kb-dev-har-002.kb.dk
echo copying $1 to:kb-dev-har-002.kb.dk
scp $1 $2@kb-dev-har-002.kb.dk:/home/dev
echo unzipping $1 at:kb-dev-har-002.kb.dk
ssh $2@kb-dev-har-002.kb.dk unzip -q -o /home/dev/$1 -d /home/dev/UNITTEST
echo copying settings and scripts
scp -r kb-dev-har-002.kb.dk/* $2@kb-dev-har-002.kb.dk:/home/dev/UNITTEST/conf/
echo make scripts executable
ssh  $2@kb-dev-har-002.kb.dk "chmod +x /home/dev/UNITTEST/conf/*.sh "
echo make password files readonly
ssh $2@kb-dev-har-002.kb.dk "chmod 400 /home/dev/UNITTEST/conf//jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO:kb-dev-acs-001.kb.dk
echo copying $1 to:kb-dev-acs-001.kb.dk
scp $1 $2@kb-dev-acs-001.kb.dk:/home/dev
echo unzipping $1 at:kb-dev-acs-001.kb.dk
ssh $2@kb-dev-acs-001.kb.dk unzip -q -o /home/dev/$1 -d /home/dev/UNITTEST
echo copying settings and scripts
scp -r kb-dev-acs-001.kb.dk/* $2@kb-dev-acs-001.kb.dk:/home/dev/UNITTEST/conf/
echo make scripts executable
ssh  $2@kb-dev-acs-001.kb.dk "chmod +x /home/dev/UNITTEST/conf/*.sh "
echo make password files readonly
ssh $2@kb-dev-acs-001.kb.dk "chmod 400 /home/dev/UNITTEST/conf//jmxremote.password"
echo --------------------------------------------
