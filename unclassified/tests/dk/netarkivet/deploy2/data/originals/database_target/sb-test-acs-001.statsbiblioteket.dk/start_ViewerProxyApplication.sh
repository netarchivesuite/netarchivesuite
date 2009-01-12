echo START LINUX APPLICATION: ViewerProxyApplication
#!/bin/bash
export CLASSPATH=/home/netarkiv/TEST/lib/dk.netarkivet.archive.jar:/home/netarkiv/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/netarkiv/TEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/netarkiv/TEST/conf/settings_ViewerProxyApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/netarkiv/TEST/conf/log_ViewerProxyApplication.prop -Djava.security.manager -Djava.security.policy=/home/netarkiv/TEST/conf/security.policy dk.netarkivet.viewerproxy.ViewerProxyApplication < /dev/null > start_ViewerProxyApplication.sh.log 2>&1 &
