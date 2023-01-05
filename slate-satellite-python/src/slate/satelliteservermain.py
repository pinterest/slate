from typing import Optional

import json
import yaml
import os

from flask import Flask
from slate.slate_resource import Resource
from slate.slate_resourcefactory import ResourceFactory
from slate.slate_taskfactory import TaskFactory
from slate.slate_utils import EntityEncoder
from slate.slate_resourcedefinition import ResourceChange
from slate.slate_process import LifecycleProcess
from slate.slate_taskfactory import LocalTaskRuntime
from slate.slate_rpc import HumanTaskSystem, RPCResourceDB

from flask import request
import traceback

CONFIG_KEY_SLATE_CORE_URL = "slateCoreUrl"
CONFIG_KEY_ENABLE_DEVELOPMENT = "enableDevelopment"
CONFIG_KEY_RESOURCE_DEF_DIR = "resourceDefinitionScanDirs"
CONFIG_KEY_TASK_DEF_DIR = "taskDefinitionScanDirs"
CONFIG_KEY_TASK_CONFIG_DIR = "taskConfigDir"
CONFIG_KEY_TASK_TMP_DIR = "taskTmpDir"


def createApp(configPath: Optional[str]=None) -> Flask:
    """
    Args:
        configPath: Optional abs/rel path to the config yaml file.

    Returns:
        Flask app.
    """
    thisApp = Flask(__name__)

    if configPath:
        with open(configPath, 'r') as fp:
            thisApp.config.update(yaml.load(fp, Loader=yaml.Loader))
    else:
        thisApp.config.update({
            CONFIG_KEY_SLATE_CORE_URL: os.environ.get('SLATE_CORE_URL', "http://localhost:8090"),
            CONFIG_KEY_ENABLE_DEVELOPMENT: True,
            CONFIG_KEY_RESOURCE_DEF_DIR: os.environ.get('RESOURCE_DEF_DIR', '.').split(","),
            CONFIG_KEY_TASK_DEF_DIR: os.environ.get('TASK_DEF_DIR', '.').split(","),
            CONFIG_KEY_TASK_CONFIG_DIR: os.environ.get('TASK_CONFIG_DIR', '/tmp/taskconfig'),
            CONFIG_KEY_TASK_TMP_DIR: os.environ.get('TASK_TMP_DIR', '/tmp/tasktmp')
        })
    return thisApp


app = createApp(os.environ.get("SLATE_SATELLITE_CONFIG_PATH", None))
slateCoreUrl = app.config[CONFIG_KEY_SLATE_CORE_URL]
resourceDB = RPCResourceDB()
resourceDB.init(app.config)

resourceFactory = ResourceFactory(app.config[CONFIG_KEY_RESOURCE_DEF_DIR], resourceDB)
taskFactory = TaskFactory(app.config[CONFIG_KEY_TASK_DEF_DIR])

hts = HumanTaskSystem(slateCoreUrl)
runtime = LocalTaskRuntime(app.config[CONFIG_KEY_ENABLE_DEVELOPMENT], app.config[CONFIG_KEY_TASK_CONFIG_DIR], app.config[CONFIG_KEY_TASK_TMP_DIR], hts, taskFactory)

backfillIteratorCache = {}


@app.route("/api/v1/resources/definitions", methods=["GET"])
def getResourceDefinitions():
    response = app.response_class(
        response=json.dumps(resourceFactory.resourceMap, cls=EntityEncoder),
        status=200,
        mimetype="application/json"
    )
    return response


@app.route("/api/v1/resources/tags", methods=["GET"])
def getResourceTags():
    response = app.response_class(
        response=json.dumps(resourceFactory.resourceTagMap, cls=EntityEncoder),
        status=200,
        mimetype="application/json"
    )
    return response


@app.route("/api/v1/resources/<resourceDefinitionClass>", methods=["POST"])
def planChange(resourceDefinitionClass):
    change = ResourceChange(**request.get_json())
    try:
        result = resourceFactory.resourceMap[resourceDefinitionClass].planChange(change)
        response = app.response_class(
            response=json.dumps(result, cls=EntityEncoder),
            status=200,
            mimetype="application/json"
        )
        return response
    except Exception as err:
        traceback.print_exc()
        response = app.response_class(
            response=str(err),
            status=400,
            mimetype="application/json"
        )
        return response


