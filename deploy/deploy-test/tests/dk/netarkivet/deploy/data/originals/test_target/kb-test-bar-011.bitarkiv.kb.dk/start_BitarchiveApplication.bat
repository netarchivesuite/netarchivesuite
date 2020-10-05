cd "c:\Documents and Settings\ba-test\test"
if exist .\conf\running_BitarchiveApplication goto NOSTART
goto START

:START
cscript .\conf\start_BitarchiveApplication.vbs
goto DONE

:NOSTART
echo Cannot start. Application already running.

:DONE
