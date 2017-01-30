#!/usr/bin/python

import sys, os, subprocess

def scriptDir():
  return os.path.abspath(os.path.dirname(sys.argv[0]))

def scriptPath(*paths):
  return os.path.join(scriptDir(), *paths)

def devPath(*paths):
  return os.path.join(scriptPath('..'), *paths)

execfile(scriptPath(r'..\misc\CourierUtils.py'))

def courierTestArgs(config, extraLibs = None, extraPars = None):
  def checkFile(path):
    if not os.path.exists(path): raise RuntimeError("File '" + path + "' not found")

  classPath = [devPath(r'project\classes')]
  libsDir = devPath(r'libs\main')
  for f in os.listdir(libsDir):
    classPath.append(os.path.join(libsDir, f))
  if extraLibs != None: classPath.extend(extraLibs)

  javaHome = None
  for ev in ('JAVAHOME', 'JAVA_HOME'):
    if ev in os.environ:
      javaHome = os.environ[ev]
      break

  java = 'java' if javaHome == None else os.path.join(javaHome, 'bin', 'java')
  args = [java, '-classpath', os.pathsep.join(classPath)]
  if extraPars != None: args.extend(extraPars)
  systemDir = devPath(r'project\test-src\ru\rd\courier\distr')
  args.extend(['ru.rd.courier.Application', config, systemDir])
  return args

params = extractScriptParams()
args = courierTestArgs(
  params['config'], 
  extraLibs = params.get('extra-libs', None),
  extraPars = params.get('params', None)
)
subprocess.check_call(args)
