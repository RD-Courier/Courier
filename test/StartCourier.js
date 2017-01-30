//*******************************************************************************************
//******************************* APPLICATION PARAMETERS ************************************
//*******************************************************************************************

/* 
This script takes one parameter - xml config file.
Example:

<?xml version="1.0" encoding="windows-1251" ?> 
<config>
  <!-- severity values: all, fine, info, warning, severe, off -->
  
  <logging
    severity="info"
    mail-active="true"
    smtp-host="magician.rd.ru"
    mail-from="Vasya_Pupkin@rdxxx.ru"
    mail-to="Vasya_Pupkin@rdxxx.ru"
  />
  
  <!-- 
    distr-path  - courier distr path
    more-params - additional parameters for JVM. The most common usage for -D options.
    process-failure-freeze-period  - (milliseconds) disable task during this period because 
                                     previously launched process failed
  -->
  <courier
    java-executable="D:\Program Files\Java\jdk1.5.0\bin\java.exe"
    libs-path="C:\Projects\IT\3D_Projects\Courier\libs"
    distr-path="C:\Projects\IT\3D_Projects\Courier\test\distr"
    more-params=""
  />
  <scheduled-task 
    name="test-stepin" 
    begin-time="18:10" end-time="18:19"
    conf-file="C:\Projects\IT\3D_Projects\Courier\test\conf-mock.xml"
    process-failure-freeze-period="900000"
  />
</config>
*/

// For advanced users
var glMainLoopSleepPeriod = 200;

//*******************************************************************************************
//*******************************************************************************************
//*******************************************************************************************




if (WScript.Arguments.length <= 0) {
  showUsage();
  throw new Error("Configuration file not specified");
}

function showUsage() {
  WScript.Echo("Usage: " + WScript.Name + " <config file>");
}

var fso = new ActiveXObject("Scripting.FileSystemObject");
var glNetwork = WScript.CreateObject("WScript.Network");
var glShell = new ActiveXObject("WScript.Shell");
var glConf = openXml(WScript.Arguments(0)).documentElement;
var logger = new Logger(WScript.ScriptName + ".log", true);
initLogger();
var glSchedule = new Array(
// you can also add tasks right here as in commented example below:
/*
  new ScheduledCourier(
    "test",                // scheduled proc name
    "15:34",               // begin time in form hh:mm
    "17:12",               // end time in form hh:mm
    "C:\\Projects\\IT\\3D_Projects\\Courier\\test\\conf-mock.xml"
  )
, new ScheduledCourier(
    "test - 2",            // scheduled proc name
    "17:12",               // begin time in form hh:mm
    "23:00",               // end time in form hh:mm
    "C:\\Projects\\IT\\3D_Projects\\Courier\\test\\conf-mock.xml"
  )
*/
);
initSchedule();

function initLogger() {
  var n = glConf.selectSingleNode("logging");
  if (n == null) return;
  loadObjectFromXml(
    n, logger 
    , "max-file-lines", "maxLines"
    , "mail-active",    "mailActive"
    , "mail-from",      "from"
    , "mail-to",        "to"
    , "smtp-host",      "smtpHost"
    , "smtp-port",      "port"
  );
  logger.mailActive = (logger.mailActive == "true");
  var attr = getXmlAttr(n, "severity");
  if (attr != undefined) logger.setSeverityAsString(attr.toLowerCase());
}

