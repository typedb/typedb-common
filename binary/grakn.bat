@echo off
REM
REM GRAKN.AI - THE KNOWLEDGE GRAPH
REM Copyright (C) 2020 Grakn Labs
REM
REM This program is free software: you can redistribute it and/or modify
REM it under the terms of the GNU Affero General Public License as
REM published by the Free Software Foundation, either version 3 of the
REM License, or (at your option) any later version.
REM
REM This program is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM GNU Affero General Public License for more details.
REM
REM You should have received a copy of the GNU Affero General Public License
REM along with this program.  If not, see <https://www.gnu.org/licenses/>.
REM

SET "GRAKN_HOME=%cd%"


SET "GRAKN_CONFIG=server\conf\grakn.properties"

where java >NUL 2>NUL
if %ERRORLEVEL% GEQ 1 (
    echo Java is not installed on this machine.
    echo Grakn needs Java 1.8 in order to run. See the following setup guide: http://dev.grakn.ai/docs/get-started/setup-guide
    pause
    exit 1
)


if "%1" == "" goto missingargument

if "%1" == "console" goto startconsole

if "%1" == "server" goto startserver

echo   Invalid argument: %1. Possible commands are:
echo   Server:          grakn server [--help]
echo   Console:         grakn console [--help]
goto exiterror

:missingargument

 echo   Missing argument. Possible commands are:
 echo   Server:          grakn server [--help]
 echo   Console:         grakn console [--help]

goto exiterror

:startconsole

set "G_CP=%GRAKN_HOME%\console\conf\;%GRAKN_HOME%\console\services\lib\*"
if exist .\console\services\lib\io-grakn-console-grakn-console-*.jar (
  java %CONSOLE_JAVAOPTS% -cp "%G_CP%" -Dgrakn.dir="%GRAKN_HOME%" grakn.console.GraknConsole %*
  goto exit
) else (
  echo Grakn Core Console is not included in this Grakn distribution^.
  echo You may want to install Grakn Core Console or Grakn Core ^(all^)^.
  goto exiterror
)

:startserver

set "G_CP=%GRAKN_HOME%\server\conf\;%GRAKN_HOME%\server\services\lib\*"
if exist .\server\services\lib\io-grakn-core-grakn-server-*.jar (
  java %GRAKN_DAEMON_JAVAOPTS% -cp "%G_CP%" -Dgrakn.dir="%GRAKN_HOME%" -Dgrakn.conf="%GRAKN_HOME%\%GRAKN_CONFIG%" -Dstorage.javaopts="%STORAGE_JAVAOPTS%" -Dserver.javaopts="%SERVER_JAVAOPTS%" grakn.core.daemon.GraknDaemon %*
  goto exit
) else (
  echo Grakn Core Server is not included in this Grakn distribution^.
  echo You may want to install Grakn Core Server or Grakn Core ^(all^)^.
  goto exiterror
)

:exit
exit /b 0

:exiterror
exit /b 1
