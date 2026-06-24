import json
import unittest

import app


class ProxyHelpersTest(unittest.TestCase):
    def test_normalize_json_text(self):
        normalized = app._normalize_json_text('{"intent":"plan_trip","entities":{},"constraints":{},"confidence":0.9}')
        parsed = json.loads(normalized)
        self.assertEqual(parsed["intent"], "plan_trip")

    def test_selected_provider_invalid(self):
        with self.assertRaises(Exception):
            app._selected_provider({"provider": "unknown"})


if __name__ == "__main__":
    unittest.main()