function initSchedule() {
  var nl = glConf.selectNodes("scheduled-task");
  for (var ni = 0; ni < nl.length; ni++) {
    var n = nl.item(ni);
    var proc = new ScheduledCourier(
        getXmlAttr(n, "name")
      , getXmlAttr(n, "begin-time")
      , getXmlAttr(n, "end-time")
      , getXmlAttr(n, "conf-file")
    );
    proc.processFailureFreezePeriod = getXmlAttr(n, "process-failure-freeze-period");
    glSchedule[glSchedule.length] = proc;
  }
  
  var courierPropsTag = "courier";
  var n = glConf.selectSingleNode(courierPropsTag);
  if (n == null) {
    throw new Error(
      "Courier parameters unspecified" 
      + " (tag '" + courierPropsTag + "' in xml config)"
    );
  }
  var jdkPath = getReqXmlAttr(n, "java-executable");
  var javaLibsPath = getReqXmlAttr(n, "libs-path");
  var courierDistr = getReqXmlAttr(n, "distr-path");
  var morePars = getReqXmlAttr(n, "more-params");

  var javaLibs = getDirFilesStr(javaLibsPath, ";");
  javaLibs += ";";
  javaLibs += courierDistr + "\\courier.zip";

  for (var i = 0; i < glSchedule.length; i++) {
    var data = glSchedule[i];
    
    courierExecStr = (
      "\"" + jdkPath + "\"" 
      + " -classpath \"" + javaLibs + "\""
      + " " + morePars
      + " ru.rd.courier.Application" 
      + " \"" + data.confFile + "\"" 
      + " \"" + courierDistr + "\""
    );
    //logger.debug("Courier Exec string\n" + courierExecStr + "\n\n\n");
    
    logger.info("Registering scheduled process: " + data.toString());
    glSchedule[i] = new ScheduledProc(
      data.desc, data.begTime, data.endTime, 
      courierExecStr, "stop " + data.courierStopTimeout, data.stopTimeout
    );
    if (data.processFailureFreezePeriod != undefined) 
      glSchedule[i].processFailedDelay = parseInt(data.processFailureFreezePeriod);
  }
}

//**************************************************************************************

try {
  while (true) {
    for (var i = 0; i < glSchedule.length; i++) {
      glSchedule[i].handleTimeSlice();
    }
    WScript.Sleep(glMainLoopSleepPeriod);
  }
} catch(e) {
  logger.severe("Starter crashed error: " + errorToString(e));
  throw e;
}
WScript.Quit(0);

//**************************************************************************************

function errorToString(e) {
  return "number = " + (e.number & 0xFFFF) + " description = " + e.description;
}

function checkPropsDefined(obj) {
  var i;
  for (i = 1; i < arguments.length; i++) {
    if (typeof(obj[arguments[i]]) == "undefined") 
      throw new Error("Object field '" + arguments[i] + "' undefined");
  }
}

function setObjectFields(obj) {
  for (var i = 1; i < arguments.length; i+=2) {
    obj[arguments[i]] = arguments[i+1];
  }
}

function objectToString(obj, maxStringLength) {
  var key, ret= "";
  for (key in obj) {
    if (typeof(obj[key]) == "function") continue;
    if (ret.length > 0) ret += " ";
    var value = obj[key]
    var bracket = "";
    if (typeof(obj[key]) == "string") {
      bracket = "'";
      if ((maxStringLength != undefined) && (value.length > maxStringLength)) {
        value = value.substr(0, maxStringLength) + " ...";
      }
    }
    ret += key + " = " + bracket + value + bracket;
  }
  return ret;
}

function getXmlAttr(node, name, defValue) {
  var attr = node.attributes.getNamedItem(name);
  if (attr == null) return defValue;
  return attr.value;
}

function getReqXmlAttr(node, name) {
  var attr = node.attributes.getNamedItem(name);
  if (attr == null) 
    throw new Error(
      "Unspecified attribute '" + name + "' for node " + node.nodeName);
  return attr.value;
}

function loadObjectFromXml(node, obj) {
  for (var i = 2; i < arguments.length; i+=2) {
    var attr = getXmlAttr(node, arguments[i]);
    if (attr != undefined) obj[arguments[i+1]] = attr;
  }
}

//**************************************************************************************

