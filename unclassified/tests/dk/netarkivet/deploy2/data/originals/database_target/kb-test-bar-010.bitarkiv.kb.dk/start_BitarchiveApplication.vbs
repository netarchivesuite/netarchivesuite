Set WshShell= CreateObject("WScript.Shell")
Set oExec = WshShell.exec( "java -Xmx1150m  -classpath ""c:\\Documents and Settings\\ba-test\\TEST\\lib\\dk.netarkivet.archive.jar;c:\\Documents and Settings\\ba-test\\TEST\\lib\\dk.netarkivet.viewerproxy.jar;c:\\Documents and Settings\\ba-test\\TEST\\lib\\dk.netarkivet.monitor.jar;"" -Ddk.netarkivet.settings.file=""c:\\Documents and Settings\\ba-test\\TEST\\conf\\settings_BitarchiveApplication.xml"" -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=""c:\\Documents and Settings\\ba-test\\TEST\\conf\\log_BitarchiveApplication.prop"" -Djava.security.manager -Djava.security.policy=""c:\\Documents and Settings\\ba-test\\TEST\\conf\\security.policy"" dk.netarkivet.archive.bitarchive.BitarchiveApplication")
set fso= CreateObject("Scripting.FileSystemObject")
set f=fso.OpenTextFile(".\conf\kill_BitarchiveApplication.bat",2,True)
f.WriteLine "taskkill /F /PID " & oExec.ProcessID
f.close
