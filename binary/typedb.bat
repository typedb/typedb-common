@echo off
REM Copyright (C) 2021 Vaticle
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

SET "TYPEDB_HOME=%cd%"


where java >NUL 2>NUL
if %ERRORLEVEL% GEQ 1 (
    echo Java is not installed on this machine.
    echo TypeDB needs Java 11+ in order to run. See the following setup guide: http://docs.vaticle.com/docs/get-started/setup-guide
    pause
    exit 1
)


if "%1" == "" goto missingargument

if "%1" == "console" goto startconsole
if "%1" == "cluster" goto startcluster
if "%1" == "server"  goto startserver

echo   Invalid argument: %1. Possible commands are:
echo   Server:          typedb server [--help]
echo   Cluster:         typedb cluster [--help]
echo   Console:         typedb console [--help]
goto exiterror

:missingargument

 echo   Missing argument. Possible commands are:
 echo   Server:          typedb server [--help]
 echo   Cluster:         typedb cluster [--help]
 echo   Console:         typedb console [--help]

goto exiterror

:startconsole

set "G_CP=%TYPEDB_HOME%\console\conf\;%TYPEDB_HOME%\console\lib\*"
if exist .\console\ (
  java %CONSOLE_JAVAOPTS% -cp "%G_CP%" -Dtypedb.dir="%TYPEDB_HOME%" com.vaticle.typedb.console.TypeDBConsole %2 %3 %4 %5 %6 %7 %8 %9
  goto exit
) else (
  echo TypeDB Console is not included in this TypeDB distribution^.
  echo You may want to install TypeDB Console or TypeDB ^(all^)^.
  goto exiterror
)

:startserver

set "G_CP=%TYPEDB_HOME%\server\conf\;%TYPEDB_HOME%\server\lib\common\*;%TYPEDB_HOME%\server\lib\prod\*"


if exist .\server\ (
  java %SERVER_JAVAOPTS% -cp "%G_CP%" -Dtypedb.dir="%TYPEDB_HOME%" com.vaticle.typedb.core.server.TypeDBServer %2 %3 %4 %5 %6 %7 %8 %9
  goto exit
) else (
  echo TypeDB Server is not included in this TypeDB distribution^.
  echo You may want to install TypeDB Server or TypeDB ^(all^)^.
  goto exiterror
)

:startcluster

set "G_CP=%TYPEDB_HOME%\server\conf\;%TYPEDB_HOME%\server\lib\common\*;%TYPEDB_HOME%\server\lib\prod\*"

if exist .\server\ (
  java %SERVER_JAVAOPTS% -cp "%G_CP%" -Dtypedb.dir="%TYPEDB_HOME%" com.vaticle.typedb.cluster.server.TypeDBClusterServer %2 %3 %4 %5 %6 %7 %8 %9
  goto exit
) else (
  echo TypeDB Cluster is not included in this TypeDB distribution^.
  echo You may want to install TypeDB Cluster or TypeDB Cluster ^(all^)^.
  goto exiterror
)


:exit
exit /b 0

:exiterror
exit /b 1
