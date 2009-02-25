echo Killing linux application: ViewerProxyApplication
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.viewerproxy.ViewerProxyApplication | grep -v grep | grep /home/netarkiv/test/conf/settings_ViewerProxyApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
