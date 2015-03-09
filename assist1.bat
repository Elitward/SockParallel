::::::::::::::::::: BROWSER :::::::::::::::::::
call ipsetting.bat

cd .\bin

:: assist
start java net.sockparallel.Main mix 2002 %server_ip%:2002

cd ..

::pause
