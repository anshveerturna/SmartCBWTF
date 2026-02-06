#!/bin/bash
KEY=~/.ssh/smartcbwtf-key.pem
HOST=ec2-user@13.235.98.218

ssh -i $KEY $HOST 'pkill -f "java.*backend" || true'
sleep 3
ssh -i $KEY $HOST 'nohup java -Xmx512m -jar /home/ec2-user/backend.jar --spring.profiles.active=prod > /home/ec2-user/backend.log 2>&1 &'
sleep 15
ssh -i $KEY $HOST 'ps aux | grep backend.jar | grep -v grep'
