#!/bin/bash

function Usage()
{
	echo "Usage"
	echo "  $0 ConfigFile [PidFolder]"
	echo ""
}

if [ $# -lt 1 ] ; then
	Usage
  exit 1
fi

CONF=$1

if [ $# -lt 2 ] ; then
  echo "PidFolder not specified" >&2
  exit 1
fi

PidFolder=$2
#echo PidFolder=$PidFolder

COMMON_SCRIPT=`dirname $0`/common.sh
. $COMMON_SCRIPT

GetPidFile $CONF
#echo PID_FILE=$PID_FILE
CheckFile $PID_FILE
PID=`cat $PID_FILE`
echo PID=$PID

kill -9 $PID
sleep 1

LeftProc=`ps -aef | grep \'$PID*courier\'`
if [ "$LeftProc" != "" ];then
  echo ERROR: Courier $PID is still working
else
  rm $PID_FILE
fi