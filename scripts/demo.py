"""Gera eventos reais na API local; usa exclusivamente as credenciais de demonstração."""
import base64
import json
import urllib.request
import urllib.error
BASE = "http://localhost:8080"
def call(method, path, body=None, auth=None):
    headers = {"Content-Type": "application/json"}
    if auth:
        headers["Authorization"] = "Basic " + base64.b64encode(auth.encode()).decode()
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as res:
            print(method, path, res.status, res.read().decode())
            return res.status
    except urllib.error.HTTPError as err:
        print(method, path, err.code, err.read().decode())
        return err.code
if __name__ == "__main__":
    assert call("PUT", "/unlock/aluno", auth="admin:Admin123!") == 200
    for _ in range(3):
        assert call("POST", "/login", {"username": "aluno", "senha": "Aluno123!"}) == 200
    for _ in range(2):
        assert call("POST", "/login", {"username": "aluno", "senha": "errada"}) == 401
    print("Aguarde a coleta do Prometheus e abra o dashboard. Aluno está bloqueado.")
