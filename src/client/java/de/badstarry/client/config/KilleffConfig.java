package de.badstarry.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.badstarry.Killeff;
import de.badstarry.client.effect.KillEffectMode;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KilleffConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(Killeff.MOD_ID + ".json");
	private static KilleffConfig instance;

	private KillEffectMode effectMode = KillEffectMode.LIGHTNING;
	private boolean playSound = true;
	private boolean showKillCounter = true;
	private boolean onlyOwnKills = true;
	private boolean affectPlayers = true;
	private boolean affectMobs = true;
	private int particles = 12;
	private float soundVolume = 0.8F;
	private int squidRiseTicks = 16;

	public static KilleffConfig getInstance() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static void saveInstance() {
		if (instance != null) {
			instance.save();
		}
	}

	public static void reset() {
		instance = new KilleffConfig();
		instance.save();
	}

	public static KilleffConfig load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
				KilleffConfig loaded = GSON.fromJson(reader, KilleffConfig.class);
				if (loaded != null) {
					loaded.sanitize();
					return loaded;
				}
			} catch (IOException exception) {
				Killeff.LOGGER.warn("Failed to load config from {}", CONFIG_PATH, exception);
			}
		}

		KilleffConfig config = new KilleffConfig();
		config.save();
		return config;
	}

	public void save() {
		sanitize();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			Killeff.LOGGER.warn("Failed to save config to {}", CONFIG_PATH, exception);
		}
	}

	public void sanitize() {
		if (effectMode == null) {
			effectMode = KillEffectMode.LIGHTNING;
		}
		particles = Math.clamp(particles, 4, 40);
		soundVolume = Math.clamp(soundVolume, 0.0F, 1.0F);
		squidRiseTicks = Math.clamp(squidRiseTicks, 8, 40);
		if (!affectPlayers && !affectMobs) {
			affectPlayers = true;
			affectMobs = true;
		}
	}

	public KillEffectMode getEffectMode() {
		return effectMode;
	}

	public void setEffectMode(KillEffectMode effectMode) {
		this.effectMode = effectMode;
	}

	public boolean isPlaySound() {
		return playSound;
	}

	public void setPlaySound(boolean playSound) {
		this.playSound = playSound;
	}

	public boolean isShowKillCounter() {
		return showKillCounter;
	}

	public void setShowKillCounter(boolean showKillCounter) {
		this.showKillCounter = showKillCounter;
	}

	public boolean isOnlyOwnKills() {
		return onlyOwnKills;
	}

	public void setOnlyOwnKills(boolean onlyOwnKills) {
		this.onlyOwnKills = onlyOwnKills;
	}

	public boolean isAffectPlayers() {
		return affectPlayers;
	}

	public void setAffectPlayers(boolean affectPlayers) {
		this.affectPlayers = affectPlayers;
	}

	public boolean isAffectMobs() {
		return affectMobs;
	}

	public void setAffectMobs(boolean affectMobs) {
		this.affectMobs = affectMobs;
	}

	public int getParticles() {
		return particles;
	}

	public void setParticles(int particles) {
		this.particles = particles;
	}

	public float getSoundVolume() {
		return soundVolume;
	}

	public void setSoundVolume(float soundVolume) {
		this.soundVolume = soundVolume;
	}

	public int getSquidRiseTicks() {
		return squidRiseTicks;
	}

	public void setSquidRiseTicks(int squidRiseTicks) {
		this.squidRiseTicks = squidRiseTicks;
	}
}
