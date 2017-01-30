#!/bin/sh

JAVAHOME=/usr/jdk/latest
CourierDir=`dirname $0`

GetPidFile() {
  PID_FILE=$PidFolder/`basename $1`.courier.pid
}

GetOutputFile() {
  OutputFile=$PidFolder/$1.courier.log
}

CheckFile() {
  if [ ! -f $1 ] ; then
    echo "ERROR ---- File: $1 not found !!!!" >&2
    exit 1
  fi
}
