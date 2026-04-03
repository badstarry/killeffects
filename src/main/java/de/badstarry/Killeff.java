package de.badstarry;

import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Killeff implements ModInitializer {
	public static final String MOD_ID = "killeffects";
	public static final Identifier KILL_SOUND_ID = Identifier.of(MOD_ID, "kill");
	public static final SoundEvent KILL_SOUND_EVENT = SoundEvent.of(KILL_SOUND_ID);

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Registry.register(Registries.SOUND_EVENT, KILL_SOUND_ID, KILL_SOUND_EVENT);
		LOGGER.info("Hello Fabric world!");
	}
}
