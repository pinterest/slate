from typing import List

from slate.slate_resourcedefinition import ResourceDefinition
import os
import sys
import json
import traceback
import inspect
from slate.slate_rpc import AbstractResourceDB


def all_files(directory):
    for path, dirs, files in os.walk(directory):
        for f in files:
            if f.endswith('.py'):
                yield os.path.join(path, f)


# Found here: 
# https://stackoverflow.com/questions/3137731/is-this-correct-way-to-import-python-scripts-residing-in-arbitrary-folders
def import_from_absolute_path(fullpath, global_name=None):
    script_dir, filename = os.path.split(fullpath)
    script, ext = os.path.splitext(filename)

    sys.path.insert(0, script_dir)
    try:
        module = __import__(script)
        if global_name is None:
            global_name = script
        globals()[global_name] = module
        sys.modules[global_name] = module
    except ModuleNotFoundError as mnf:
        print("Module load failed:" + str(mnf) + " " + fullpath)
        raise
    except ImportError as ie:
        print(ie)
    except FileNotFoundError as fnf:
        print(fnf)
    finally:
        del sys.path[0]


def find_resource_definition_classes(full_file_path: str):
    """
    Find all the resource definition subclasses in the given file path.

    Args:
        full_file_path: str

    Returns:
        list of class object
    """
    script_dir, filename = os.path.split(full_file_path)
    script, ext = os.path.splitext(filename)

    if script in sys.modules:
        return [cls_obj for cls_name, cls_obj in inspect.getmembers(sys.modules[script])
                if inspect.isclass(cls_obj) and issubclass(cls_obj, ResourceDefinition)
                and cls_obj is not ResourceDefinition]
    else:
        return []


class ResourceFactory:

    def __new__(cls, importdirs: List[str] = [], resourceDB: AbstractResourceDB = None):
        if not hasattr(cls, 'instance'):
            cls.instance = super(ResourceFactory, cls).__new__(cls)
        return cls.instance

    def __init__(self, importdirs: List[str], resourceDB: AbstractResourceDB):
        self.resourceMap = {}
        self.resourceTagMap = {}
        for d in importdirs:
            for f in all_files(d):
                if "slate_resourcefactory" in f:
                    continue
                if "satelliteservermain" in f:
                    continue
                import_from_absolute_path(f)
                resource_definition_classes = find_resource_definition_classes(f)

                for cls in resource_definition_classes:
                    classname = cls.__module__ + "." + cls.__qualname__
                    obj = cls()
                    if inspect.isabstract(cls):
                        print("Ignoring:" + classname + " because it's abstract")
                        continue

                    # process schema and inject required fields
                    obj.configSchema

                    print("Loaded RD:" + classname)
                    self.resourceMap[classname] = obj
                    obj.init("", resourceDB)
                    tags = obj.getTags()
                    if tags is None:
                        continue
                    for t in tags:
                        if t not in self.resourceTagMap.keys():
                            self.resourceTagMap[t] = []
                        self.resourceTagMap[t].append(classname)

    def autoFixConfigSchema(self, resourceDefinitionObject: ResourceDefinition):
        val = json.loads(resourceDefinitionObject.configSchema)
