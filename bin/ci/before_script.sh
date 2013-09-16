#!/bin/sh

sudo rabbitmq_plugin enable rabbitmq_mqtt
sudo service rabbitmq-server restart
sleep 5
