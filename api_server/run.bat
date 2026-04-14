@echo off
REM 이 배치 파일 안에서만 JAVA_HOME / PATH / 시크릿 override (시스템/다른 프로세스 영향 없음)
setlocal

set "JAVA_HOME=C:\tools\jdk-17.0.18+8"
set "M2_HOME=C:\tools\apache-maven-3.9.14"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

cd /d "%~dp0"

REM 로컬 시크릿 로드 (gitignore 대상). env.local.bat 안에 set ANTHROPIC_API_KEY=... 형태로 저장.
if exist "env.local.bat" (
    echo [run.bat] loading env.local.bat
    call "env.local.bat"
) else (
    echo [run.bat] env.local.bat not found - LLM proxy will run in disabled mode
)

echo [run.bat] JAVA_HOME=%JAVA_HOME%
echo [run.bat] M2_HOME=%M2_HOME%
java -version
echo.
call mvn spring-boot:run

endlocal
