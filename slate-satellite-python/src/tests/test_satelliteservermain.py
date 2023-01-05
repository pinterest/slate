import os
import unittest

from slate.satelliteservermain import createApp
from slate.satelliteservermain import CONFIG_KEY_SLATE_CORE_URL
from slate.satelliteservermain import CONFIG_KEY_ENABLE_DEVELOPMENT
from slate.satelliteservermain import CONFIG_KEY_RESOURCE_DEF_DIR
from slate.satelliteservermain import CONFIG_KEY_TASK_DEF_DIR


class FlaskSatelliteAppTestCase(unittest.TestCase):

    def testYamlContentsAreLoadedToConfig(self):
        resDirPath = f"{os.path.dirname(os.path.realpath(__file__))}/resources"
        app = createApp(f"{resDirPath}/test_config.yaml")

        expected = {
            CONFIG_KEY_SLATE_CORE_URL: "http://some_url",
            CONFIG_KEY_ENABLE_DEVELOPMENT: True,
            CONFIG_KEY_RESOURCE_DEF_DIR: ["./some_rel_dir"],
            CONFIG_KEY_TASK_DEF_DIR: ["/opt/some_abs_dir"],
        }

        self.assertEqual(
            app.config,
            app.config | expected
        )