Set WshShell= CreateObject("WScript.Shell")
Set oExec = WshShell.exec( "java -Xmx1150m -classpath ""c:\\Documents and Settings\\dev\\UNITTEST\\lib\\dk.netarkivet.archive.jar;c:\\Documents and Settings\\dev\\UNITTEST\\lib\\dk.netarkivet.viewerproxy.jar;c:\\Documents and Settings\\dev\\UNITTEST\\lib\\dk.netarkivet.monitor.jar;"" -Ddk.netarkivet.settings.file=""c:\\Documents and Settings\\dev\\UNITTEST\\conf\\settings.xml"" -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=""c:\\Documents and Settings\\dev\\UNITTEST\\conf\\log_bitarchiveapplication.prop"" -Dsettings.common.jmx.port=8100 -Dsettings.common.jmx.rmiPort=8200 -Dsettings.common.jmx.passwordFile=""c:\\Documents and Settings\\dev\\UNITTEST\\conf\\jmxremote.password"" dk.netarkivet.archive.bitarchive.BitarchiveApplication")
Set fso= CreateObject("Scripting.FileSystemObject")
set f=fso.OpenTextFile(".\conf\kill_bitarchive.bat",2,True)
f.WriteLine "taskkill /F /PID " & oExec.ProcessID 
f.close
