package br.com.arcasoltec.modchatapi;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

/**
 * Comando client-side /minecraftia para controlar o mod sem editar o arquivo de config:
 *   /minecraftia on | off | status | url <endpoint> | whitelist [<nickname>]
 */
public final class ChatApiCommands {
	private ChatApiCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("minecraftia")
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
									"[ChatAPI] %s | endpoint: %s | timeout: %dms | ignorar próprias: %s | sistema: %s | prefixo: %s | whitelist: %s",
									config.enabled ? "ativado" : "desativado",
									config.endpointUrl,
									config.timeoutMs,
									config.ignoreOwnMessages,
									config.listenToSystemMessages,
									config.triggerPrefix.isEmpty() ? "(nenhum)" : config.triggerPrefix,
									config.whitelist.isEmpty() ? "(todos)" : String.join(", ", config.whitelist))));
							return 1;
						}))
						.then(ClientCommandManager.literal("whitelist")
								.executes(ctx -> {
									ModConfig config = ModChatApiClient.getConfig();
									ctx.getSource().sendFeedback(Text.literal(config.whitelist.isEmpty()
											? "[ChatAPI] Whitelist vazia — respondendo a todos os jogadores."
											: "[ChatAPI] Whitelist: " + String.join(", ", config.whitelist)));
									return 1;
								})
								.then(ClientCommandManager.argument("nickname", StringArgumentType.word())
										.executes(ctx -> {
											ModConfig config = ModChatApiClient.getConfig();
											String nick = StringArgumentType.getString(ctx, "nickname");
											boolean removed = config.whitelist.removeIf(n -> n.equalsIgnoreCase(nick));
											if (!removed) {
												config.whitelist.add(nick);
											}
											config.save();
											ctx.getSource().sendFeedback(Text.literal(removed
													? "[ChatAPI] " + nick + " removido da whitelist."
													: "[ChatAPI] " + nick + " adicionado à whitelist."));
											return 1;
										})))
						.then(ClientCommandManager.literal("url")
								.then(ClientCommandManager.argument("endpoint", StringArgumentType.greedyString())
										.executes(ctx -> {
											ModConfig config = ModChatApiClient.getConfig();
											config.endpointUrl = StringArgumentType.getString(ctx, "endpoint");
											config.save();
											ctx.getSource().sendFeedback(
													Text.literal("[ChatAPI] Endpoint: " + config.endpointUrl));
											return 1;
										})))
						.then(ClientCommandManager.literal("autoeat")
								.then(ClientCommandManager.literal("on").executes(ctx -> {
									ModConfig config = ModChatApiClient.getConfig();
									config.autoEat = true;
									config.save();
									ctx.getSource().sendFeedback(Text.literal("[MinecraftIA] AutoEat ativado."));
									return 1;
								}))
								.then(ClientCommandManager.literal("off").executes(ctx -> {
									ModConfig config = ModChatApiClient.getConfig();
									config.autoEat = false;
									config.save();
									ctx.getSource().sendFeedback(Text.literal("[MinecraftIA] AutoEat desativado."));
									return 1;
								})))));
	}
}
