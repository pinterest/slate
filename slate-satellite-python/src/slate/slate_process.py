from slate.slate_process_commons import Status
from slate.slate_process_commons import Task
from slate.slate_process_commons import ProcessType

class LifecycleProcess(object):
    
    def __init__(self, processId:str=None, processType:ProcessType=None, executionId:str=None, maxConcurrentTasks=1, processContext=None, currenTaskSet=None, startTaskId:str=None, allTasks=None, endStatus=Status.NOT_STARTED, startTimeMs=0, endTimeMs=0):
        self.__dict__ = {}
        self.processId = processId
        self.executionId = executionId
        self.processType = processType
        self.maxConcurrentTask = maxConcurrentTasks
        self.processContext = processContext or dict()
        self.currenTaskSet = currenTaskSet or list()
        self.startTaskId = startTaskId
        self.allTasks = allTasks or dict()
        self.endStatus = endStatus
        self.startTimeMs = startTimeMs
        self.endTimeMs = endTimeMs
        self.allTasks[Task.SUCCEED_PROCESS_TASK] = Task(Task.SUCCEED_PROCESS_TASK, taskDefinitionId=Task.SUCCEED_PROCESS_TASK, nextPointers={Status.SUCCEEDED:[], Status.FAILED:[], Status.CANCELLED:[]})
        self.allTasks[Task.FAIL_PROCESS_TASK] = Task(Task.FAIL_PROCESS_TASK, taskDefinitionId=Task.FAIL_PROCESS_TASK, nextPointers={Status.SUCCEEDED:[], Status.FAILED:[], Status.CANCELLED:[]})
         
    def init(self):
        if (self.startTaskId == None):
            raise Exception("Process hasn't been initialized correctly")
