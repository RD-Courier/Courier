import sys, os, subprocess, shutil, time, datetime, random, itertools
import TdLogUtils
from TdSysUtils import *
from TdLogUtils import *
from TdScheduler import *

execfile(scriptPath('distr/CourierUtils.py'))

def ensureDir(path):
  if not os.path.exists(path): os.makedirs(path)

class TestCouriers(object):
  def __init__(self, pattern, distr, couriersPath):
    self.distr = distr
    self.pattern = pattern
    self.root = couriersPath

  def tpath(self, *p):
    return os.path.join(self.root, *p)

  def start(self):
    for i in xrange(1, 6):
      if i > 1: time.sleep(2)
      TestCourier(self, self.tpath(str(i))).start()

class TestCourier(object):
  def __init__(self, tc, folder, asynch = True, courierLibs = None):
    self.tc = tc
    self.folder = folder
    self.asynch = asynch
    self.courierLibs = courierLibs
    self.process = None
    ensureDir(self.folder)
    self.pcopy('conf-mock.xml')
    self.pcopy('conf-def.xml')
    self.startTime = None

  def ppath(self, *p):
    return os.path.join(self.tc.pattern, *p)

  def path(self, *p):
    return os.path.join(self.folder, *p)

  def dpath(self, *p):
    return os.path.join(self.tc.distr, *p)

  def pcopy(self, f):
    shutil.copyfile(self.ppath(f), self.path(f))

  def start(self):
    self.startTime = datetime.datetime.now()

    if self.asynch:
      cmd = (
        'start "CourierTest" cmd /C ' + self.dpath('courier.cmd') + ' ' + self.path('conf-mock.xml')
        #+ ((' > ' + self.path('STDOUT.log') + ' 2>&1') if not self.asynch else '')
      )
      env = os.environ.copy()
      #env['JAVA_HOME'] = r'C:\Program Files\Java\jre6'
      #env['COURIER_DEFAULT_CONFIG'] = self.ppath('conf-def.xml')
      if self.courierLibs != None:
        env['CourierLibs'] = self.courierLibs
      info('Starting: %s' % (cmd,))
      subprocess.check_call(cmd, shell=True, env=env)
    else:
      conf = self.path('conf-mock.xml')
      cmd = courierCmd(
        conf, 
        libsDir = self.courierLibs,
        courierDir = scriptPath('distr')
      )
      info('Starting: %s' % (conf,))
      self.process = subprocess.Popen(cmd, stdin=subprocess.PIPE)

  def stop(self):
    if self.process.poll() != None: return
    self.process.stdin.write('stop 5000\n')
    self.process.wait()

  def kill(self):
    if self.process.poll() != None: return
    self.process.kill()

  def workTime(self):
    if self.startTime == None: return 0
    return (datetime.datetime.now() - self.startTime).seconds

class LogThread(threading.Thread):
  def __init__(self, target):
    self.wrappedTarget = target
    threading.Thread.__init__(self, target=self.logtarget)

  def logtarget(self):
    try:
      self.wrappedTarget()
    except:
      errorExc()

class TestCouriersEx(object):
  class TimerTask(object):
    def __init__(self, couriers):
      self.couriers = couriers

    def start(self):
      #info('LoadStartStop: start')
      t = LogThread(target=self.couriers.startCouriers)
      t.setDaemon(True)
      t.start()
      #self.couriers.startCouriers()

    def stop(self):
      #info('LoadStartStop: stop')
      t = LogThread(target=self.couriers.stopCouriers)
      t.setDaemon(True)
      t.start()
      #self.couriers.stopCouriers()

  def __init__(self, pattern, distr, couriersPath, maxCouriers = None, courierLibs = None):
    self.lock = threading.Condition()
    self.working = False
    self.active = True
    self.distr = distr
    self.pattern = pattern
    self.root = couriersPath
    self.maxCouriers = maxCouriers
    self.courierLibs = courierLibs
    self.couriers = dict()
    tt = MinuteTimeTable(datetime.timedelta(seconds=0), datetime.timedelta(seconds=30))
    self.timer = TimeTableTimer(tt, self.TimerTask(self))
    self.timer.start()


  def tpath(self, *p):
    return os.path.join(self.root, *p)

  def findCid(self):
    for i in xrange(1, 1000):
      if not(i in self.couriers): return i
  
  def _canWork(self):
    self.lock.acquire()
    try:
      if (not self.active) or self.working: return False
      self.working = True
      return True
    finally:
      self.lock.release()

  def _finishWorking(self):
    self.lock.acquire()
    try:
      self.working = False
      self.lock.notifyAll()
    finally:
      self.lock.release()

  def startCouriers(self):    
    if not self._canWork(): return
    try:
      #createCount = random.randint(1, 4)
      createCount = 10
      for i in xrange(0, createCount):
        if (self.maxCouriers != None and len(self.couriers) >= self.maxCouriers): break
        if i > 1: time.sleep(2)
        cid = self.findCid()
        c = TestCourier(self, self.tpath(str(cid)), asynch = False, courierLibs = self.courierLibs)
        c.start()
        self.couriers[cid] = c
    finally:
      self._finishWorking()

  def stopCouriers(self):
    if not self._canWork(): return
    try:
      scouriers = dict()
      for cid, c in self.couriers.iteritems():
        if c.workTime() > 30:
          scouriers[cid] = c
      for cid, c in scouriers.iteritems():
        self.stopCourier(cid, c)
    finally:
      self._finishWorking()

  def stopCourier(self, cid, c):    
    try:
      del self.couriers[cid]
      if cid % 2 == 1:
        info('STOPPING: %s' % (cid,))
        c.stop()
      else:
        info('KILLING: %s' % (cid,))
        c.kill()
    except:
      errorExc()

  def stop(self):    
    self.timer.stop()
    self.lock.acquire()
    try:
      self.active = False
      while self.working:
        self.lock.wait()
    finally:
      self.lock.release()

    while len(self.couriers) > 0:
      for cid, c in self.couriers.iteritems():
        self.stopCourier(cid, c)
        break


def runTestCouriers(maxCouriers = None, courierLibs = None):
  TdLogUtils.confLogger(scriptDir(), None, False, True)
  #test = TestCouriers(scriptPath(''), scriptPath('distr'), scriptPath('couriers'))
  test = TestCouriersEx(scriptPath(''), scriptPath('distr'), scriptPath('couriers'), maxCouriers = maxCouriers, courierLibs = courierLibs)
  #test.start()

  try:
    while True:
      iline = raw_input('> ')
      iline = iline.strip()
      if iline == 'stop': break
      if iline == 'list': 
        info(','.join(itertools.imap(lambda cid: str(cid), test.couriers.iterkeys())))
  finally:
    test.stop()
