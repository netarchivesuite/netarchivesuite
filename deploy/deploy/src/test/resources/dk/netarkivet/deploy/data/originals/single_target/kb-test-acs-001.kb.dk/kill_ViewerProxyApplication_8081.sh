echo Killing linux application.
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.viewerproxy.ViewerProxyApplication | grep -v grep | grep /home/dev/TEST/conf/settings_ViewerProxyApplication_8081.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
