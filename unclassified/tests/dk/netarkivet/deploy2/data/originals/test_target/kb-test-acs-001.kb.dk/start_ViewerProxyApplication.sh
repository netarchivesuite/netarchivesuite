echo START LINUX APPLICATION: ViewerProxyApplication
#!/bin/bash
export CLASSPATH=/home/test/test/lib/dk.netarkivet.viewerproxy.jar:/home/test/test/lib/dk.netarkivet.archive.jar:/home/test/test/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/test
java -Xmx1536m  -Ddk.netarkivet.settings.file=/home/test/test/conf/settings_ViewerProxyApplication.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/test/conf/log_ViewerProxyApplication.prop -Djava.security.manager -Djava.security.policy=/home/test/test/conf/security.policy dk.netarkivet.viewerproxy.ViewerProxyApplication < /dev/null > start_ViewerProxyApplication.sh.log 2>&1 &
