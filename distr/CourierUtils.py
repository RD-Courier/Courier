#!/usr/bin/python

import sys, os, cStringIO

def courierArgs2(config, libsDir = None, extraLibs = None, extraPars = None, courierDir = None):
  def checkFile(path):
    if not os.path.exists(path): raise RuntimeError("File '" + path + "' not found")

  if courierDir == None:
    courierDir = os.path.dirname(__file__)
  classPath = [os.path.join(courierDir, 'courier.zip')]

  if libsDir == None:
    libsDir = os.environ.get('CourierLibs', os.path.join(courierDir, '..', 'CourierLibs'))
  checkFile(libsDir)

  for f in os.listdir(libsDir):
    classPath.append(os.path.join(libsDir, f))

  if extraLibs != None: classPath.extend(extraLibs)

  if 'CourierExtraLibs' in os.environ:
    classPath.append(os.environ['CourierExtraLibs'])

  javaHome = None
  for ev in ('JAVAHOME', 'JAVA_HOME'):
    if ev in os.environ:
      javaHome = os.environ[ev]
      break

  java = 'java' if javaHome == None else os.path.join(javaHome, 'bin', 'java')
  args = [java, '-classpath', os.pathsep.join(classPath)]
  if extraPars != None: args.extend(extraPars)
  return (args, ['ru.rd.courier.Application', config, courierDir])

def courierArgs(config, libsDir = None, extraLibs = None, extraPars = None):
  args2 = courierArgs2(config, libsDir = libsDir, extraLibs = extraLibs, extraPars = extraPars)
  return args2[0] + args2[1]

def courierCmd(config, libsDir = None, extraLibs = None, extraPars = None, courierDir = None):
  args2 = courierArgs2(config, libsDir = libsDir, extraLibs = extraLibs, extraPars = extraPars, courierDir = courierDir)
  envExtra = ''
  for ev in ('CourierJavaParams',):
    if ev in os.environ:
      envExtra += ' ' + os.environ[ev]

  return shellParams(args2[0]) + envExtra + ' ' + shellParams(args2[1])

def shellParams(pars):
  s = cStringIO.StringIO()
  try:
    for i, p in enumerate(pars):
      if i > 0: s.write(' ')
      if (p.find(' ') >= 0) or (p.find('"') >= 0):
        s.write('"')
        p = p.replace('\\', '\\\\')
        p = p.replace('"', '\\"')
        s.write(p)      
        s.write('"')
      else:
        s.write(p)      
    return s.getvalue()
  finally:
    s.close()  

def extractScriptParams():
  ret = {}
  sargs = sys.argv
  i = 1
  while i < len(sargs):
    n = sargs[i]
    if n.startswith('--'):
      n = n[2:]
      if i+1 < len(sargs):
        v = sargs[i+1]
        if v.startswith('--'): v = True
        else: i += 1
      else:
        v = True
      ret[n] = v
    i += 1
  return ret
