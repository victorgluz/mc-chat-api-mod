package br.com.arcasoltec.modchatapi;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModChatApiClient implements ClientModInitializer {
	public static final String MOD_ID = "modchatapi";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ModConfig config;
	private static final ChatApiService SERVICE = new ChatApiService();

	public static ModConfig getConfig() {
		return config;
	}

	@Override
	public void onInitializeClient() {
		config = ModConfig.load();

		// Mensagens de chat de jogadores.
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			String text = message.getString();
			LOGGER.info("[debug] chat recebido de {}: {}", sender != null ? sender.getName() : "(null)", text);
			if (!shouldForward(sender)) {
				LOGGER.info("[debug] ignorado: {}", config.enabled ? "mensagem do próprio jogador (ignoreOwnMessages)" : "mod desativado");
				return;
			}
			if (!matchesPrefix(text)) {
				LOGGER.info("[debug] ignorado: não começa com o prefixo '{}'", config.triggerPrefix);
				return;
			}
			SERVICE.forwardMessage(
					"chat",
					text,
					sender != null ? sender.getName() : null,
					sender != null ? sender.getId() : null
			);
		});

		// Mensagens de sistema (feedback de comandos, join/leave, etc.) — opcional via config.
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay || !config.enabled || !config.listenToSystemMessages) {
				return;
			}
			String text = message.getString();
			if (!matchesPrefix(text)) {
				return;
			}
			SERVICE.forwardMessage("game", text, null, null);
		});

		ChatApiCommands.register();

		LOGGER.info("Mod Chat API inicializado. Endpoint: {}", config.endpointUrl);
	}

	private boolean shouldForward(GameProfile sender) {
		if (!config.enabled) {
			return false;
		}
		if (config.ignoreOwnMessages && sender != null) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null && client.player.getUuid().equals(sender.getId())) {
				return false;
			}
		}
		return true;
	}

	private boolean matchesPrefix(String text) {
		return config.triggerPrefix == null
				|| config.triggerPrefix.isEmpty()
				|| text.startsWith(config.triggerPrefix);
	}
}
