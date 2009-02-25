echo Killing linux application: ArcRepositoryApplication
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.archive.arcrepository.ArcRepositoryApplication | grep -v grep | grep /home/test/test/conf/settings_ArcRepositoryApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
