#echo START WINDOWS APPLICATION: BitarchiveApplication
cd "c:\Documents and Settings\ba-test\test"
if Exist .\conf\kill_ps_BitarchiveApplication.bat GOTO NOSTART
GOTO START

:START
cscript .\conf\start_BitarchiveApplication.vbs
GOTO DONE

:NOSTART
echo Cannot start. Application already running.

:DONE
