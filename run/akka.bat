@echo off

set AKKA_HOME=%~dp0..
set JAVA_OPTS=-Xmx1024M -Xms1024M -Xss1M -XX:MaxPermSize=256M -XX:+UseParallelGC 
set AKKA_CLASSPATH=%AKKA_HOME%\lib\tooe\*;%AKKA_HOME%\config;%AKKA_HOME%\lib\akka\*

java %JAVA_OPTS% -cp "%AKKA_CLASSPATH%" -Dakka.home="%AKKA_HOME%" -Dconfig.file=%AKKA_HOME%\config\dev_application.conf -DCONFIG_ROOT=%AKKA_HOME%\config akka.kernel.Main %* 
