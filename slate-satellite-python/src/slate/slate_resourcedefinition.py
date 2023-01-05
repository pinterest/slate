from abc import abstractmethod, ABC
from slate.slate_resource import Resource
from slate.slate_resource import Tool
from slate.slate_process import LifecycleProcess
from typing import Dict
from typing import List
from typing import Set
from slate.slate_rpc import AbstractResourceDB

class EdgeDefinition(object):

    def __init__(self,
                 connectedResourceType: List[str],
                 minCardinality: int = 0,
                 maxCardinality: int = 100):
        self.connectedResourceType = connectedResourceType
        self.minCardinality = minCardinality
        self.maxCardinality = maxCardinality

    def __str__(self):
        return f"EdgeDefinition [connectionType={self.connectedResourceType}, " \
               f"minCardinality={self.minCardinality}, maxCardinality={self.maxCardinality}"


class Plan(object):

    def __init__(self, proposedResource: Resource, process: LifecycleProcess = None, updatedResourceId=None,
                 upstreamVertexDependencyIds = list()):
        self.__dict__ = {}
        self.proposedResource = proposedResource
        self.process = process
        self.updatedResourceId = updatedResourceId
        self.upstreamVertexDependencyIds = upstreamVertexDependencyIds


class PlanError(Exception):
    pass


class MetricsDefinition:

    def __init__(self, metricLabel: str, query: str, warnThreshold: float, severeThreshold: float):
        self.metricLabel = metricLabel
        self.query = query
        self.warnThreshold = warnThreshold
        self.severeThreshold = severeThreshold


class ResourceChange(object):

    def __init__(self, requester, proposedResourceObject: Resource, deltaGraph: Dict[str, Resource],
                 currentState: Dict = None, currentResourceObject: Resource = None):
        self.__dict__ = {}
        self.requester = requester
        if isinstance(proposedResourceObject, Dict):
            self.proposedResourceObject = Resource(**proposedResourceObject)
        elif isinstance(proposedResourceObject, Resource):
            self.proposedResourceObject = proposedResourceObject
        self.deltaGraph = deltaGraph
        self.currentState = currentState
        if currentResourceObject is None:
            self.currentResourceObject = None
        else:
            if isinstance(currentResourceObject, Dict):
                self.currentResourceObject = Resource(**currentResourceObject)
            elif isinstance(currentResourceObject, Resource):
                self.currentResourceObject = currentResourceObject


class ResourceDefinition(ABC):

    def __init__(self):
        self.simpleName = ""
        self.configSchema = {}
        self.author = ""
        self.chatLink = ""
        self.documentationLink = ""
        self.shortDescription = ""
        self.uiSchema = {}
        self.requiredInboundEdgeTypes = {}
        self.requiredOutboundEdgeTypes = {}

    @abstractmethod
    def init(self, configDirectory: str, db: AbstractResourceDB):
        pass

    def getAllBackfillResources(self, db: AbstractResourceDB):
        pass

    @abstractmethod
    def planChange(self, change: ResourceChange) -> Plan:
        pass

    @abstractmethod
    def readExternalCurrentState(self, resource: Resource) -> Dict:
        pass

    @abstractmethod
    def getTools(self, resource: Resource) -> Set[Tool]:
        pass

    @abstractmethod
    def getMetrics(self, resource: Resource) -> Set[MetricsDefinition]:
        pass

    def getShortDescription(self) -> str:
        return self.shortDescription

    @abstractmethod
    def getTags(self) -> Set[str]:
        pass

    def getSimpleName(self) -> str:
        return self.simpleName

    def getUiSchema(self) -> Dict:
        return self.uiSchema

    def getDocumentationLink(self) -> str:
        return self.documentationLink

    def getAuthor(self) -> str:
        return self.author

    def getChatLink(self) -> str:
        return self.chatLink

    def getConfigSchema(self) -> Dict:
        return self.configSchema
    
    def getRequiredParentEdgeTypes(self) -> EdgeDefinition:
        pass
    
    def getRequiredChildEdgeTypes(self) -> Set[EdgeDefinition]:
        pass


