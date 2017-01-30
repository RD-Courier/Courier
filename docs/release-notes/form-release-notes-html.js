var xmlSource = new ActiveXObject("MSXML2.DOMDocument");
var xmlXForm = new ActiveXObject("MSXML2.DOMDocument");

xmlSource.validateOnParse = true;
xmlXForm.validateOnParse = true;
xmlSource.async = false;
xmlXForm.async = false;
xmlSource.resolveExternals = false;
xmlXForm.resolveExternals = false;

xmlSource.load("release-notes.xml");
xmlXForm.load("release-notes.xsl");
var html = xmlSource.transformNode(xmlXForm);
var fso = WScript.CreateObject("Scripting.FileSystemObject");
var out = fso.CreateTextFile("release-notes.html", true);
out.Write(html);
out.Close();