@echo off
REM -----------------------------------------------------------------------------
REM Gradle startup script for Windows
REM -----------------------------------------------------------------------------
set DIRNAME=%~dp0nset PRG=%~dp0
"%DIRNAME%\gradle\wrapper\gradle-wrapper.jar" %*
