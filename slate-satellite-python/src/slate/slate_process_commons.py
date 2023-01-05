from typing import Optional, List


class ProcessType(object):
    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"


class Status(object):
    RUNNING = "RUNNING"
    NOT_STARTED = "NOT_STARTED"
    FAILED = "FAILED"
    SUCCEEDED = "SUCCEEDED"
    CANCELLED = "CANCELLED"

    
class StatusUpdate(object):
    
    def __init__(self, status, stdOut: Optional[str] = None, stdErr: Optional[str] = None,
                 processContext: Optional[List] = None):
        self.__dict__ = {}
        self.status = status
        self.stdOut = stdOut
        self.stdErr = stdErr
        self.processContextUpdate = processContext or []


class Task(object):
    
    SUCCEED_PROCESS_TASK = "succeedProcess"
    FAIL_PROCESS_TASK = "failProcess"
    
    def __init__(self, instanceId:str, taskDefinitionId:str, nextPointers=None, startTimeMs=0, endTimeMs=0, status=Status.NOT_STARTED, stdOut=None, stdErr=None):
        self.__dict__ = {}
        self.instanceId = instanceId
        self.taskDefinitionId = taskDefinitionId
        self.startTimeMs = startTimeMs
        self.endTimeMs = endTimeMs
        self.status = status
        self.nextPointers = nextPointers or dict()
        self.stdOut = stdOut or list()
        self.stdErr = stdErr or list()
