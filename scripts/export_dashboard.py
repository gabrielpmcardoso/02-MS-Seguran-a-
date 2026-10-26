"""Exporta o dashboard real do Grafana local (sem credenciais no arquivo exportado)."""
import base64
import json
import os
from pathlib import Path
import urllib.request
headers = {"Authorization": "Basic " + base64.b64encode(("admin:" + os.environ.get("GRAFANA_PASSWORD", "Grafana123!")).encode()).decode()}
request = urllib.request.Request("http://localhost:3000/api/dashboards/uid/login-security", headers=headers)
with urllib.request.urlopen(request) as response:
    dashboard = json.load(response)["dashboard"]
dashboard["id"] = None
output = Path(__file__).resolve().parents[1] / "docs" / "dashboard-exportado.json"
output.parent.mkdir(exist_ok=True)
output.write_text(json.dumps(dashboard, ensure_ascii=False, indent=2), encoding="utf-8")
print(output)
