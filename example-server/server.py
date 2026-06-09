#!/usr/bin/env python3
"""Web app de exemplo para testar o Mod Chat API.

Roda em http://localhost:8080/api/chat sem dependências externas.
"""
import json
from http.server import BaseHTTPRequestHandler, HTTPServer


class ChatHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        payload = json.loads(self.rfile.read(length))
        print(f"Recebido: {payload}")

        message = payload.get("message", "").lower()
        sender = payload.get("senderName") or "sistema"

        if "diamante" in message:
            response = {"type": "command", "value": "give @s diamond 1"}
        elif "oi" in message or "olá" in message:
            response = {"type": "chat", "value": f"Olá, {sender}! Sou um bot."}
        else:
            response = {"type": "none"}

        body = json.dumps(response).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        pass  # silencia o log padrão; já imprimimos o payload acima


if __name__ == "__main__":
    print("Escutando em http://localhost:8080/api/chat")
    HTTPServer(("0.0.0.0", 8080), ChatHandler).serve_forever()
