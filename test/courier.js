var fso = new ActiveXObject("Scripting.FileSystemObject");
var shell = new ActiveXObject("WScript.Shell");
var env = shell.Environment("PROCESS");
var glParams = getScriptParams();

var clibs = env("CourierLibs");
if (clibs == "") {
  var paths = new Array(scriptPath(".."));
  for (var i in paths) {
    var clibs = fso.BuildPath(paths[i], "CourierLibs");
    if (fso.FolderExists(clibs)) break;
  }
}
if (!fso.FolderExists(clibs)) throw new Error("CourierLibs not found");

if (env("CourierCode").length > 0) courierCode = env("CourierCode");
else courierCode = scriptPath("courier.zip");
var classPath = collectLibs(clibs) + ";" + courierCode;
if (env("JavaLibs").length > 0) classPath += ";" + env("JavaLibs");
var javaHome = env("JAVA_HOME");
if (javaHome.length == 0) javaHome = env("JAVAHOME");
if (javaHome.length == 0) throw new Error("JavaHome not specified");
checkFolder(javaHome);

var extParams = env("JavaParams");
if (extParams.length > 0) extParams = " " + extParams;
var conf = glParams.getReqParam("conf");
if (glParams.hasParam("system")) {
  var system = glParams.getParam("system");
} else {
  var system = scriptPath();
}

var cmd = 
  checkedParam(buildPath(javaHome, "bin\\java.exe")) + 
  " -server -classpath " + checkedParam(classPath) + extParams + " ru.rd.courier.Application " + 
  checkedParam(conf) + " " + checkedParam(system);

if (glParams.hasParam("title")) {
  cmd = checkedParam(env("comspec")) + " /C start " + '"' + glParams.getParam("title") + '" ' + cmd;
}

if (env("CourierStart").length > 0) {
  cmd = env("CourierStart") + " " + cmd;
}
//WScript.Echo("cmd=" + cmd);
shell.Run(cmd, 1, false);

function buildPath(path1, path2) {
  return fso.BuildPath(path1, path2);
}

function scriptPath(rpath) {
  var scrPath = extractFilePath(WScript.ScriptFullName);
  if (typeof(rpath) == "undefined") return scrPath;
  return fso.BuildPath(scrPath, rpath);
}

function checkFolder(path) {
  if (!fso.FolderExists(path)) throw new Error("Invalid folder: " + path);
}

function extractFilePath(fullName) {
  var p;
  p = fullName.lastIndexOf("\\");
  if (p >= 0) return fullName.substring(0, p);
  p = fullName.lastIndexOf("/");
  if (p >= 0) return fullName.substring(0, p);
  return fullName;
}

function hasBlanks(s) {
  if (typeof(glHasBlanksRegExp) == "undefined") glHasBlanksRegExp = new RegExp("\\s+", "gmi");
  glHasBlanksRegExp.lastIndex = 0;
  return glHasBlanksRegExp.test(s);
}

function checkedParam(param) {
  if (!hasBlanks(param)) return param;
  var bracket = "\"";
  return bracket + param + bracket;
}

function collectLibs(path) {
  var re = new RegExp("^.+\\.(?:jar|zip)$", "gmi");
  var folder = fso.GetFolder(path);
  var fEnum = new Enumerator(folder.Files);
  var ret = "";
  //var ii = 0;
  for (; !fEnum.atEnd(); fEnum.moveNext()) {
     var f = fEnum.item();
     re.lastIndex = 0;
     if (re.test(f.Name)) {
       //ii++;
       if (ret.length > 0) ret += ";";
       ret += f.Path;
     }
  }
  //WScript.Echo("collectLibs: " + ii);
  return ret;
}

function stringStartsWith(aString, aWith) {
  return (
    (aString.length >= aWith.length) && (aString.substr(0, aWith.length) == aWith)
  );
}

function getScriptParams() {
  function ScriptParams() {
    this.params = new Object();

    function hasParam(name) {
      return (typeof(this.params[name]) != "undefined");
    }
    this.hasParam = hasParam;

    function getParam(name) {
      return this.params[name];
    }
    this.getParam = getParam;

    function getParamDef(name, def) {
      if (this.hasParam(name)) return this.getParam(name);
      return def;
    }
    this.getParamDef = getParamDef;

    function getReqParam(name) {
      if (!this.hasParam(name)) throw new Error("Script param '" + name + "' not found");
      return this.params[name];
    }
    this.getReqParam = getReqParam;

    function setParam(name, value) {
      this.params[name] = value;
    }
    this.setParam = setParam;
  }
  
  var scriptArguments = WScript.Arguments;  
  var scriptOptions = new ScriptParams();
  for (var i = 0; i < scriptArguments.length; i++)  {
    var arg = scriptArguments(i);
    if (stringStartsWith(arg, "--")) {
      var argName = arg.substring(2);
      if (argName == "D") {
        i++;
        scriptOptions.setParam(scriptArguments(i), scriptArguments(i+1));
        i++;
      } else {
        if ((i >= (scriptArguments.length - 1)) || stringStartsWith(scriptArguments(i+1), "--")) {
          scriptOptions.setParam(argName, "true");
        } else {
          i++;
          scriptOptions.setParam(argName, scriptArguments(i));
        }
      } 
    }
  }
  return scriptOptions;
}
