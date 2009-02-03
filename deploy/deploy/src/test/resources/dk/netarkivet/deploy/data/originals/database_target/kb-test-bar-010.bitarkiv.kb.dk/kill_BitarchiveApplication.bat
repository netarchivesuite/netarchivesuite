ECHO Killing windows application: BitarchiveApplication
CD "c:\Documents and Settings\ba-test\TEST\conf"
IF EXIST kill_ps_BitarchiveApplication.bat GOTO KILL
GOTO NOKILL

:KILL
"C:\Program Files\Bitvise WinSSHD\bvRun" -brj -new -cmd="kill_ps_BitarchiveApplication.bat"
DEL kill_ps_BitarchiveApplication.bat
GOTO DONE

:NOKILL
ECHO Cannot kill application. Is not running.

:DONE
