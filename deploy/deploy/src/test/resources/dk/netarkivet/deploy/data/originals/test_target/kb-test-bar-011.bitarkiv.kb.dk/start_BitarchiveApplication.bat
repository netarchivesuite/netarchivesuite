cd "c:\Documents and Settings\ba-test\test"
IF EXIST .\conf\running_BitarchiveApplication GOTO NOSTART
GOTO START

:START
cscript .\conf\start_BitarchiveApplication.vbs
GOTO DONE

:NOSTART
echo Cannot start. Application already running.

:DONE
