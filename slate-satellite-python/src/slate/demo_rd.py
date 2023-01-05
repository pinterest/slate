from time import sleep
from slate.slate_resourcedefinition import ResourceDefinition
from slate.slate_resourcedefinition import Plan
from slate.slate_resourcedefinition import ResourceChange
from slate.slate_resource import Resource
from slate.slate_resource import Tool
from slate.slate_rpc import AbstractResourceDB
from typing import Dict
from typing import Set
from slate.slate_process import LifecycleProcess
from slate.slate_taskdefinition import TaskRuntime
from slate.slate_taskdefinition import TaskDefinition
from slate.slate_process_commons import StatusUpdate
from slate.slate_process_commons import Task
from slate.slate_process_commons import Status
from slate.slate_rpc import HumanTask


class DemoRD(ResourceDefinition):
    
    def __init__(self):
         super().__init__()
         self.simpleName = "DemoPythonResource"
         self.shortDescription = "Simple ResourceDefinition to test python Slate Satellites"
         self.configSchema = {
            "type": "object",
            "title": "PubSubTopic",
            "properties": {
                "name": {
                  "type": "string",
                  "title": "Name",
                  "minLength": 1,
                  "maxLength": 130
                }
            },
            "required": ["name"]
        }
         
    def init(self, configDirectory:str, graph:AbstractResourceDB):
        pass
        
    def planChange(self, change: ResourceChange) -> Plan:
        process = LifecycleProcess()
        process.processContext["demoTask"] = {
            "value": "val1"
        }
        process.processContext["approval"] = {
            "assigneeUser": "ambudsharma",
            "approvalGroup": "loggingteam",
            "description": "Test approvals from python",
            "summary": "Test approvals from python"
        }

        process.allTasks["approval"] = Task("approval", "groupApprovalTask", nextPointers={Status.SUCCEEDED:["demoTask"], Status.FAILED:[Task.FAIL_PROCESS_TASK], Status.CANCELLED:[Task.FAIL_PROCESS_TASK]})

        process.allTasks["demoTask"] = Task("demoTask", "demoTask", nextPointers={Status.SUCCEEDED:[Task.SUCCEED_PROCESS_TASK], Status.FAILED:[Task.FAIL_PROCESS_TASK], Status.CANCELLED:[Task.FAIL_PROCESS_TASK]})
        process.startTaskId = "approval"
        
        o = change.proposedResourceObject
        # prn:memq:%s:aws_%s::%s:%s", environment, region, cluster, topicName
        return Plan(change.proposedResourceObject, process, "prn:demo_slate_python:" + o.environment + ":aws_" + o.region + "::" + o.desiredState['name'])
    
    def readExternalCurrentState(self, resource:Resource) -> Dict:
        return resource.desiredState
    
    def getTools(self, Resource) -> Set[Tool]:
        pass

    def getTags(self) -> Set[str]:
        return ["DemoTeam"]

    
class DemoTaskDef(TaskDefinition):
    
    def __init__(self):
        self.taskDefinitionId = "demoTask"
    
    def startExecution(self, runtime:TaskRuntime, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict) -> StatusUpdate:
        runtime.hts.create(taskId=taskId, processId=process.processId, executionId=process.executionId, summary="Python verifyable task", description="Simple python verifyable task", assigneeGroupName="loggingteam", assigneeUser="ambudsharma", additionalData=None, taskType=HumanTask.VERIFYABLE)
        print("task started:" + process.processId)
        sleep(5)
        return StatusUpdate(Status.RUNNING)
    
    def checkStatus(self, runtime:TaskRuntime, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict) -> StatusUpdate:
        runtime.hts.updateStatus(processId=process.processId, taskId=taskId, status=Status.SUCCEEDED)
        print("task completed")
        return StatusUpdate(Status.SUCCEEDED)
    
    def validate(self, taskId:str, process:LifecycleProcess, processContext:Dict, taskContext:Dict):
        print("validation completed:" + str)
        return
 
