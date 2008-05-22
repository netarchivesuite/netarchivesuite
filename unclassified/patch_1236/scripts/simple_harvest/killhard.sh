#!/bin/bash

ps -wweo pid,command | grep simple.harvest.indicator |  sed 's/^[ ^t]*//' |  cut -d ' ' -f 1,1  | xargs kill -9
ps -wweo pid,command | grep "JMS Broker" |  sed 's/^[ ^t]*//' |  cut -d ' ' -f 1,1  | xargs kill -9