function ScheduledCourier(desc, begTime, endTime, confFile, courierStopTimeout, stopTimeout) {
  this.desc = desc;
  this.begTime = begTime;
  this.endTime = endTime;
  this.confFile = confFile;
  this.courierStopTimeout = (typeof(courierStopTimeout) == "undefined") ? 30 : courierStopTimeout;
  this.stopTimeout = (typeof(courierStopTimeout) == "undefined") ? 60 : stopTimeout;

  function toString() {
    return objectToString(this);
  }
  this.toString = toString;
}

//**************************************************************************************

function ScheduledProc(desc, begTime, endTime, execStr, stopStr, stopTimeout) {
  var timeArr;
  
  timeArr = begTime.split(":");
  if (timeArr.length < 2) throw new Error("Schedule begin time format error");
  this.begHour = parseInt(timeArr[0], 10);
  if (isNaN(this.begHour)) throw new Error("Begin time hour is not an integer number");
  this.begMin = parseInt(timeArr[1], 10);
  if (isNaN(this.begMin)) throw new Error("Begin time minutes is not an integer number");
  
  timeArr = endTime.split(":");
  if (timeArr.length < 2) throw new Error("Schedule end time format error");
  this.endHour = parseInt(timeArr[0], 10);
  if (isNaN(this.endHour)) throw new Error("End time hour is not an integer number");
  this.endMin = parseInt(timeArr[1], 10);
  if (isNaN(this.endMin)) throw new Error("End time minutes is not an integer number");

  this.desc = desc;
  this.execStr = execStr;
  this.execObj = null;
  this.beyondDay = !timeBefore(this.begHour, this.begMin, this.endHour, this.endMin);
  this.stopStr = stopStr;
  this.stopping = false;
  var defaultStopTimeout = 40;
  if (stopTimeout == null) stopTimeout = defaultStopTimeout;
  this.stopTimeout = stopTimeout*1000;
  this.errorDelay = 60*1000;
  this.errorTime = undefined;
  this.processFailedDelay = 15*60*1000;
  this.processFailedTime = undefined;
  
  function toString() {
    return objectToString(this);
  }
  this.toString = toString;

  function toShortString() {
    return objectToString(this, 100);
  }
  this.toShortString = toShortString;
  
  function handleTimeSlice() {
    if (this.errorTime != undefined) {
      var curErrorDelay = (new Date()).getTime() - this.errorTime;
      if (curErrorDelay < this.errorDelay) return;
      this.errorTime = undefined;
    }
    if (this.processFailedDelay != undefined && this.processFailedTime != undefined) {
      var curErrorDelay = (new Date()).getTime() - this.processFailedTime;
      if (curErrorDelay < this.processFailedDelay) return;
      this.processFailedTime = undefined;
    }

    try {
      this.unsafeHandleTimeSlice();
    } catch (e) {
      this.errorTime = (new Date()).getTime();
      logger.severe("handleTimeSlice failed: " + errorToString(e));
      logger.info(
        "Process '" + this.desc + "' will be freezed " + 
        this.errorDelay + " ms because of error"
      );
    }
  }
  this.handleTimeSlice = handleTimeSlice;
  
  function unsafeHandleTimeSlice() {
    this.handleAction();
    
    if (this.stopping) {
      if (this.execObj == null) {
        logger.severe(
          "Process '" + this.desc + 
          "' in stopping state but exec object is null"
        );
      } else {
        if (this.execObj.Status != 0) {
          try {
            WScript.Echo(this.execObj.StdErr.ReadAll());
            var msg = "Process '" + this.desc + 
              "' stopped with exit code = " + this.execObj.ExitCode;
            if (this.execObj.ExitCode == 0) logger.info(msg);
            else logger.severe(msg);
          } finally {
            this.stopped();
          }
        } else {
          var curTime = (new Date()).getTime();
          logger.debug(
            "curTime - proc.stopTime = " + (curTime - this.stopTime)
          );
          if (curTime - this.stopTime > this.stopTimeout) {
            logger.severe(
              "Process " + this.desc + 
              " stopping timeout. It will be terminated"
            );
            this.stopped();
            this.execObj.Terminate();
          }
        }
      }
    } else if (this.isActive()) {
      if (this.execObj.Status != 0) {
        this.stopped();
        this.processFailedTime = (new Date()).getTime();
        logger.severe(
          "Exec.Status != 0 while process '" + this.desc + "' is active");
        logger.info(
          "Process '" + this.desc + "' will be freezed " + 
          this.processFailedDelay + " ms because of process failure"
        );
      }
    }
    if (this.execObj != null) {
      for (var i = 0; i < 10; i++) {      
        if (this.execObj.StdErr.AtEndOfStream) break;
        WScript.Echo(this.execObj.StdErr.ReadLine());
      }
    } 
  }
  this.unsafeHandleTimeSlice = unsafeHandleTimeSlice;

  function handleAction() {
    var date = new Date();
    var begDate = createDate(date, this.begHour, this.begMin);
    var endDate = createDate(date, this.endHour, this.endMin);
    //if (this.beyondDay) endDate.setDate(endDate.getDate() + 1);
    var action;
    if (this.beyondDay) {
      action = (date.getTime() > begDate.getTime()) || (date.getTime() < endDate.getTime());
    } else {
      action = (date.getTime() > begDate.getTime()) && (date.getTime() < endDate.getTime());
    }

    logger.debug(
      "checkTime: date=" + date + 
      " begDate=" + begDate + 
      " endDate=" + endDate + 
      " beyondDay=" + this.beyondDay +
      " action=" + action
    );
    
    if (action != this.isActive()) {
      if (action) {
        logger.info("Starting '" + this.desc + "'");
        logger.debug("Exec string:\n" + this.execStr);
        try {
          this.execObj = glShell.Exec(this.execStr);
        } catch (e) {
          this.execObj = null;
          throw new Error(
            "Error: " + errorToString(e) + 
            " starting process with string:\n" + this.execStr
          );
        }
        logger.info("Process '" + this.desc + "' started");
      } else {
        if (!this.stopping) {
          logger.info("Stopping '" + this.desc + "'");
          this.execObj.StdIn.Write(this.stopStr + "\n");
          this.stopping = true;
          this.stopTime = (new Date()).getTime();
        }
      }
    }
  }
  this.handleAction = handleAction;

  function isActive() {
    return (this.execObj != null) && !this.stopping;
  }
  this.isActive = isActive;

  function stopped() {
    this.stopping = false;
    this.stopTime = null;
    this.execObj = null;
  }
  this.stopped = stopped;

  function createDate(date, hours, mins) {
    var ret = new Date(date.getTime());
    ret.setHours(hours);
    ret.setMinutes(mins);
    ret.setSeconds(0, 0);
    return ret;
  }

  function timeBefore(hour1, min1, hour2, min2) {
    if (hour1 != hour2) return hour1 < hour2;
    return min1 < min2;
  }
}

