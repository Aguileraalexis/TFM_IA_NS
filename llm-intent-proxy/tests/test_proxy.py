import asyncio
import json
import os
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer

import app


class _TestHandler(BaseHTTPRequestHandler):
    cities_response = [{"id": "MADR", "nombre": "Madrid"}, {"id": "LOGR", "nombre": "Logroño"}]
    attractions_response = [{"id": "AT021", "ciudadId": "LOGR", "nombre": "Universidad de La Rioja"}]
    last_openai_body = None

    def _write_json(self, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/ciudades":
            self._write_json(self.cities_response)
            return
        if self.path == "/atractivos":
            self._write_json(self.attractions_response)
            return
        self.send_error(404)

    def do_POST(self):
        if self.path != "/v1/chat/completions":
            self.send_error(404)
            return

        length = int(self.headers.get("Content-Length", "0"))
        _TestHandler.last_openai_body = self.rfile.read(length).decode("utf-8")
        upstream_response = {
            "choices": [
                {
                    "message": {
                        "content": json.dumps(
                            {
                                "intent": "plan_trip",
                                "entities": {
                                    "originCityId": "MADR",
                                    "targetCityIds": "LOGR",
                                    "requestedAttractionIds": "AT021",
                                    "travelDate": "2026-07-10",
                                },
                                "constraints": {},
                                "confidence": 0.91,
                            },
                            ensure_ascii=False,
                        )
                    }
                }
            ]
        }
        self._write_json(upstream_response)

    def log_message(self, format, *args):
        return


class ProxyHelpersTest(unittest.TestCase):
    def setUp(self):
        self._env_backup = os.environ.copy()
        _TestHandler.last_openai_body = None
        self.server = HTTPServer(("127.0.0.1", 0), _TestHandler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base_url = f"http://127.0.0.1:{self.server.server_port}"

    def tearDown(self):
        self.server.shutdown()
        self.thread.join(timeout=5)
        self.server.server_close()
        os.environ.clear()
        os.environ.update(self._env_backup)

    def test_normalize_json_text(self):
        normalized = app._normalize_json_text('{"intent":"plan_trip","entities":{},"constraints":{},"confidence":0.9}')
        parsed = json.loads(normalized)
        self.assertEqual(parsed["intent"], "plan_trip")

    def test_runtime_catalog_is_rendered_and_forwarded_to_openai(self):
        os.environ["OPENAI_API_KEY"] = "test-key"
        os.environ["OPENAI_BASE_URL"] = self.base_url

        request = app.InterpretRequest.model_validate(
            {
                "prompt": "Quiero viajar desde Madrid a Logrono para visitar Universidad de La Rioja el 2026-07-10",
                "provider": "openai",
                "model": "gpt-4o-mini",
                "system": "Cities:\n{{CITY_CATALOG}}\n\nAttractions:\n{{ATTRACTION_CATALOG}}",
                "catalog-base-url": self.base_url,
                "catalog-cities-path": "/ciudades",
                "catalog-attractions-path": "/atractivos",
            }
        )

        response = asyncio.run(app.interpret(request))

        self.assertIn("\"originCityId\": \"MADR\"", response["content"])
        self.assertIsNotNone(_TestHandler.last_openai_body)

        sent_body = json.loads(_TestHandler.last_openai_body)
        system_prompt = sent_body["messages"][0]["content"]
        self.assertIn("Madrid -> MADR", system_prompt)
        self.assertIn("Logroño -> LOGR", system_prompt)
        self.assertIn("Universidad de La Rioja -> AT021", system_prompt)
        self.assertNotIn("{{CITY_CATALOG}}", system_prompt)
        self.assertNotIn("{{ATTRACTION_CATALOG}}", system_prompt)

    def test_selected_provider_invalid(self):
        with self.assertRaises(Exception):
            app._selected_provider({"provider": "unknown"})


if __name__ == "__main__":
    unittest.main()

