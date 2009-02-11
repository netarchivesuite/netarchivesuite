ECHO Killing windows application: BitarchiveApplication
CD "c:\Documents and Settings\dev\TEST\conf"
IF EXIST running_BitarchiveApplication GOTO KILL
GOTO NOKILL

:KILL
"C:\Program Files\Bitvise WinSSHD\bvRun" -brj -new -cmd="kill_ps_BitarchiveApplication.bat"
DEL running_BitarchiveApplication
GOTO DONE

:NOKILL
ECHO Cannot kill application. Is not running.

:DONE