//**************************************************************************************

function getDirFilesStr(path, separator) {
  var folder = fso.GetFolder(path);

  var ret = "";
  var fEnum = new Enumerator(folder.Files);
  for (; !fEnum.atEnd(); fEnum.moveNext()) {
    var f = fEnum.item();
    if (ret.length > 0) ret += separator;
    ret += f.Path;
  }
  return ret;
}

function sendMailEx(smtpServer, port, from, to, subject, text) {
  var myMail = new ActiveXObject("CDO.Message");
  myMail.Subject = subject;
  myMail.From = from;
  myMail.To = to;
  myMail.TextBody = text;
  // use smtp server
  myMail.Configuration.Fields.Item("http://schemas.microsoft.com/cdo/configuration/sendusing") = 2;
  // Name or IP of remote SMTP server
  myMail.Configuration.Fields.Item("http://schemas.microsoft.com/cdo/configuration/smtpserver") = smtpServer;
  // Server port
  if (port != null)
    myMail.Configuration.Fields.Item("http://schemas.microsoft.com/cdo/configuration/smtpserverport") = port; 
  myMail.Configuration.Fields.Update();
  myMail.Send();
  myMail = null;
}

function openXml(file) {
  var xmlDoc = new ActiveXObject("Msxml2.DOMDocument");
  xmlDoc.async = false;
  xmlDoc.validateOnParse = false;
  xmlDoc.resolveExternals = false;
  xmlDoc.load(file);
  if (xmlDoc.parseError.errorCode != 0) {
    var e = xmlDoc.parseError;
    throw new Error(
      "Error loading xml: "
      + " errorCode=" + e.errorCode
      + " filepos=" + e.filepos
      + " line=" + e.line
      + " linepos=" + e.linepos
      + " reason=" + e.reason
      + " srcText=" + e.srcText
      + " url=" + e.url
    );
  }
  return xmlDoc;
}

