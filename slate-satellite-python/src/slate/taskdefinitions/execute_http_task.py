from ..slate_process import LifecycleProcess
from ..slate_taskdefinition import TaskRuntime
from ..slate_taskdefinition import TaskDefinition
from ..slate_process_commons import StatusUpdate
from ..slate_process_commons import Task
from ..slate_process_commons import Status
from ..slate_rpc import HumanTask
from abc import abstractmethod
from typing import Dict
from typing import Set
import requests
import json
import contextlib
try:
    from http.client import HTTPConnection # py3
except ImportError:
    from httplib import HTTPConnection # py2
import logging

URL = "url"
USE_SSL = "useSSL"
METHOD = "method"
DATA = "data"
SUCCESS_CODES = "successCodes"
RESULT = "result"

def debug_requests_on():
    '''Switches on logging of the requests module.'''
    HTTPConnection.debuglevel = 1

    logging.basicConfig()
    logging.getLogger().setLevel(logging.DEBUG)
    requests_log = logging.getLogger("requests.packages.urllib3")
    requests_log.setLevel(logging.DEBUG)
    requests_log.propagate = True

def debug_requests_off():
    '''Switches off logging of the requests module, might be some side-effects'''
    HTTPConnection.debuglevel = 0

    root_logger = logging.getLogger()
    root_logger.setLevel(logging.WARNING)
    root_logger.handlers = []
    requests_log = logging.getLogger("requests.packages.urllib3")
    requests_log.setLevel(logging.WARNING)
    requests_log.propagate = False


class ExecuteHttpCall(TaskDefinition):
    
    def __init__(self, taskDefinitionId):
        self.taskDefinitionId = taskDefinitionId
        #debug_requests_on()
    
    @abstractmethod
    def startExecution(self, runtime:TaskRuntime, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict) -> StatusUpdate:
        url = str(taskContext[URL])
        method = str(taskContext[METHOD]).upper()
        data = None
        headers = None
        if (DATA in taskContext):
            data = taskContext[DATA]
            headers = {"Content-Type": "application/json"}
            
        successCodes = [200, 201, 204]
        if (SUCCESS_CODES in taskContext.keys()):
            successCodes = taskContext[SUCCESS_CODES]
        
        resp = None
        if (method == "GET"):
            resp = requests.get(url, json=data, headers=headers)
        elif (method == "POST"):
            resp = requests.post(url, json=data, headers=headers)
        elif(method == "PUT"):
            resp = requests.put(url, json=data, headers=headers)
        elif(method == "DELETE"):
            resp = requests.delete(url, json=data, headers=headers)
        else:
            return StatusUpdate(Status.FAILED)
        
        if (resp.status_code in successCodes):
            print("Request succeeded")
            return StatusUpdate(Status.SUCCEEDED)
        else:
            print("Failed code:" + str(resp.status_code) + " reason:" + str(resp.reason) + " data:" + json.dumps(data) + " url:" + url)
            return StatusUpdate(Status.FAILED, "Failed:" + str(resp.status_code) + " " + str(resp.reason))
    
    def checkStatus(self, runtime:TaskRuntime, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict) -> StatusUpdate:
        return None
    
    def validate(self, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict):
        if (METHOD not in taskContext):
            raise Exception("Missing method")
        if (taskContext[METHOD] not in ["GET", "POST", "PUT", "DELETE"]):
            raise Exception("Unsupported method:" + taskContext["method"])
        if (URL not in taskContext):
            raise Exception("Missing url")
        url = taskContext[URL]
        if (USE_SSL in taskContext):
            # verify that url is https
            if (taskContext[USE_SSL] and (not url.startswith("https"))):
                raise Exception("URL to use SSL must be https://")
        return
