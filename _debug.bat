::::::::::::::::::: SERVER :::::::::::::::::::
cd .\bin
:: 1 socket
:: start java net.sockparallel.Main mix 2001 remote.mss.icics.ubc.ca:22

:: 5 socket
:: start java net.sockparallel.Main mix 2001 2002 2003 2004 2005 remote.mss.icics.ubc.ca:22

:: assist
:: start java net.sockparallel.Main mix 2001 2002 remote.mss.icics.ubc.ca:22



::MIX at end
start java net.sockparallel.Main mix 2001 2002 remote.mss.icics.ubc.ca:22

::FORWARD
start java net.sockparallel.Main mix 1001 2001

::SPLIT at start
start java net.sockparallel.Main split 22 1001 2002

cd ..




