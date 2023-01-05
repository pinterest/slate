from typing import List

from slate.slate_taskdefinition import TaskDefinition
from slate.slate_taskdefinition import TaskRuntime
from slate.slate_process import LifecycleProcess
import os, sys
import traceback
import inspect
from slate.slate_rpc import HumanTaskSystem


def all_files(directory):
    for path, dirs, files in os.walk(directory):
        for f in files:
            if f.endswith('.py'):
                yield os.path.join(path, f)


# Found here: 
# https://stackoverflow.com/questions/3137731/is-this-correct-way-to-import-python-scripts-residing-in-arbitrary-folders
def import_from_absolute_path(fullpath, global_name=None):
    script_dir, filename = os.path.split(fullpath)
    script, ext = os.path.splitext(filename)

    sys.path.insert(0, script_dir)
    try:
        module = __import__(script)
        if global_name is None:
            global_name = script
        globals()[global_name] = module
        sys.modules[global_name] = module
        print("Imported source file for TD:" + filename)
    except ModuleNotFoundError as mnf:
        print("Module load failed:" + str(mnf) + " " + fullpath)
        raise
    except ImportError as ie:
        print(ie)
    except FileNotFoundError as fnf:
        print(fnf)
    finally:
        del sys.path[0]


class TaskFactory:
    
    def __new__(cls, importdirs: List[str]):
        if not hasattr(cls, 'instance'):
          cls.instance = super(TaskFactory, cls).__new__(cls)
        return cls.instance
    
    def allSubclasses(self, cls):
        return set(cls.__subclasses__()).union(
            [s for c in cls.__subclasses__() for s in self.allSubclasses(c)])
    
    def __init__(self, importdirs: List[str]):
        self.taskMap = {}
        print("TD importdirs:" + str(importdirs))
        for d in importdirs:
            for f in all_files(d):
                if "slate_taskfactory" in f:
                    continue
                if "satelliteservermain" in f:
                    continue
                import_from_absolute_path(f)
        
        print(TaskDefinition.__subclasses__())
        # run reflections to find all classes
        for cls in self.allSubclasses(TaskDefinition):
            classname = cls.__module__ + "." + cls.__qualname__
            if inspect.isabstract(cls):
                print("Ignoring:" + classname + " because it's abstract")
                continue
            obj = cls()
            print("Loaded TD:" + obj.taskDefinitionId)
            self.taskMap[obj.taskDefinitionId] = obj 

class LocalTaskRuntime(TaskRuntime):
    
    def __init__(self, dev:bool, configDirectory:str, tmpDirectory: str, hts: HumanTaskSystem, taskFactory: TaskFactory):
        self.dev = dev
        self.configDirectory = configDirectory
        self.tmpDirectory = tmpDirectory
        self.hts = hts
        self.taskFactory = taskFactory
        
    def startExecution(self, taskDefinitionId:str, taskInstanceId:str, process:LifecycleProcess):
        taskDef = self.taskFactory.taskMap[taskDefinitionId]
        return taskDef.startExecution(self, taskInstanceId, process, process.processContext, process.processContext[taskInstanceId] if taskInstanceId in process.processContext else None)
    
    def checkStatus(self, taskDefinitionId:str, taskInstanceId:str, process:LifecycleProcess):
        taskDef = self.taskFactory.taskMap[taskDefinitionId]
        return taskDef.checkStatus(self, taskInstanceId, process, process.processContext, process.processContext[taskInstanceId] if taskInstanceId in process.processContext else None)
  
