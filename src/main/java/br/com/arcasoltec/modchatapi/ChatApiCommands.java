package br.com.arcasoltec.modchatapi;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

/**
 * Comando client-side /chatapi para controlar o mod sem editar o arquivo de config:
 *   /chatapi on | off | status | url <endpoint>
 */
public final class ChatApiCommands {
	private ChatApiCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("chatapi")
						.then(ClientCommandManager.literal("on").executes(ctx -> {
							ModConfig config = ModChatApiClient.getConfig();
							config.enabled = true;
							config.save();
							ctx.getSource().sendFeedback(Text.literal("[ChatAPI] Ativado."));
							return 1;
						}))
						.then(ClientCommandManager.literal("off").executes(ctx -> {
							ModConfig config = ModChatApiClient.getConfig();
							config.enabled = false;
							config.save();
							ctx.getSource().sendFeedback(Text.literal("[ChatAPI] Desativado."));
							return 1;
						}))
						.then(ClientCommandManager.literal("status").executes(ctx -> {
							ModConfig config = ModChatApiClient.getConfig();
							ctx.getSource().sendFeedback(Text.literal(String.format(
									"[ChatAPI] %s | endpoint: %s | timeout: %dms | ignorar próprias: %s | sistema: %s | prefixo: %s",
									config.enabled ? "ativado" : "desativado",
									config.endpointUrl,
									config.timeoutMs,
									config.ignoreOwnMessages,
									config.listenToSystemMessages,
									config.triggerPrefix.isEmpty() ? "(nenhum)" : config.triggerPrefix)));
							return 1;
						}))
						.then(ClientCommandManager.literal("url")
								.then(ClientCommandManager.argument("endpoint", StringArgumentType.greedyString())
										.executes(ctx -> {
											ModConfig config = ModChatApiClient.getConfig();
											config.endpointUrl = StringArgumentType.getString(ctx, "endpoint");
											config.save();
											ctx.getSource().sendFeedback(
													Text.literal("[ChatAPI] Endpoint: " + config.endpointUrl));
											return 1;
										})))));
	}
}
