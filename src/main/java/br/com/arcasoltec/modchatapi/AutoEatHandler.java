package br.com.arcasoltec.modchatapi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class AutoEatHandler {

	private enum State { IDLE, EATING }

	private State state = State.IDLE;
	private int ticksEating = 0;
	private int savedSlot = 0;
	private boolean eatStarted = false;

	public void tick(MinecraftClient client) {
		if (client.player == null || client.getNetworkHandler() == null || client.interactionManager == null) {
			state = State.IDLE;
			return;
		}

		ModConfig config = ModChatApiClient.getConfig();

		if (!config.autoEat && state == State.EATING) {
			client.player.getInventory().setSelectedSlot(savedSlot);
			client.getNetworkHandler().sendChatMessage("#resume");
			state = State.IDLE;
			return;
		}

		if (!config.autoEat) return;

		int hunger = client.player.getHungerManager().getFoodLevel();

		switch (state) {
			case IDLE -> {
				if (hunger <= 4) {
					int foodSlot = findFoodSlot(client);
					if (foodSlot != -1) {
						savedSlot = client.player.getInventory().getSelectedSlot();
						client.player.getInventory().setSelectedSlot(foodSlot);
						client.getNetworkHandler().sendChatMessage("#pause");
						ticksEating = 0;
						eatStarted = false;
						state = State.EATING;
					}
				}
			}
			case EATING -> {
				// Fecha qualquer tela aberta (inventário, ESC, etc.) para que o jogo processe o uso do item
				if (client.currentScreen != null) {
					client.setScreen(null);
				}

				// Simula segurar clique direito — igual ao que o vanilla faz a cada tick
				client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
				ticksEating++;

				// getActiveItem() reflete o estado local (activeItemStack),
				// mais confiável que isUsingItem() que é sincronizado pelo servidor
				if (!client.player.getActiveItem().isEmpty()) {
					eatStarted = true;
				}

				boolean eatDone = eatStarted && client.player.getActiveItem().isEmpty();
				boolean eatTimedOut = !eatStarted && ticksEating > 15;

				if (eatDone || eatTimedOut) {
					// Se ainda com fome, tenta comer mais sem enviar #pause/#resume extra
					if (!eatTimedOut && client.player.getHungerManager().getFoodLevel() <= 4) {
						int nextSlot = findFoodSlot(client);
						if (nextSlot != -1) {
							client.player.getInventory().setSelectedSlot(nextSlot);
							ticksEating = 0;
							eatStarted = false;
							return;
						}
					}

					client.player.getInventory().setSelectedSlot(savedSlot);
					client.getNetworkHandler().sendChatMessage("#resume");
					state = State.IDLE;
				}
			}
		}
	}

	private int findFoodSlot(MinecraftClient client) {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = client.player.getInventory().getStack(i);
			if (!stack.isEmpty() && stack.getComponents().contains(DataComponentTypes.FOOD)) {
				return i;
			}
		}
		return -1;
	}
}
