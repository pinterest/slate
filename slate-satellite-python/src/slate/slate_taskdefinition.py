from abc import abstractmethod, ABC
from slate.slate_process_commons import Status
from slate.slate_process_commons import StatusUpdate
from slate.slate_process import LifecycleProcess
from typing import Dict
from typing import List

class TaskRuntime(ABC):
    
    def __init__(self):
        pass
    
    def configure(self):
        pass
        
    @abstractmethod
    def startExecution(self, taskDefinitionId:str, taskInstanceId:str, process:LifecycleProcess):
        pass
    
    @abstractmethod
    def checkStatus(self, taskDefinitionId:str, taskInstanceId:str, process:LifecycleProcess):
        pass

class TaskDefinition(ABC):
    
    def __init__(self):
        self.taskDefinitionId = ""
        
    @abstractmethod
    def startExecution(self, runtime:TaskRuntime, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict) -> StatusUpdate:
        pass
    
    @abstractmethod
    def checkStatus(self, runtime:TaskRuntime, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict) -> StatusUpdate:
        pass
    
    @abstractmethod
    def validate(self, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict):
        pass
