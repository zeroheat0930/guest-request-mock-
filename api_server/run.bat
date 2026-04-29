@echo off
setlocal

set "JAVA_HOME=C:\tools\jdk-17.0.18+8"
set "M2_HOME=C:\tools\apache-maven-3.9.14"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

cd /d "%~dp0"

REM cmd 가 NoDefaultCurrentDirectoryInExePath=1 환경에서 현재 디렉토리의 batch 를
REM 명령어로 검색하지 않으므로 .\ prefix 또는 절대경로로 호출해야 함.
if not exist "%~dp0env.local.bat" goto :nolocal
echo [run.bat] loading env.local.bat
call "%~dp0env.local.bat"
goto :endlocal_check
:nolocal
echo [run.bat] env.local.bat not found - LLM proxy disabled
:endlocal_check

echo [run.bat] JAVA_HOME=%JAVA_HOME%
echo [run.bat] M2_HOME=%M2_HOME%
java -version
echo.
REM spring-boot-maven-plugin 의 fork 모드는 OS env 전파가 안정적이지 않아
REM DB_USER/DB_PASSWORD 를 system property 로 직접 박아서 application.yml 의 ${DB_USER}/${DB_PASSWORD} 우회
call mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.datasource.username=%DB_USER% -Dspring.datasource.password=%DB_PASSWORD% -Danthropic.api-key=%ANTHROPIC_API_KEY%"

endlocal
