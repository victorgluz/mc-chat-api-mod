package br.com.arcasoltec.modchatapi;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModChatApiClient implements ClientModInitializer {
	public static final String MOD_ID = "modchatapi";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ModConfig config;
	private static final ChatApiService SERVICE = new ChatApiService();
	private static final Pattern WHISPER_PATTERN = Pattern.compile("^(\\S+) whispers to you: (.+)$");

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
			String blockReason = blockReason(sender);
			if (blockReason != null) {
				LOGGER.info("[debug] ignorado: {}", blockReason);
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
					sender != null ? sender.getId() : null,
					null
			);
		});

		// Mensagens de sistema (whispers e feedback de comandos, join/leave, etc.)
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay || !config.enabled) {
				return;
			}
			String text = message.getString();

			Matcher m = WHISPER_PATTERN.matcher(text);
			if (m.matches()) {
				String senderName = m.group(1);
				String whisperText = m.group(2);
				if (!config.whitelist.isEmpty()
						&& config.whitelist.stream().noneMatch(n -> n.equalsIgnoreCase(senderName))) {
					LOGGER.info("[debug] whisper ignorado: {} fora da whitelist", senderName);
					return;
				}
				LOGGER.info("[debug] whisper de {}: {}", senderName, whisperText);
				SERVICE.forwardMessage("tell", whisperText, senderName, null, senderName);
				return;
			}

			if (!config.listenToSystemMessages) {
				return;
			}
			if (!matchesPrefix(text)) {
				return;
			}
			SERVICE.forwardMessage("game", text, null, null, null);
		});

		ChatApiCommands.register();

		AutoEatHandler autoEatHandler = new AutoEatHandler();
		ClientTickEvents.END_CLIENT_TICK.register(autoEatHandler::tick);

		LOGGER.info("Mod Chat API inicializado. Endpoint: {}", config.endpointUrl);
	}

	/** Retorna o motivo para NÃO encaminhar a mensagem, ou null se deve encaminhar. */
	private String blockReason(GameProfile sender) {
		if (!config.enabled) {
			return "mod desativado";
		}
		if (config.ignoreOwnMessages && sender != null) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null && client.player.getUuid().equals(sender.getId())) {
				return "mensagem do próprio jogador (ignoreOwnMessages)";
			}
		}
		if (!config.whitelist.isEmpty()) {
			if (sender == null
					|| config.whitelist.stream().noneMatch(n -> n.equalsIgnoreCase(sender.getName()))) {
				return "jogador fora da whitelist";
			}
		}
		return null;
	}

	private boolean matchesPrefix(String text) {
		return config.triggerPrefix == null
				|| config.triggerPrefix.isEmpty()
				|| text.startsWith(config.triggerPrefix);
	}
}
