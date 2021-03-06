@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  xd-container startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and SPRING_XD_CONTAINER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0

set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

@echo off
set APP_HOME_LIB=%APP_HOME%\lib
set HADOOP_DISTRO=hadoop12
if exist "%APP_HOME_LIB%" (
    setLocal EnableDelayedExpansion
    set found=0
    for %%a in (%CMD_LINE_ARGS%) do (
        if !found!==1 set HADOOP_DISTRO=%%a
        if "%%a"=="--hadoopDistro" (
            set found=1
        ) else (
            set found=0
        )
    )
    set CLASSPATH=%APP_HOME%\modules\processor\scripts;%APP_HOME%\config
    set CLASSPATH=!CLASSPATH!;%APP_HOME_LIB%\*
    set HADOOP_LIB=%APP_HOME%\lib\!HADOOP_DISTRO!
    if exist "!HADOOP_LIB!" (
        set CLASSPATH=!CLASSPATH!;!HADOOP_LIB!\*
    )
)

@rem Set XD_HOME to APP_HOME if XD_HOME is not defined yet
if not exist "%XD_HOME%" (
    set XD_HOME=%APP_HOME%
)

@rem Execute xd-container
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% -DXD_CONTAINER=container -Dlog4j.configuration=file:///%XD_HOME%/config/xd-container-logger.properties -classpath "%CLASSPATH%" org.springframework.xd.dirt.server.ContainerMain %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SPRING_XD_CONTAINER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SPRING_XD_CONTAINER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
