@echo off
set /a counter = 0
:Loop
set /a counter +=1
echo %counter%
if not (%counter%)==(1000) goto Loop
echo That's all folks