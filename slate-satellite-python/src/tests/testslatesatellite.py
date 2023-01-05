'''
Created on Mar 14, 2022

@author: ambudsharma
'''


import unittest
from io import BytesIO
import base64
import json
from slate.slate_utils import EntityEncoder
from slate.slate_resource import Resource
from slate.slate_process import LifecycleProcess


class Test(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testDeserializationToResourceObject(self):
        counter = 1
        resourceJson = """
        {"desiredState":{"schemaDefinition":{"protocol":"thrift","thriftType":"com.pinterest.test.TestClass"},
        "pubSubConfigs":{"pubSubType":"kafka","partitions":12,"replicationFactor":3,"selectCluster":true,"cluster":"datakafka08","brokerset":"Capacity_B12_P12_0",
        "clusterFamily":"organic","stride":0},"capacity":{"capacityType":"By Volume (GB)","dailyTrafficGB":10},
        "retentionHours":12,"compliance":{"isPII":false,"isSOX":false},"customConfigs":{},"name":"test"},"id":"newnode_0","owner":"test",
        "project":"testd","region":"us-east-1","resourceDefinitionClass":
        "com.pinterest.slate.resources.logging.pubsub.PubSubTopicDefinition",
        "environment":"prod"
        }
        """
        resource = Resource(**json.loads(resourceJson))
        assert resource != None, "The resource shouldn't be null"
        assert resource.desiredState.get("name") == 'test', "This resource name is should be 'test'"
        assert resource.desiredState.get("schemaDefinition").get("protocol") == 'thrift', "This resource should have nested protocol field = thrift"
        
        # reserialize and then deserialize to compare the objects
        reEncodedJson = json.dumps(resource, cls=EntityEncoder)
        re2Resource = Resource(**json.loads(reEncodedJson))
        assert json.dumps(resource, cls=EntityEncoder) == json.dumps(re2Resource, cls=EntityEncoder), "Resources are not equal"
        
    def testProcessAndTask(self):
        LifecycleProcess(processId="test")

if __name__ == "__main__":
    # import sys;sys.argv = ['', 'Test.testName']
    unittest.main()
