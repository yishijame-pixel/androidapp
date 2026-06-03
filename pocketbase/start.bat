@echo off
REM FunLife PocketBase 一键启动（Windows）
cd /d %~dp0
echo PocketBase API:  http://127.0.0.1:8090/api/
echo Admin Dashboard: http://127.0.0.1:8090/_/
pocketbase.exe serve --http=0.0.0.0:8090
