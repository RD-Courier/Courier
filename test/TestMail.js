var fso = new ActiveXObject("Scripting.FileSystemObject");
var logger = new Logger(WScript.ScriptName + ".log", true);
var oShell = new ActiveXObject("WScript.Shell");


//try {
  logger.severe("Process stopped with exit code = 0");
/*
} catch(e) {
  logger.severe("number = " + (e.number & 0xFFFF) + " description = " + e.description);
  throw e;
  WScript.Quit(1);
}
*/
WScript.Quit(0);


//**************************************************************************************

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

function Logger(logName, showToDisplay) {
  this.stream = fso.CreateTextFile(logName, true);
  this.showToDisplay = showToDisplay;

  function writeLn(message, aShowToDisplay) {
    var showToDisplay = (aShowToDisplay == null) ? this.showToDisplay : aShowToDisplay;
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
    if (severity <= 500) return;
    text = "STARTER " + text;
    this.writeLn(text);
    if (severity >= 1000) this.sendMail(text);
  }
  this.logMessage = logMessage;

  function sendMail(text) {
    //try {
      sendMailEx("magician.rd.ru", null, "testemail@rdxxx.ru", "testemail@rdxxx.ru", text, text);
    /*
    } catch (e) {
      logger.writeLn("Error sending mail:" + " number = " + (e.number & 0xFFFF) + " description = " + e.description);
    }
    */
  }
  this.sendMail = sendMail;
}