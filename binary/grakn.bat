@echo off
REM Copyright (C) 2021 Grakn Labs
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

if "%1" == "server"  goto startserver
if "%1" == "version" goto startserver

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

set "G_CP=%GRAKN_HOME%\console\conf\;%GRAKN_HOME%\console\lib\*"
if exist .\console\ (
  java %CONSOLE_JAVAOPTS% -cp "%G_CP%" -Dgrakn.dir="%GRAKN_HOME%" grakn.console.GraknConsole %2 %3 %4 %5 %6 %7 %8 %9
  goto exit
) else (
  echo Grakn Core Console is not included in this Grakn distribution^.
  echo You may want to install Grakn Core Console or Grakn Core ^(all^)^.
  goto exiterror
)

:startserver

set "G_CP=%GRAKN_HOME%\server\conf\;%GRAKN_HOME%\server\lib\common\*;%GRAKN_HOME%\server\lib\prod\*"


if exist .\server\ (
  java %SERVER_JAVAOPTS% -cp "%G_CP%" -Dgrakn.dir="%GRAKN_HOME%" -Dgrakn.conf="%GRAKN_HOME%\%GRAKN_CONFIG%" grakn.core.server.GraknServer %2 %3 %4 %5 %6 %7 %8 %9
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
