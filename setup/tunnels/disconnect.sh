#!/bin/bash
kill $(ps ax | grep kiali | awk 'NR==1{print $1}')
kill $(ps ax | grep loadgenerator | awk 'NR==1{print $1}')
kill $(ps ax | grep grafana | awk 'NR==1{print $1}')