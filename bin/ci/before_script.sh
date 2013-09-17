#!/bin/sh

sudo rabbitmq-plugins enable rabbitmq_mqtt
sudo service rabbitmq-server restart
sleep 5
