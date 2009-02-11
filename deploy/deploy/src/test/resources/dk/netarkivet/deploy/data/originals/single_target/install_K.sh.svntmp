#!/bin/bash
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-adm-001.kb.dk
echo copying null.zip to:kb-test-adm-001.kb.dk
scp null.zip dev@kb-test-adm-001.kb.dk:/home/dev
echo unzipping null.zip at:kb-test-adm-001.kb.dk
ssh dev@kb-test-adm-001.kb.dk unzip -q -o /home/dev/null.zip -d /home/dev/TEST
echo Creating directories.
ssh dev@kb-test-adm-001.kb.dk "cd /home/dev/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo preparing for copying of settings and scripts
ssh dev@kb-test-adm-001.kb.dk " cd ~; if [ -e /home/dev/TEST/conf/jmxremote.password ]; then chmod u+rwx /home/dev/TEST/conf/jmxremote.password; fi; "
echo copying settings and scripts
scp -r kb-test-adm-001.kb.dk/* dev@kb-test-adm-001.kb.dk:/home/dev/TEST/conf/
echo make scripts executable
ssh dev@kb-test-adm-001.kb.dk "chmod 700 /home/dev/TEST/conf/*.sh "
echo make password files readonly
ssh dev@kb-test-adm-001.kb.dk "chmod 400 /home/dev/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-010.bitarkiv.kb.dk
echo copying null.zip to: kb-test-bar-010.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-010.bitarkiv.kb.dk:
echo unzipping null.zip at: kb-test-bar-010.bitarkiv.kb.dk
ssh dev@kb-test-bar-010.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo Creating directories.
scp dir_kb-test-bar-010.bitarkiv.kb.dk.bat dev@kb-test-bar-010.bitarkiv.kb.dk:
ssh  dev@kb-test-bar-010.bitarkiv.kb.dk cmd /c dir_kb-test-bar-010.bitarkiv.kb.dk.bat
ssh  dev@kb-test-bar-010.bitarkiv.kb.dk cmd /c del dir_kb-test-bar-010.bitarkiv.kb.dk.bat
echo preparing for copying of settings and scripts
if [ $(ssh dev@kb-test-bar-010.bitarkiv.kb.dk cmd /c if exist TEST\\conf\\jmxremote.password echo 1 ) ]; then echo Y | ssh dev@kb-test-bar-010.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:F; fi;
echo copying settings and scripts
scp -r kb-test-bar-010.bitarkiv.kb.dk/* dev@kb-test-bar-010.bitarkiv.kb.dk:TEST\\conf\\
echo Database not implemented for windows.
echo make password files readonly
echo Y | ssh dev@kb-test-bar-010.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-011.bitarkiv.kb.dk
echo copying null.zip to: kb-test-bar-011.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-011.bitarkiv.kb.dk:
echo unzipping null.zip at: kb-test-bar-011.bitarkiv.kb.dk
ssh dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo Creating directories.
scp dir_kb-test-bar-011.bitarkiv.kb.dk.bat dev@kb-test-bar-011.bitarkiv.kb.dk:
ssh  dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c dir_kb-test-bar-011.bitarkiv.kb.dk.bat
ssh  dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c del dir_kb-test-bar-011.bitarkiv.kb.dk.bat
echo preparing for copying of settings and scripts
if [ $(ssh dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c if exist TEST\\conf\\jmxremote.password echo 1 ) ]; then echo Y | ssh dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:F; fi;
echo copying settings and scripts
scp -r kb-test-bar-011.bitarkiv.kb.dk/* dev@kb-test-bar-011.bitarkiv.kb.dk:TEST\\conf\\
echo Database not implemented for windows.
echo make password files readonly
echo Y | ssh dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-012.bitarkiv.kb.dk
echo copying null.zip to: kb-test-bar-012.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-012.bitarkiv.kb.dk:
echo unzipping null.zip at: kb-test-bar-012.bitarkiv.kb.dk
ssh dev@kb-test-bar-012.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo Creating directories.
scp dir_kb-test-bar-012.bitarkiv.kb.dk.bat dev@kb-test-bar-012.bitarkiv.kb.dk:
ssh  dev@kb-test-bar-012.bitarkiv.kb.dk cmd /c dir_kb-test-bar-012.bitarkiv.kb.dk.bat
ssh  dev@kb-test-bar-012.bitarkiv.kb.dk cmd /c del dir_kb-test-bar-012.bitarkiv.kb.dk.bat
echo preparing for copying of settings and scripts
if [ $(ssh dev@kb-test-bar-012.bitarkiv.kb.dk cmd /c if exist TEST\\conf\\jmxremote.password echo 1 ) ]; then echo Y | ssh dev@kb-test-bar-012.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:F; fi;
echo copying settings and scripts
scp -r kb-test-bar-012.bitarkiv.kb.dk/* dev@kb-test-bar-012.bitarkiv.kb.dk:TEST\\conf\\
echo Database not implemented for windows.
echo make password files readonly
echo Y | ssh dev@kb-test-bar-012.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-013.bitarkiv.kb.dk
echo copying null.zip to: kb-test-bar-013.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-013.bitarkiv.kb.dk:
echo unzipping null.zip at: kb-test-bar-013.bitarkiv.kb.dk
ssh dev@kb-test-bar-013.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo Creating directories.
scp dir_kb-test-bar-013.bitarkiv.kb.dk.bat dev@kb-test-bar-013.bitarkiv.kb.dk:
ssh  dev@kb-test-bar-013.bitarkiv.kb.dk cmd /c dir_kb-test-bar-013.bitarkiv.kb.dk.bat
ssh  dev@kb-test-bar-013.bitarkiv.kb.dk cmd /c del dir_kb-test-bar-013.bitarkiv.kb.dk.bat
echo preparing for copying of settings and scripts
if [ $(ssh dev@kb-test-bar-013.bitarkiv.kb.dk cmd /c if exist TEST\\conf\\jmxremote.password echo 1 ) ]; then echo Y | ssh dev@kb-test-bar-013.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:F; fi;
echo copying settings and scripts
scp -r kb-test-bar-013.bitarkiv.kb.dk/* dev@kb-test-bar-013.bitarkiv.kb.dk:TEST\\conf\\
echo Database not implemented for windows.
echo make password files readonly
echo Y | ssh dev@kb-test-bar-013.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-bar-014.bitarkiv.kb.dk
echo copying null.zip to: kb-test-bar-014.bitarkiv.kb.dk
scp null.zip dev@kb-test-bar-014.bitarkiv.kb.dk:
echo unzipping null.zip at: kb-test-bar-014.bitarkiv.kb.dk
ssh dev@kb-test-bar-014.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST -o null.zip
echo Creating directories.
scp dir_kb-test-bar-014.bitarkiv.kb.dk.bat dev@kb-test-bar-014.bitarkiv.kb.dk:
ssh  dev@kb-test-bar-014.bitarkiv.kb.dk cmd /c dir_kb-test-bar-014.bitarkiv.kb.dk.bat
ssh  dev@kb-test-bar-014.bitarkiv.kb.dk cmd /c del dir_kb-test-bar-014.bitarkiv.kb.dk.bat
echo preparing for copying of settings and scripts
if [ $(ssh dev@kb-test-bar-014.bitarkiv.kb.dk cmd /c if exist TEST\\conf\\jmxremote.password echo 1 ) ]; then echo Y | ssh dev@kb-test-bar-014.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:F; fi;
echo copying settings and scripts
scp -r kb-test-bar-014.bitarkiv.kb.dk/* dev@kb-test-bar-014.bitarkiv.kb.dk:TEST\\conf\\
echo Database not implemented for windows.
echo make password files readonly
echo Y | ssh dev@kb-test-bar-014.bitarkiv.kb.dk cmd /c cacls TEST\\conf\\jmxremote.password /P BITARKIV\\dev:R
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-har-001.kb.dk
echo copying null.zip to:kb-test-har-001.kb.dk
scp null.zip dev@kb-test-har-001.kb.dk:/home/dev
echo unzipping null.zip at:kb-test-har-001.kb.dk
ssh dev@kb-test-har-001.kb.dk unzip -q -o /home/dev/null.zip -d /home/dev/TEST
echo Creating directories.
ssh dev@kb-test-har-001.kb.dk "cd /home/dev/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo preparing for copying of settings and scripts
ssh dev@kb-test-har-001.kb.dk " cd ~; if [ -e /home/dev/TEST/conf/jmxremote.password ]; then chmod u+rwx /home/dev/TEST/conf/jmxremote.password; fi; "
echo copying settings and scripts
scp -r kb-test-har-001.kb.dk/* dev@kb-test-har-001.kb.dk:/home/dev/TEST/conf/
echo make scripts executable
ssh dev@kb-test-har-001.kb.dk "chmod 700 /home/dev/TEST/conf/*.sh "
echo make password files readonly
ssh dev@kb-test-har-001.kb.dk "chmod 400 /home/dev/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-har-002.kb.dk
echo copying null.zip to:kb-test-har-002.kb.dk
scp null.zip dev@kb-test-har-002.kb.dk:/home/dev
echo unzipping null.zip at:kb-test-har-002.kb.dk
ssh dev@kb-test-har-002.kb.dk unzip -q -o /home/dev/null.zip -d /home/dev/TEST
echo Creating directories.
ssh dev@kb-test-har-002.kb.dk "cd /home/dev/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo preparing for copying of settings and scripts
ssh dev@kb-test-har-002.kb.dk " cd ~; if [ -e /home/dev/TEST/conf/jmxremote.password ]; then chmod u+rwx /home/dev/TEST/conf/jmxremote.password; fi; "
echo copying settings and scripts
scp -r kb-test-har-002.kb.dk/* dev@kb-test-har-002.kb.dk:/home/dev/TEST/conf/
echo make scripts executable
ssh dev@kb-test-har-002.kb.dk "chmod 700 /home/dev/TEST/conf/*.sh "
echo make password files readonly
ssh dev@kb-test-har-002.kb.dk "chmod 400 /home/dev/TEST/conf/jmxremote.password"
echo --------------------------------------------
echo INSTALLING TO MACHINE: dev@kb-test-acs-001.kb.dk
echo copying null.zip to:kb-test-acs-001.kb.dk
scp null.zip dev@kb-test-acs-001.kb.dk:/home/dev
echo unzipping null.zip at:kb-test-acs-001.kb.dk
ssh dev@kb-test-acs-001.kb.dk unzip -q -o /home/dev/null.zip -d /home/dev/TEST
echo Creating directories.
ssh dev@kb-test-acs-001.kb.dk "cd /home/dev/TEST; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d viewerproxy ]; then mkdir viewerproxy; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d viewerproxy ]; then mkdir viewerproxy; fi; if [ ! -d x:\bitarchive ]; then mkdir x:\bitarchive; fi; if [ ! -d x:\bitarchive/filedir ]; then mkdir x:\bitarchive/filedir; fi; if [ ! -d x:\bitarchive/tempdir ]; then mkdir x:\bitarchive/tempdir; fi; if [ ! -d x:\bitarchive/atticdir ]; then mkdir x:\bitarchive/atticdir; fi; if [ ! -d y:\bitarchive ]; then mkdir y:\bitarchive; fi; if [ ! -d y:\bitarchive/filedir ]; then mkdir y:\bitarchive/filedir; fi; if [ ! -d y:\bitarchive/tempdir ]; then mkdir y:\bitarchive/tempdir; fi; if [ ! -d y:\bitarchive/atticdir ]; then mkdir y:\bitarchive/atticdir; fi; if [ ! -d z:\bitarchive ]; then mkdir z:\bitarchive; fi; if [ ! -d z:\bitarchive/filedir ]; then mkdir z:\bitarchive/filedir; fi; if [ ! -d z:\bitarchive/tempdir ]; then mkdir z:\bitarchive/tempdir; fi; if [ ! -d z:\bitarchive/atticdir ]; then mkdir z:\bitarchive/atticdir; fi; if [ ! -d viewerproxy ]; then mkdir viewerproxy; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo preparing for copying of settings and scripts
ssh dev@kb-test-acs-001.kb.dk " cd ~; if [ -e /home/dev/TEST/conf/jmxremote.password ]; then chmod u+rwx /home/dev/TEST/conf/jmxremote.password; fi; "
echo copying settings and scripts
scp -r kb-test-acs-001.kb.dk/* dev@kb-test-acs-001.kb.dk:/home/dev/TEST/conf/
echo make scripts executable
ssh dev@kb-test-acs-001.kb.dk "chmod 700 /home/dev/TEST/conf/*.sh "
echo make password files readonly
ssh dev@kb-test-acs-001.kb.dk "chmod 400 /home/dev/TEST/conf/jmxremote.password"
echo --------------------------------------------
