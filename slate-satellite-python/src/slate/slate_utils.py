from json import JSONEncoder
import sys

class EntityEncoder(JSONEncoder):
    
    def default(self, o):
        try:
            return o.__dict__
        except BaseException:
            print(o.__class__)
            print(o)
            raise
            