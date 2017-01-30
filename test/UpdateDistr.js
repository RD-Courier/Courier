if (WScript.Arguments.length <= 0) {
  showUsage();
  throw new Error("Development path not specified");
}

if (WScript.Arguments.length <= 1) {
  showUsage();
  throw new Error("Deployment target path not specified");
}

var fso = new ActiveXObject("Scripting.FileSystemObject");
var oShell = new ActiveXObject("WScript.Shell");
var logger = new Logger(
  WScript.Arguments.length > 2 ? WScript.Arguments(2) : (WScript.ScriptName + ".log"),
  true
);

try {
  var devPath = WScript.Arguments(0);
  logger.writeLn("Dev path: " + devPath);
  var deployPath = WScript.Arguments(1);
  logger.writeLn("Deploying to: " + deployPath);

  var distrFolderName = "distr";
  var distrPath = deployPath + "\\" + distrFolderName;
  var archDir = deployPath + "\\archive";
  var appName = "courier";

  var archiver = new Archiver(oShell);
  var zipName, zipDesc;

  if (fso.FolderExists(distrPath)) deleteFolderContent(fso, distrPath);
  ensureFolder(fso, distrPath);

  classesArch = appName + ".zip";
  classesArchPath = distrPath + "\\" + classesArch;
  archiver.archFolder(classesArchPath, devPath + "\\project\\classes");

  var testCasesDistr = "project\\resources\\ru\\rd\\courier\\resources";
  var files = new Array(
      new DeployItem(testCasesDistr + "\\config.xsl", "config.xsl")
    , new DeployItem(testCasesDistr + "\\custom-tmpl.xsl", "custom-tmpl.xsl")
    , new DeployItem(testCasesDistr + "\\StatementFactoryConf.xml", "StatementFactoryConf.xml")
    , new DeployItem(testCasesDistr + "\\sys-config.xml", "sys-config.xml")
    , new DeployItem(testCasesDistr + "\\base-structure.xml", "base-structure.xml")
    , new DeployItem("misc\\courier.cmd", "courier.cmd")
    , new DeployItem("misc\\StartExample.cmd", "StartExample.cmd")
    , new DeployItem("misc\\CourierUtils.py", "CourierUtils.py")
    , new DeployItem("misc\\Encrypt.cmd", "Encrypt.cmd")
  );
  
  logger.writeLn("Deploying to distibutive");
  copyFiles(fso, devPath, distrPath, files);

  logger.writeLn("\nDeployment successfull");
} catch(e) {
  logger.writeLn("Error occured: number = " + (e.number & 0xFFFF) + " description = " + e.description);
  throw e;
  WScript.Quit(1);
}
WScript.Quit(0);

function Archiver(aShell) {
  if (aShell == null) this.shell = new ActiveXObject("WScript.Shell");
  else this.shell = aShell;

  this.jdkPath = "C:\\Program Files\\Java\\jdk1.5.0_07\\bin";
  this.execStrBeg = this.jdkPath + "\\jar cvfM ";

  function createArchive(zipName, desc) {
    var execStr = this.execStrBeg + zipName  + " " + desc;
    logger.writeLn("Creating archive with command:");
    logger.writeLn(execStr);
    var oArchExec = this.shell.Exec(execStr);
    while(!oArchExec.StdOut.AtEndOfStream) {
       logger.writeLn(oArchExec.StdOut.ReadLine(), false);
    }
    while (oArchExec.Status == 0) {
      WScript.Sleep(200);
    }
    if (oArchExec.ExitCode == 0) {
      logger.writeLn("Archive " + zipName + " created successfully");
    } else {
      throw new Error("Failed to create archive " + zipName + ". Exit code = " + oArchExec.ExitCode);
    }
  }
  this.createArchive = createArchive;

  function archFolder(zipName, path) {
    this.createArchive(zipName, "-C " + path + " .");
  }
  this.archFolder = archFolder;
}

