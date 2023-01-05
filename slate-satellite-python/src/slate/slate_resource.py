import json


class Resource:
    
    def __init__(self, id: str, resourceDefinitionClass: str, desiredState: dict, environment: str, project: str, owner: str, region: str, inputResources=None, outputResources=None, resourceWatchList=None, deleted=False, resourceLockOwner=None, lastUpdateTimestamp=0, parentResource=None, childResources=None):
        self.id = id
        self.resourceDefinitionClass = resourceDefinitionClass
        self.resourceLockOwner = resourceLockOwner
        self.desiredState = desiredState
        self.environment = environment
        self.owner = owner
        self.project = project
        self.region = region
        self.resourceWatchList = resourceWatchList or list()
        self.inputResources = inputResources or dict()
        self.outputResources = outputResources or dict()
        self.deleted = deleted
        self.lastUpdateTimestamp = lastUpdateTimestamp
        self.parentResource = parentResource
        self.childResources = childResources
        
    def __eq__(self, other):
        return self.__dict__ == other.__dict__
        
class Tool:
    
    def __init__(self, label:str=None, url:str=None, description:str=None, embed:bool=False):
        self.label = label
        self.url = url
        self.description = description
        self.embed = embed
