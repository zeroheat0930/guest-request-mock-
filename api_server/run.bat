@echo off
setlocal

set "JAVA_HOME=C:\tools\jdk-17.0.18+8"
set "M2_HOME=C:\tools\apache-maven-3.9.14"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

cd /d "%~dp0"

if exist "env.local.bat" (
    echo [run.bat] loading env.local.bat
    call "env.local.bat"
) else (
    echo [run.bat] env.local.bat not found - LLM proxy disabled
)

echo [run.bat] JAVA_HOME=%JAVA_HOME%
echo [run.bat] M2_HOME=%M2_HOME%
java -version
echo.
call mvn spring-boot:run

endlocal
