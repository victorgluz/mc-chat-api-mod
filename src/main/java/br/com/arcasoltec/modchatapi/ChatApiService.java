package br.com.arcasoltec.modchatapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Envia mensagens do chat para o web app e executa a ação retornada.
 *
 * Contrato da API — o web app recebe via POST:
 *   { "type": "chat"|"game", "message": "...", "senderName": "...", "senderUuid": "...", "timestamp": 123 }
 * e deve responder:
 *   { "type": "command", "value": "give @s diamond" }   -> executa como comando (com ou sem "/")
 *   { "type": "chat",    "value": "olá!" }              -> envia como mensagem no chat
 *   { "type": "none" }                                  -> não faz nada
 */
public class ChatApiService {
	private static final Gson GSON = new Gson();

	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	public void forwardMessage(String type, String message, String senderName, UUID senderUuid) {
		ModConfig config = ModChatApiClient.getConfig();

		JsonObject payload = new JsonObject();
		payload.addProperty("type", type);
		payload.addProperty("message", message);
		payload.addProperty("senderName", senderName);
		payload.addProperty("senderUuid", senderUuid != null ? senderUuid.toString() : null);
		payload.addProperty("timestamp", System.currentTimeMillis());

		HttpRequest request;
		try {
			request = HttpRequest.newBuilder()
					.uri(URI.create(config.endpointUrl))
					.timeout(Duration.ofMillis(config.timeoutMs))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
					.build();
		} catch (IllegalArgumentException e) {
			ModChatApiClient.LOGGER.error("URL do endpoint inválida: {}", config.endpointUrl, e);
			return;
		}

		httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenAccept(this::handleResponse)
				.exceptionally(e -> {
					ModChatApiClient.LOGGER.error("Falha na requisição para {}", config.endpointUrl, e);
					return null;
				});
	}

	private void handleResponse(HttpResponse<String> response) {
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			ModChatApiClient.LOGGER.warn("Web app respondeu HTTP {}: {}", response.statusCode(), response.body());
			return;
		}

		String body = response.body();
		if (body == null || body.isBlank()) {
			return;
		}

		JsonObject json;
		try {
			json = GSON.fromJson(body, JsonObject.class);
		} catch (Exception e) {
			ModChatApiClient.LOGGER.error("Resposta do web app não é JSON válido: {}", body, e);
			return;
		}
		if (json == null || !json.has("type")) {
			return;
		}

		String actionType = json.get("type").getAsString();
		String value = json.has("value") && !json.get("value").isJsonNull()
				? json.get("value").getAsString()
				: null;

		// Interação com o jogo precisa acontecer na thread do cliente.
		MinecraftClient client = MinecraftClient.getInstance();
		client.execute(() -> executeAction(client, actionType, value));
	}

	private void executeAction(MinecraftClient client, String actionType, String value) {
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		if (networkHandler == null) {
			ModChatApiClient.LOGGER.warn("Resposta recebida fora de um mundo, ação ignorada: {} {}", actionType, value);
			return;
		}

		switch (actionType) {
			case "command" -> {
				if (value == null || value.isBlank()) {
					return;
				}
				String command = value.startsWith("/") ? value.substring(1) : value;
				networkHandler.sendChatCommand(command);
				ModChatApiClient.LOGGER.info("Comando executado pelo web app: /{}", command);
			}
			case "chat" -> {
				if (value == null || value.isBlank()) {
					return;
				}
				networkHandler.sendChatMessage(value);
				ModChatApiClient.LOGGER.info("Mensagem enviada pelo web app: {}", value);
			}
			case "none" -> {
				// Web app decidiu não agir.
			}
			default -> {
				ModChatApiClient.LOGGER.warn("Tipo de ação desconhecido na resposta: {}", actionType);
				if (client.player != null) {
					client.player.sendMessage(
							Text.literal("[ChatAPI] Tipo de ação desconhecido: " + actionType), false);
				}
			}
		}
	}
}
