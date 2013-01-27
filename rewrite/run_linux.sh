#!/bin/sh

check_call()
{
	res=$?
	if [ $res -ne 0 ]
	then
		echo "Aborting"
		exit $res
	fi
}

cd `dirname $0`
check_call
cd lua
check_call

../native/build/aware-engine