@echo off
REM 이 배치 파일 안에서만 JAVA_HOME / PATH override (시스템 환경 영향 없음)
setlocal

set "JAVA_HOME=C:\tools\jdk-17.0.18+8"
set "M2_HOME=C:\tools\apache-maven-3.9.14"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

cd /d "%~dp0"

echo [run.bat] JAVA_HOME=%JAVA_HOME%
echo [run.bat] M2_HOME=%M2_HOME%
java -version
echo.
call mvn spring-boot:run

endlocal
