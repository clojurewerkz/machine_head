#!/bin/sh

sudo rabbitmq-plugin enable rabbitmq_mqtt
sudo service rabbitmq-server restart
sleep 5
