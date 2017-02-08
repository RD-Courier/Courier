#!/usr/bin/python

import sys, os, subprocess

def scriptDir():
  return os.path.abspath(os.path.dirname(sys.argv[0]))

def scriptPath(*paths):
  return os.path.join(scriptDir(), *paths)

execfile(scriptPath('CourierUtils.py'))

params = extractScriptParams()
subprocess.check_call(courierCmd(
  params['config'], 
  libsDir = params.get('libs-dir', None),
  extraLibs = params.get('extra-libs', None),
  extraPars = params.get('params', None)
))
