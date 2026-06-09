# Mod Chat API

Mod **client-side** (Fabric, Minecraft 1.21.8) que intercepta cada mensagem do chat, envia para um web app via HTTP POST, aguarda a resposta e executa a ação retornada — um comando ou uma mensagem no chat.

## Como funciona

```
[chat recebido] -> POST endpoint -> resposta JSON -> executa comando / envia mensagem
```

1. O mod escuta as mensagens de chat recebidas pelo cliente (Fabric API `ClientReceiveMessageEvents`).
2. Cada mensagem é enviada de forma **assíncrona** (não trava o jogo) para o endpoint configurado.
3. A resposta do web app decide o que fazer.

## Contrato da API

### Request (mod -> web app)

`POST` no endpoint configurado, `Content-Type: application/json`:

```json
{
  "type": "chat",
  "message": "<Steve> oi, me dá um diamante",
  "senderName": "Steve",
  "senderUuid": "8667ba71-b85a-4004-af54-457a9734eed7",
  "timestamp": 1765300000000
}
```

- `type`: `"chat"` (mensagem de jogador) ou `"game"` (mensagem de sistema, se habilitado na config).
- `senderName`/`senderUuid`: `null` para mensagens de sistema.

### Response (web app -> mod)

```json
{ "type": "command", "value": "give @s diamond" }
```

| `type`    | Efeito                                                        |
|-----------|---------------------------------------------------------------|
| `command` | Executa `value` como comando (com ou sem `/` no início)        |
| `chat`    | Envia `value` como mensagem de texto no chat                   |
| `none`    | Não faz nada                                                   |

## Configuração

Arquivo `config/modchatapi.json` (criado no primeiro uso):

```json
{
  "enabled": true,
  "endpointUrl": "http://localhost:8080/api/chat",
  "timeoutMs": 10000,
  "ignoreOwnMessages": true,
  "listenToSystemMessages": false,
  "triggerPrefix": ""
}
```

- `ignoreOwnMessages`: ignora suas próprias mensagens — **importante** para evitar loop infinito quando o web app responde com `"type": "chat"`.
- `triggerPrefix`: se preenchido (ex.: `"!"`), só encaminha mensagens que começam com esse prefixo.

### Comando in-game

```
/chatapi on | off | status | url <endpoint>
```

(comando client-side; não aparece para outros jogadores)

## Build

Requer JDK 21:

```bash
./gradlew build
```

O jar fica em `build/libs/mod-chat-api-1.0.0.jar`. Copie para a pasta `mods/` do seu Minecraft (com Fabric Loader e Fabric API instalados).

## Testar rápido

Servidor de exemplo em `example-server/server.py` (sem dependências, só Python 3):

```bash
python3 example-server/server.py
```

Ele responde a qualquer mensagem contendo "diamante" com um comando `/give`, e ecoa as demais no chat.