//**************************************************************************************

function Logger(logName, showToDisplay) {
  this.showToDisplay = showToDisplay;
  this.maxLines = 10000;
  this.fileSwitcher = false;
  this.severity = 800;

  this.mailActive = false;
  this.from = undefined;
  this.to = undefined;
  this.smtpHost = undefined;
  this.port = null;

  function setMailProps(smtpHost, port, from, to) {
    this.from = from;
    this.to = to;
    this.smtpHost = smtpHost;
    this.port = port;
  }
  this.setMailProps = setMailProps;

  this.severityStrings = new Object();
  setObjectFields(
    this.severityStrings
    , "all",     0
    , "fine",    500
    , "info",    800
    , "warning", 900
    , "severe",  1000
    , "off",     Number.POSITIVE_INFINITY
  );
  function setSeverityAsString(severityString) {
    this.severity = this.severityStrings[severityString];
  }
  this.setSeverityAsString = setSeverityAsString;

  function createStream() {
    this.stream = fso.CreateTextFile(logName + (this.fileSwitcher ? "-2" : ""), true);
    this.fileSwitcher = !this.fileSwitcher;
  }
  this.createStream = createStream;

  this.createStream();


  function writeLn(message, aShowToDisplay) {
    var showToDisplay = (aShowToDisplay == null) ? this.showToDisplay : aShowToDisplay;
    
    if (this.stream.Line > this.maxLines) {
      try {
        this.stream.Close();
      } catch (e) {
        WScript.Echo("Error closing logger stream: " + errorToString(e));
      }
      this.createStream();
    }

    this.stream.WriteLine(message);
    
    if (showToDisplay) WScript.Echo(message);
  }
  this.writeLn = writeLn;

  function debug(text) {
    this.logMessage(500, "FINE: " + text);
  }
  this.debug = debug;

  function info(text) {
    this.logMessage(800, "INFO: " + text);
  }
  this.info = info;

  function warning(text) {
    this.logMessage(900, "WARNING: " + text);
  }
  this.warning = warning;

  function severe(text) {
    this.logMessage(1000, "SEVERE: " + text);
  }
  this.severe = severe;

  function logMessage(severity, text) {
    if (severity < this.severity) return;
    var dt = new Date();
    text = dt.getHours() + ":" + dt.getMinutes() + ":" + dt.getSeconds() + " " + text;
    this.writeLn(text);
    if (this.mailActive && (severity >= 1000)) {
      this.sendMail(
        glNetwork.ComputerName + " - " + WScript.ScriptName + ": " + text
      );
    }
  }
  this.logMessage = logMessage;

  function sendMail(text) {
    if (this.smtpHost == undefined || this.to == undefined) {
      logger.writeLn(
        "WARNING: mail is active but: smtpHost=" + 
        this.smtpHost + " to=" + this.to
      );
      return;
    }
    try {
      sendMailEx(this.smtpHost, this.port, this.from, this.to, text, text);
    } catch (e) {
      logger.writeLn("Error sending mail: " + errorToString(e));
    }
  }
  this.sendMail = sendMail;
}