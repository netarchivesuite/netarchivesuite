ECHO Killing windows application: BitarchiveApplication
cd "c:\Documents and Settings\ba-test\test\conf\"
if exist running_BitarchiveApplication goto KILL
goto NOKILL

:KILL
"C:\Program Files\Bitvise WinSSHD\bvRun" -brj -new -cmd="kill_ps_BitarchiveApplication.bat"
del running_BitarchiveApplication
goto DONE

:NOKILL
ECHO Cannot kill application. Is not running.

:DONE
