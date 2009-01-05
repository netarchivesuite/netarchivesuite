#!/bin/bash
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-adm-001.kb.dk
echo copying null.zip to:kb-test-adm-001.kb.dk
scp null.zip dev@kb-test-adm-001.kb.dk:/home/dev
echo unzipping null.zip at:kb-test-adm-001.kb.dk
ssh dev@kb-test-adm-001.kb.dk unzip -q -o /home/dev/null.zip -d /home/dev/TEST
echo copying settings and scripts
scp -r kb-test-adm-001.kb.dk/* dev@kb-test-adm-001.kb.dk:/home/dev/TEST/conf/
echo make scripts executable
ssh dev@kb-test-adm-001.kb.dk "chmod +x /home/dev/TEST/conf/*.sh "
echo make password files readonly
ssh dev@kb-test-adm-001.kb.dk "chmod 400 /home/dev/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-010.bitarkiv.kb.dk
echo copying null.zip to:kb-test-bar-010.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-010.bitarkiv.kb.dk:
echo unzipping null.zip at:kb-test-bar-010.bitarkiv.kb.dk
ssh dev@kb-test-bar-010.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo copying settings and scripts
scp -r kb-test-bar-010.bitarkiv.kb.dk/* dev@kb-test-bar-010.bitarkiv.kb.dk:""c:\\Documents and Settings\\dev\\TEST\\conf\\""
echo make scripts executable
echo make password files readonly
echo Y | ssh dev@kb-test-bar-010.bitarkiv.kb.dk cmd /c cacls ""c:\\Documents and Settings\\dev\\TEST\\conf\\jmxremote.password"" /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-011.bitarkiv.kb.dk
echo copying null.zip to:kb-test-bar-011.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-011.bitarkiv.kb.dk:
echo unzipping null.zip at:kb-test-bar-011.bitarkiv.kb.dk
ssh dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo copying settings and scripts
scp -r kb-test-bar-011.bitarkiv.kb.dk/* dev@kb-test-bar-011.bitarkiv.kb.dk:""c:\\Documents and Settings\\dev\\TEST\\conf\\""
echo make scripts executable
echo make password files readonly
echo Y | ssh dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c cacls ""c:\\Documents and Settings\\dev\\TEST\\conf\\jmxremote.password"" /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-012.bitarkiv.kb.dk
echo copying null.zip to:kb-test-bar-012.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-012.bitarkiv.kb.dk:
echo unzipping null.zip at:kb-test-bar-012.bitarkiv.kb.dk
ssh dev@kb-test-bar-012.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo copying settings and scripts
scp -r kb-test-bar-012.bitarkiv.kb.dk/* dev@kb-test-bar-012.bitarkiv.kb.dk:""c:\\Documents and Settings\\dev\\TEST\\conf\\""
echo make scripts executable
echo make password files readonly
echo Y | ssh dev@kb-test-bar-012.bitarkiv.kb.dk cmd /c cacls ""c:\\Documents and Settings\\dev\\TEST\\conf\\jmxremote.password"" /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-013.bitarkiv.kb.dk
echo copying null.zip to:kb-test-bar-013.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-013.bitarkiv.kb.dk:
echo unzipping null.zip at:kb-test-bar-013.bitarkiv.kb.dk
ssh dev@kb-test-bar-013.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo copying settings and scripts
scp -r kb-test-bar-013.bitarkiv.kb.dk/* dev@kb-test-bar-013.bitarkiv.kb.dk:""c:\\Documents and Settings\\dev\\TEST\\conf\\""
echo make scripts executable
echo make password files readonly
echo Y | ssh dev@kb-test-bar-013.bitarkiv.kb.dk cmd /c cacls ""c:\\Documents and Settings\\dev\\TEST\\conf\\jmxremote.password"" /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-014.bitarkiv.kb.dk
echo copying null.zip to:kb-test-bar-014.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-014.bitarkiv.kb.dk:
echo unzipping null.zip at:kb-test-bar-014.bitarkiv.kb.dk
ssh dev@kb-test-bar-014.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo copying settings and scripts
scp -r kb-test-bar-014.bitarkiv.kb.dk/* dev@kb-test-bar-014.bitarkiv.kb.dk:""c:\\Documents and Settings\\dev\\TEST\\conf\\""
echo make scripts executable
echo make password files readonly
echo Y | ssh dev@kb-test-bar-014.bitarkiv.kb.dk cmd /c cacls ""c:\\Documents and Settings\\dev\\TEST\\conf\\jmxremote.password"" /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-har-001.kb.dk
echo copying null.zip to:kb-test-har-001.kb.dk
scp null.zip dev@kb-test-har-001.kb.dk:/home/dev
echo unzipping null.zip at:kb-test-har-001.kb.dk
ssh dev@kb-test-har-001.kb.dk unzip -q -o /home/dev/null.zip -d /home/dev/TEST
echo copying settings and scripts
scp -r kb-test-har-001.kb.dk/* dev@kb-test-har-001.kb.dk:/home/dev/TEST/conf/
echo make scripts executable
ssh dev@kb-test-har-001.kb.dk "chmod +x /home/dev/TEST/conf/*.sh "
echo make password files readonly
ssh dev@kb-test-har-001.kb.dk "chmod 400 /home/dev/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-har-002.kb.dk
echo copying null.zip to:kb-test-har-002.kb.dk
scp null.zip dev@kb-test-har-002.kb.dk:/home/dev
echo unzipping null.zip at:kb-test-har-002.kb.dk
ssh dev@kb-test-har-002.kb.dk unzip -q -o /home/dev/null.zip -d /home/dev/TEST
echo copying settings and scripts
scp -r kb-test-har-002.kb.dk/* dev@kb-test-har-002.kb.dk:/home/dev/TEST/conf/
echo make scripts executable
ssh dev@kb-test-har-002.kb.dk "chmod +x /home/dev/TEST/conf/*.sh "
echo make password files readonly
ssh dev@kb-test-har-002.kb.dk "chmod 400 /home/dev/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-acs-001.kb.dk
echo copying null.zip to:kb-test-acs-001.kb.dk
scp null.zip dev@kb-test-acs-001.kb.dk:/home/dev
echo unzipping null.zip at:kb-test-acs-001.kb.dk
ssh dev@kb-test-acs-001.kb.dk unzip -q -o /home/dev/null.zip -d /home/dev/TEST
echo copying settings and scripts
scp -r kb-test-acs-001.kb.dk/* dev@kb-test-acs-001.kb.dk:/home/dev/TEST/conf/
echo make scripts executable
ssh dev@kb-test-acs-001.kb.dk "chmod +x /home/dev/TEST/conf/*.sh "
echo make password files readonly
ssh dev@kb-test-acs-001.kb.dk "chmod 400 /home/dev/TEST/conf/jmxremote.password"
echo --------------------------------------------
