#!/bin/bash
source /home/ec2-user/.bashrc
pkill -9 -f backend.jar 2>/dev/null
sleep 2
cd /home/ec2-user
nohup java -Xmx512m -jar backend.jar --spring.profiles.active=prod > backend.log 2>&1 &
echo "PID=$!"
