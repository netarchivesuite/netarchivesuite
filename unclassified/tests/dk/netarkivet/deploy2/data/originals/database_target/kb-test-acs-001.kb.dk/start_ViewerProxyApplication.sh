echo START LINUX APPLICATION: ViewerProxyApplication
#!/bin/bash
export CLASSPATH=/home/test/TEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/TEST/lib/dk.netarkivet.archive.jar:/home/test/TEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/TEST
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/TEST/conf/settings_ViewerProxyApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/TEST/conf/log_ViewerProxyApplication.prop -Djava.security.manager -Djava.security.policy=/home/test/TEST/conf/security.policy dk.netarkivet.viewerproxy.ViewerProxyApplication < /dev/null > start_ViewerProxyApplication.sh.log 2>&1 &
