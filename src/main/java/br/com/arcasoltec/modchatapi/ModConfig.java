package br.com.arcasoltec.modchatapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuração persistida em config/modchatapi.json.
 */
public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("modchatapi.json");

	/** Liga/desliga o encaminhamento de mensagens. */
	public boolean enabled = true;

	/** URL do web app que recebe as mensagens via POST. */
	public String endpointUrl = "http://localhost:8080/chat";

	/** Tempo máximo (ms) de espera pela resposta do web app. */
	public int timeoutMs = 10000;

	/** Ignora mensagens enviadas pelo próprio jogador (evita loop infinito). */
	public boolean ignoreOwnMessages = true;

	/** Também encaminha mensagens de sistema (feedback de comandos, joins, etc.). */
	public boolean listenToSystemMessages = false;

	/** Se não vazio, só encaminha mensagens que começam com este prefixo (ex.: "!"). */
	public String triggerPrefix = "";

	/** Se não vazia, só encaminha mensagens de jogadores desta lista (/chatapi whitelist <nick>). */
	public List<String> whitelist = new ArrayList<>();

	/** Liga/desliga o autoeat (come automaticamente quando fome ≤ 4). */
	public boolean autoEat = false;

	public static ModConfig load() {
		if (Files.exists(PATH)) {
			try {
				ModConfig config = GSON.fromJson(Files.readString(PATH), ModConfig.class);
				if (config != null) {
					// Configs salvas por versões antigas não têm o campo
					if (config.whitelist == null) {
						config.whitelist = new ArrayList<>();
					}
					return config;
				}
			} catch (Exception e) {
				ModChatApiClient.LOGGER.error("Falha ao ler {}, usando configuração padrão", PATH, e);
			}
		}
		ModConfig config = new ModConfig();
		config.save();
		return config;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			ModChatApiClient.LOGGER.error("Falha ao salvar {}", PATH, e);
		}
	}
}