@app.route("/api/v1/resources/<resourceDefinitionClass>/currentstate", methods=["POST"])
def getCurrentState(resourceDefinitionClass):
    resource = Resource(**request.get_json())
    result = resourceFactory.resourceMap[resourceDefinitionClass].readExternalCurrentState(resource)
    response = app.response_class(
        response=json.dumps(result, cls=EntityEncoder),
        status=200,
        mimetype="application/json"
    )
    return response


@app.route("/api/v1/resources/<resourceDefinitionClass>/backfill", methods=["GET", "POST"])
def backfill(resourceDefinitionClass):
    pageSize = 10
    if ("pageSize" in request.args.keys()):
        pageSize = int(request.args["pageSize"])
    iterator = None
    if (resourceDefinitionClass in backfillIteratorCache.keys()):
        iterator = backfillIteratorCache[resourceDefinitionClass]
    else:
        iterator = resourceFactory.resourceMap[resourceDefinitionClass].getAllBackfillResources(resourceDB)
        if (iterator is None):
            return app.response_class(
                status=204,
                mimetype="application/json"
            )
        backfillIteratorCache[resourceDefinitionClass] = iterator
    
    result = []
    v = 0
    for i in range(pageSize):
        if (v == pageSize):
            break
        v = v + 1
        try:
            result.append(iterator.__next__())
        except:
            break    
    
    response = None
    if len(result) == 0:
        response = app.response_class(
            status=204,
            mimetype="application/json"
        )
    else:
        response = app.response_class(
            response=json.dumps(result, cls=EntityEncoder),
            status=200,
            mimetype="application/json"
        )
    return response


@app.route("/api/v1/resources/<resourceDefinitionClass>/tools", methods=["POST"])
def getTools(resourceDefinitionClass):
    resource = Resource(**request.get_json())
    result = resourceFactory.resourceMap[resourceDefinitionClass].getTools(resource)
    response = app.response_class(
        response=json.dumps(result, cls=EntityEncoder),
        status=200,
        mimetype="application/json"
    )
    return response


@app.route("/api/v1/resources/<resourceDefinitionClass>/metrics", methods=["POST"])
def getMetrics(resourceDefinitionClass):
    resource = Resource(**request.get_json())
    result = resourceFactory.resourceMap[resourceDefinitionClass].getMetrics(resource)
    dump = json.dumps(result, cls=EntityEncoder)
    response = app.response_class(
        response=dump,
        status=200,
        mimetype="application/json"
    )
    return response


@app.route("/api/v1/tasks/definitions", methods=["GET"])
def getTaskDefinitions():
    result = list(taskFactory.taskMap.keys())
    response = app.response_class(
        response=json.dumps(result, cls=EntityEncoder),
        status=200,
        mimetype="application/json"
    )
    return response


@app.route("/api/v1/tasks/<taskDefinitionId>/<taskInstanceId>/execution", methods=["POST"])
def startExecution(taskDefinitionId, taskInstanceId):
    process = LifecycleProcess(**request.get_json())
    result = runtime.startExecution(taskDefinitionId, taskInstanceId, process)
    response = app.response_class(
        response=json.dumps(result, cls=EntityEncoder),
        status=200,
        mimetype="application/json"
    )
    return response


@app.route("/api/v1/tasks/<taskDefinitionId>/<taskInstanceId>/status", methods=["POST"])
def checkStatus(taskDefinitionId, taskInstanceId):
    process = LifecycleProcess(**request.get_json())
    result = runtime.checkStatus(taskDefinitionId, taskInstanceId, process)
    response = app.response_class(
        response=json.dumps(result, cls=EntityEncoder),
        status=200,
        mimetype="application/json"
    )
    return response


@app.route("/api/v1/tasks/<taskDefinitionId>/<taskInstanceId>/validation", methods=["POST"])
def validation(taskDefinitionId, taskInstanceId):
    process = LifecycleProcess(**request.get_json())
    result = taskFactory.taskMap[taskDefinitionId].validate(taskInstanceId, process, process.processContext, process.processContext[taskInstanceId])
    response = app.response_class(
        response=json.dumps(result, cls=EntityEncoder),
        status=200,
        mimetype="application/json"
    )
    return response


if __name__ == "__main__":
    print(app.url_map)
    app.run()
