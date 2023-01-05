from abc import ABC
import json

import requests
from slate.slate_resource import Resource
from slate.slate_process_commons import Status
from slate.slate_utils import EntityEncoder
from typing import List


class AbstractResourceDB(ABC):
    
    def init(self, configuration):
        pass
    
    def getResourceById(self, id:str) -> Resource:
        raise Exception("Unsupported method")
    
    def getResourcesById(self, ids:List[str]) -> List[Resource]:
        result = []
        for id in ids:
            result.append(getResourceById(id))
        return result

    
class RPCResourceDB(AbstractResourceDB):
    
    BASE_API = "/api/v2/"
    
    def init(self, configuration):
        self.slateCoreUrl = configuration["slateCoreUrl"]
        self.headers = {}
        if ("mesh" in self.slateCoreUrl):
            self.headers = {
                "Host": self.slateCoreUrl.replace("mesh://", ""),
                "Content-Type": "application/json"
            }
            self.slateCoreUrl = "http://localhost:19193"
        print("Configured core url to:" + self.slateCoreUrl)
    
    def getResourceById(self, id:str) -> Resource:
        resp = requests.get(self.slateCoreUrl + self.BASE_API + "resources/" + id, headers=self.headers)
        if resp.status_code == 200:
            return Resource(**resp.json())
        else:
            raise Exception("Failed to find resource:" + str(id) + " Exception:" + str(resp.status_code))

        
class HumanTask:

    APPROVAL = "APPROVAL"
    NON_VERIFIABLE = "NON_VERIFIABLE"
    VERIFYABLE = "VERIFYABLE"
    
    def __init__(self, taskId:str, processId:str, executionId:str, assigneeGroupName:str, taskType:str, summary:str, description:str, assigneeUser:str=None, additionalData:str=None, comment:str=None, createTime=None, updateTime=None, taskStatus=Status.NOT_STARTED):
        self.taskId = taskId
        self.processId = processId
        self.executionId = executionId
        self.assigneeGroupName = assigneeGroupName
        self.assigneeUser = assigneeUser
        self.taskType = taskType
        self.taskStatus = taskStatus
        self.summary = summary
        self.description = description
        self.additionalData = additionalData
        self.comment = comment
        self.createTime = createTime
        self.updateTime = updateTime

        
class HumanTaskSystem(ABC):
    
    BASE_API = "/api/v2/hts"
    
    def __init__(self, slateCoreUrl):
        self.slateCoreUrl = slateCoreUrl
        self.headers = {}
        if ("mesh" in self.slateCoreUrl):
            self.headers = {
                "Host": self.slateCoreUrl.replace("mesh://", ""),
                "Content-Type": "application/json"
            }
            self.slateCoreUrl = "http://localhost:19193"
    
    def createHumanTask(self, task:HumanTask):
        resp = requests.put(self.slateCoreUrl + self.BASE_API, headers=self.headers, data=json.dumps(task, cls=EntityEncoder))
        if resp.status_code == 200:
            print("Response:" + str(resp))
            return HumanTask(**resp.json())
        else:
            raise Exception("Failed to create HumanTask:" + str(task) + " resp:" + str(resp.status_code))

    def create(self, taskId:str, processId:str, executionId:str, summary:str, description:str, assigneeGroupName:str, assigneeUser:str, additionalData:str, taskType:str):
        task = HumanTask(taskId=taskId, processId=processId, executionId=executionId, summary=summary, description=description, assigneeGroupName=assigneeGroupName, assigneeUser=assigneeUser, additionalData=additionalData, taskType=taskType, taskStatus=Status.RUNNING)
        return self.createHumanTask(task=task)

    def getTask(self, processId:str, taskId: str):
        resp = requests.get(self.slateCoreUrl + self.BASE_API + "/" + processId + "/" + taskId, headers=self.headers)
        if resp.status_code == 200:
            return HumanTask(**resp.json())
        else:
            raise Exception("Failed to get HumanTask:" + processId + " :" + taskId)

    def updateAssignee(self, processId:str, taskId: str, status:str, comment:str=None):
        resp = requests.put(self.slateCoreUrl + self.BASE_API + "/" + processId + "/" + taskId + "/status/" + status, data=comment, headers=self.headers)
        if resp.status_code == 200:
            return None
        else:
            raise Exception("Failed to create HumanTask:" + processId + " :" + taskId)

    def updateStatus(self, processId:str, taskId: str, status:str, comment:str=None):
        resp = requests.put(self.slateCoreUrl + self.BASE_API + "/" + processId + "/" + taskId + "/status/" + status, data=comment, headers=self.headers)
        if resp.status_code == 204:
            return None
        else:
            raise Exception("Failed to create HumanTask:" + processId + " :" + taskId)
