#!/bin/bash
echo --------------------------------------------
echo INSTALLING TO MACHINE: test@kb-test-adm-001.kb.dk
echo copying null.zip to:kb-test-adm-001.kb.dk
scp null.zip test@kb-test-adm-001.kb.dk:/home/test
echo unzipping null.zip at:kb-test-adm-001.kb.dk
ssh test@kb-test-adm-001.kb.dk unzip -q -o /home/test/null.zip -d /home/test/TEST
echo Creating directories.
ssh test@kb-test-adm-001.kb.dk "cd /home/test/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo copying settings and scripts
scp -r kb-test-adm-001.kb.dk/* test@kb-test-adm-001.kb.dk:/home/test/TEST/conf/
echo Copying database
scp tests/dk/netarkivet/deploy2/data/working/fullhddb.jar test@kb-test-adm-001.kb.dk:/home/test/TEST/harvestdefinitionbasedir/fullhddb.jar
echo Unzipping database
ssh test@kb-test-adm-001.kb.dk "cd /home/test/TEST; if [ -d harvestDatabase ]; then echo ; else mkdir harvestDatabase; fi; if [ $(ls -A harvestDatabase) ]; then echo The database directory is not empty as required.; else unzip -q -o harvestdefinitionbasedir/fullhddb.jar -d harvestDatabase/.; fi; exit; "
echo make scripts executable
ssh test@kb-test-adm-001.kb.dk "chmod +x /home/test/TEST/conf/*.sh "
echo make password files readonly
ssh test@kb-test-adm-001.kb.dk "chmod 400 /home/test/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: ba-test@kb-test-bar-010.bitarkiv.kb.dk
echo copying null.zip to:kb-test-bar-010.bitarkiv.kb.dk
scp null.zip ba-test@kb-test-bar-010.bitarkiv.kb.dk:
echo unzipping null.zip at:kb-test-bar-010.bitarkiv.kb.dk
ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo Creating directories.
scp make_dir_kb-test-bar-010.bitarkiv.kb.dk.bat ba-test@kb-test-bar-010.bitarkiv.kb.dk:
ssh  ba-test@kb-test-bar-010.bitarkiv.kb.dk CMD /C make_dir_kb-test-bar-010.bitarkiv.kb.dk.bat
ssh  ba-test@kb-test-bar-010.bitarkiv.kb.dk DEL make_dir_kb-test-bar-010.bitarkiv.kb.dk.bat
echo copying settings and scripts
scp -r kb-test-bar-010.bitarkiv.kb.dk/* ba-test@kb-test-bar-010.bitarkiv.kb.dk:""c:\\Documents and Settings\\ba-test\\TEST\\conf\\""
echo Database not implemented for windows.
echo make password files readonly
echo Y | ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c cacls ""c:\\Documents and Settings\\ba-test\\TEST\\conf\\jmxremote.password"" /P BITARKIV\\ba-test:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: ba-test@kb-test-bar-011.bitarkiv.kb.dk
echo copying null.zip to:kb-test-bar-011.bitarkiv.kb.dk
scp null.zip ba-test@kb-test-bar-011.bitarkiv.kb.dk:
echo unzipping null.zip at:kb-test-bar-011.bitarkiv.kb.dk
ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo Creating directories.
scp make_dir_kb-test-bar-011.bitarkiv.kb.dk.bat ba-test@kb-test-bar-011.bitarkiv.kb.dk:
ssh  ba-test@kb-test-bar-011.bitarkiv.kb.dk CMD /C make_dir_kb-test-bar-011.bitarkiv.kb.dk.bat
ssh  ba-test@kb-test-bar-011.bitarkiv.kb.dk DEL make_dir_kb-test-bar-011.bitarkiv.kb.dk.bat
echo copying settings and scripts
scp -r kb-test-bar-011.bitarkiv.kb.dk/* ba-test@kb-test-bar-011.bitarkiv.kb.dk:""c:\\Documents and Settings\\ba-test\\TEST\\conf\\""
echo Database not implemented for windows.
echo make password files readonly
echo Y | ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c cacls ""c:\\Documents and Settings\\ba-test\\TEST\\conf\\jmxremote.password"" /P BITARKIV\\ba-test:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: test@kb-test-har-001.kb.dk
echo copying null.zip to:kb-test-har-001.kb.dk
scp null.zip test@kb-test-har-001.kb.dk:/home/test
echo unzipping null.zip at:kb-test-har-001.kb.dk
ssh test@kb-test-har-001.kb.dk unzip -q -o /home/test/null.zip -d /home/test/TEST
echo Creating directories.
ssh test@kb-test-har-001.kb.dk "cd /home/test/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo copying settings and scripts
scp -r kb-test-har-001.kb.dk/* test@kb-test-har-001.kb.dk:/home/test/TEST/conf/
echo make scripts executable
ssh test@kb-test-har-001.kb.dk "chmod +x /home/test/TEST/conf/*.sh "
echo make password files readonly
ssh test@kb-test-har-001.kb.dk "chmod 400 /home/test/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: test@kb-test-har-002.kb.dk
echo copying null.zip to:kb-test-har-002.kb.dk
scp null.zip test@kb-test-har-002.kb.dk:/home/test
echo unzipping null.zip at:kb-test-har-002.kb.dk
ssh test@kb-test-har-002.kb.dk unzip -q -o /home/test/null.zip -d /home/test/TEST
echo Creating directories.
ssh test@kb-test-har-002.kb.dk "cd /home/test/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d harvester_high ]; then mkdir harvester_high; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo copying settings and scripts
scp -r kb-test-har-002.kb.dk/* test@kb-test-har-002.kb.dk:/home/test/TEST/conf/
echo make scripts executable
ssh test@kb-test-har-002.kb.dk "chmod +x /home/test/TEST/conf/*.sh "
echo make password files readonly
ssh test@kb-test-har-002.kb.dk "chmod 400 /home/test/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: test@kb-test-acs-001.kb.dk
echo copying null.zip to:kb-test-acs-001.kb.dk
scp null.zip test@kb-test-acs-001.kb.dk:/home/test
echo unzipping null.zip at:kb-test-acs-001.kb.dk
ssh test@kb-test-acs-001.kb.dk unzip -q -o /home/test/null.zip -d /home/test/TEST
echo Creating directories.
ssh test@kb-test-acs-001.kb.dk "cd /home/test/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo copying settings and scripts
scp -r kb-test-acs-001.kb.dk/* test@kb-test-acs-001.kb.dk:/home/test/TEST/conf/
echo make scripts executable
ssh test@kb-test-acs-001.kb.dk "chmod +x /home/test/TEST/conf/*.sh "
echo make password files readonly
ssh test@kb-test-acs-001.kb.dk "chmod 400 /home/test/TEST/conf/jmxremote.password"
echo --------------------------------------------