function Logger(logName, showToDisplay) {
  this.stream = fso.CreateTextFile(logName, true);
  this.showToDisplay = showToDisplay;

  function writeLn(message, aShowToDisplay) {
    showToDisplay = (aShowToDisplay == null) ? this.showToDisplay : aShowToDisplay;
    this.stream.WriteLine(message);
    if (showToDisplay) WScript.Echo(message);
  }
  this.writeLn = writeLn;
}

function deleteFolderContent(folderPath) {
  var fso = new ActiveXObject("Scripting.FileSystemObject");
  var f = fso.GetFolder(folderPath);
  var fc = new Enumerator(f.files);
  for (; !fc.atEnd(); fc.moveNext()) {
    fc.item().Delete(true);
  }
}

function deleteFolderAndCheck(fso, path) {
  fso.DeleteFolder(path, true);
  if (fso.FolderExists(path)) {
    throw new Error("Failed to delete folder: " + path);
  } else {
    logger.writeLn("Folder deleted: " + path);
  }
}

function deleteFolderContent(fso, path) {
  if (!fso.FolderExists(path)) return;
  var folder = fso.GetFolder(path);

  fEnum = new Enumerator(folder.Files);
  for (; !fEnum.atEnd(); fEnum.moveNext()) {
     fEnum.item().Delete();
  }

  fEnum = new Enumerator(folder.SubFolders);
  for (; !fEnum.atEnd(); fEnum.moveNext()) {
     fEnum.item().Delete();
  }

  if ((folder.Files.Count > 0) || (folder.SubFolders.Count > 0)) {
    throw new Error("Failed to delete folder content");
  } else {
    logger.writeLn("Folder content deleted: " + path);
  }
}

function deleteFileAndCheck(fso, path) {
  fso.DeleteFile(path, true);
  if (fso.FileExists(path)) {
    throw new Error("Failed to delete file: " + path);
  } else {
    logger.writeLn("File deleted: " + path);
  }
}

function ensureFolder(fso, path) {
  if (fso.FolderExists(path)) return;
  fso.CreateFolder(path);
  if (fso.FolderExists(path)) {
    logger.writeLn("Folder created: " + path);
  } else {
    throw new Error("ensureFolder --> Failed to create folder: " + path);
  }
}

function DeployItem(fromName, toName) {
  this.fromName = fromName;
  if (toName == null) this.toName = fromName;
  else this.toName = toName;
}

function copyFiles(fso, fromPath, toPath, items) {
  for (i in items) {
    var deplItem = items[i];
    var fromName, toName;
    if (typeof(deplItem) == "string") {
      fromName = deplItem; 
      toName = deplItem;
    } else {
      fromName = deplItem.fromName; 
      toName = deplItem.toName;
    }
    var endOfPath = Math.max(toName.lastIndexOf("\\"), toName.lastIndexOf("/"));
    if (endOfPath >= 0) {
      var path = toName.substring(0, endOfPath);
      ensureFolder(fso, toPath + "\\" + path);
    }
    var fn = fromPath + "\\" + fromName;
    var ft = toPath + "\\" + toName;
    
    logger.writeLn("Copying " + fn + " --> " + ft);
    if (fso.FileExists(fn)) {
      //var f = fso.GetFile(fn);
      //f.Copy(ft, true);
      fso.CopyFile(fn, ft);
    } else if (fso.FolderExists(fn)) {
      fso.CopyFolder(fn, ft);
    } else {
      throw new Error("File or Folder not found: " + fn);
    }
  }
}

function getTwoDiditString(d) {
  if (d < 10) {
    return "0" + d
  } else {
    return d;
  }
}

function dateToString(dt) {
  return (
    "" + dt.getFullYear() + 
    '-' + (getTwoDiditString(dt.getMonth() + 1)) + 
    '-' + getTwoDiditString(dt.getDate()) + 
    "-" + getTwoDiditString(dt.getHours()) + 
    '-' + getTwoDiditString(dt.getMinutes()) + 
    '-' + getTwoDiditString(dt.getSeconds())
  ); 
}
