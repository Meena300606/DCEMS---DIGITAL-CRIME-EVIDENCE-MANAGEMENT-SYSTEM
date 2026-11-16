@echo off
echo Starting DCEMS ... (make sure MySQL is running and db.properties is correct)
call mvn -q javafx:run
pause
