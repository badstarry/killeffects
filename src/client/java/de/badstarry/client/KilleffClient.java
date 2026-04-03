package de.badstarry.client;

import de.badstarry.Killeff;
import de.badstarry.client.config.KilleffConfig;
import de.badstarry.client.effect.KillEffectController;
import de.badstarry.client.gui.KilleffConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class KilleffClient implements ClientModInitializer {
	private static KeyBinding openConfigKeyBinding;

	@Override
	public void onInitializeClient() {
		KilleffConfig.load();

		openConfigKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.killeffects.open_config",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			KeyBinding.Category.create(Identifier.of(Killeff.MOD_ID, "general"))
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> KilleffConfig.saveInstance());
		AttackEntityCallback.EVENT.register(this::onAttackEntity);

		Killeff.LOGGER.info("Killeffects client initialized");
	}

	private void onClientTick(MinecraftClient client) {
		KillEffectController.getInstance().tick(client);

		while (openConfigKeyBinding.wasPressed()) {
			client.setScreen(KilleffConfigScreen.create(client.currentScreen));
		}
	}

	private ActionResult onAttackEntity(net.minecraft.entity.player.PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || player != client.player || !world.isClient()) {
			return ActionResult.PASS;
		}

		KillEffectController.getInstance().trackTarget(client, entity);
		return ActionResult.PASS;
	}

	public static Text getConfigScreenTitle() {
		return Text.literal("Killeffects 配置");
	}
}
