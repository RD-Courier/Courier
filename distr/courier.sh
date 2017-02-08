#!/bin/bash

function Usage()
{
    echo "Usage"
    echo "  $0 ConfigFile AsDaemon [PidFolder] [ExtraPars]"
    echo ""
}

if [ $# -lt 3 ] ; then
  Usage
  exit 1
fi

CONF=$1
Daemon=$2
if [ $Daemon -gt 0 ] ; then
  if [ $# -lt 3 ] ; then
    echo "PidFolder not specified" >&2
    exit 1
  fi
  PidFolder=$3
  if [ ! -d $PidFolder ]; then
    mkdir -p $PidFolder
  fi
  ExtraPars=$4
else
  ExtraPars=$3
fi

. `dirname $0`/common.sh

#echo "CONF=$1 Daemon=$Daemon ExtraPars=$ExtraPars PidFolder=$PidFolder"

PID_FILE=$PidFolder/$CONF.courier.pid

ClassPath=$CourierDir/courier.zip

if [ "$CourierLibs" = "" ] ; then
  CourierLibs=$CourierDir/../CourierLibs
fi

if [ ! -e "$CourierLibs"  ] ; then
  echo "CourierLibs not found" >&2
  exit 1
fi

for file in `ls $CourierLibs`
do
  ClassPath="$ClassPath:$CourierLibs/$file"
done

if [ "$CourierExtraLibs" != "" ] ; then
  ClassPath="$ClassPath:$CourierExtraLibs"
fi

EXEC_CMD="$JAVAHOME/bin/java -classpath $ClassPath $ExtraPars ru.rd.courier.Application $CONF $CourierDir"

if [ "$Daemon" = "1" ] ; then
  #GetOutputFile $CONF
  OutputFile=/dev/null
  #OutputFile=`dirname $0`/`basename $1`.courier.log
  #OutputFile=/dev/null
  #echo OutputFile=$OutputFile
  nohup $EXEC_CMD > $OutputFile 2>&1 &
  LastId=$!
  GetPidFile $CONF
  echo $LastId > $PID_FILE
else
  $EXEC_CMD
fi
