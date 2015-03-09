::::::::::::::::::: BROWSER :::::::::::::::::::
call ipsetting.bat

cd .\bin
:: 1 socket
::start java net.sockparallel.Main split 22 %server_ip%:2001

:: 5 socket
::start java net.sockparallel.Main split 22 %server_ip%:2001 %server_ip%:2002 %server_ip%:2003 %server_ip%:2004 %server_ip%:2005

:: assist
start java net.sockparallel.Main split 22 %server_ip%:2001 %assist1_ip%:2002

cd ..

::pause
