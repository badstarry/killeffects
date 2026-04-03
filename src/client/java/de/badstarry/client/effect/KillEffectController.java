package de.badstarry.client.effect;

import de.badstarry.client.config.KilleffConfig;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KillEffectController {
	private static final KillEffectController INSTANCE = new KillEffectController();
	private static final SoundEvent SQUID_KILL_SOUND = de.badstarry.Killeff.KILL_SOUND_EVENT;

	private final Map<UUID, TrackedTarget> trackedTargets = new HashMap<>();
	private final Map<Integer, SquidVisual> squidVisuals = new HashMap<>();
	private int killCount = 0;


	private KillEffectController() {
	}

	public static KillEffectController getInstance() {
		return INSTANCE;
	}

	public void trackTarget(MinecraftClient client, Entity entity) {
		if (client.world == null || client.player == null || !(entity instanceof LivingEntity livingEntity)) {
			return;
		}
		if (entity instanceof SquidEntity && squidVisuals.containsKey(entity.getId())) {
			return;
		}
		if (livingEntity.isDead() || livingEntity.getHealth() <= 0.0F || livingEntity.isRemoved()) {
			return;
		}

		KilleffConfig config = KilleffConfig.getInstance();
		if (!shouldAffectTarget(config, livingEntity)) {
			return;
		}

		trackedTargets.put(entity.getUuid(), new TrackedTarget(
			entity.getUuid(),
			config.isOnlyOwnKills(),
			new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5D, entity.getZ()),
			entity instanceof PlayerEntity,
			client.world.getTime() + 40L
		));
	}

	public void tick(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			trackedTargets.clear();
			squidVisuals.clear();
			return;
		}

		tickTrackedTargets(client);
		tickSquidVisuals(client);
	}

	private void tickTrackedTargets(MinecraftClient client) {
		if (trackedTargets.isEmpty()) {
			return;
		}

		Set<UUID> completed = new HashSet<>();
		for (TrackedTarget trackedTarget : trackedTargets.values()) {
			Entity entity = client.world.getEntity(trackedTarget.uuid());
			if (!(entity instanceof LivingEntity livingEntity)) {
				completed.add(trackedTarget.uuid());
				continue;
			}

			KilleffConfig config = KilleffConfig.getInstance();
			if (!shouldAffectTarget(config, livingEntity)) {
				completed.add(trackedTarget.uuid());
				continue;
			}

			trackedTarget.updateFrom(livingEntity, client.world.getTime() + 40L);

			if (livingEntity.isDead() || livingEntity.getHealth() <= 0.0F || livingEntity.isRemoved()) {
				if (!trackedTarget.requireOwnKill() || client.world.getTime() <= trackedTarget.expireTick()) {
					spawnEffect(client, trackedTarget.lastKnownCenter(), trackedTarget.playerTarget());
				}
				completed.add(trackedTarget.uuid());
			}
		}

		for (UUID uuid : completed) {
			trackedTargets.remove(uuid);
		}
	}

	private void tickSquidVisuals(MinecraftClient client) {
		if (squidVisuals.isEmpty() || client.world == null) {
			return;
		}

		Iterator<Map.Entry<Integer, SquidVisual>> iterator = squidVisuals.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, SquidVisual> entry = iterator.next();
			SquidVisual visual = entry.getValue();
			Entity entity = client.world.getEntityById(entry.getKey());
			if (!(entity instanceof SquidEntity squidEntity)) {
				iterator.remove();
				continue;
			}

			visual.age++;
			double progress = Math.min(1.0D, (double) visual.age / Math.max(1, visual.maxAge));
			squidEntity.setPos(visual.origin.x, visual.origin.y + easeInOut(progress) * 1.6D, visual.origin.z);
			squidEntity.setPitch(-90.0F);
			squidEntity.setYaw(visual.initialYaw);
			squidEntity.bodyYaw = squidEntity.getYaw();
			squidEntity.headYaw = squidEntity.getYaw();

			if (visual.age % 3 == 0) {
				spawnParticle(client, ParticleTypes.FLAME, squidEntity.getX(), squidEntity.getBodyY(0.5D), squidEntity.getZ(), 0.0D, 0.02D, 0.0D);
			}

			if (visual.age >= visual.maxAge) {
				for (int i = 0; i < 10; i++) {
					double offsetX = (client.world.random.nextDouble() - 0.5D) * 0.8D;
					double offsetY = client.world.random.nextDouble() * 0.5D;
					double offsetZ = (client.world.random.nextDouble() - 0.5D) * 0.8D;
					spawnParticle(client, ParticleTypes.FLAME, squidEntity.getX() + offsetX, squidEntity.getY() + offsetY, squidEntity.getZ() + offsetZ, 0.0D, 0.03D, 0.0D);
				}
				squidEntity.discard();
				iterator.remove();
			}
		}
	}

	private void spawnEffect(MinecraftClient client, LivingEntity entity) {
		spawnEffect(client, new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5D, entity.getZ()), entity instanceof PlayerEntity);
	}

	private void spawnEffect(MinecraftClient client, Vec3d center, boolean targetIsPlayer) {
		KilleffConfig config = KilleffConfig.getInstance();
		KillEffectMode mode = config.getEffectMode();
		if (mode == KillEffectMode.OFF || client.world == null || client.player == null) {
			return;
		}

		if (targetIsPlayer && !config.isAffectPlayers()) {
			return;
		}
		if (!targetIsPlayer && !config.isAffectMobs()) {
			return;
		}

		int particles = config.getParticles();

		if (mode == KillEffectMode.SQUID) {
			spawnSquidVisual(client, center, config.getSquidRiseTicks());
		} else {
			for (int i = 0; i < particles; i++) {
				double offsetX = (client.world.random.nextDouble() - 0.5D) * 0.9D;
				double offsetY = (client.world.random.nextDouble() - 0.5D) * 1.0D;
				double offsetZ = (client.world.random.nextDouble() - 0.5D) * 0.9D;
				double velocityX = (client.world.random.nextDouble() - 0.5D) * 0.15D;
				double velocityY = client.world.random.nextDouble() * 0.2D;
				double velocityZ = (client.world.random.nextDouble() - 0.5D) * 0.15D;

				switch (mode) {
					case LIGHTNING -> {
					}
					case THUNDER -> spawnParticle(client, ParticleTypes.ELECTRIC_SPARK, center.x + offsetX, center.y + offsetY, center.z + offsetZ, velocityX, velocityY, velocityZ);
					case FLAME -> spawnParticle(client, ParticleTypes.FLAME, center.x + offsetX, center.y + offsetY, center.z + offsetZ, velocityX, velocityY, velocityZ);
					case SMOKE -> spawnParticle(client, ParticleTypes.LARGE_SMOKE, center.x + offsetX, center.y + offsetY, center.z + offsetZ, velocityX, velocityY, velocityZ);
					case HEART -> spawnParticle(client, ParticleTypes.HEART, center.x + offsetX, center.y + offsetY, center.z + offsetZ, 0.0D, 0.05D, 0.0D);
					case SOUL -> spawnParticle(client, ParticleTypes.SOUL_FIRE_FLAME, center.x + offsetX, center.y + offsetY, center.z + offsetZ, velocityX, velocityY, velocityZ);
					case BLOOD -> spawnParticle(client, new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState()), center.x + offsetX, center.y + offsetY, center.z + offsetZ, velocityX, velocityY, velocityZ);
					case TOTEM -> spawnParticle(client, ParticleTypes.TOTEM_OF_UNDYING, center.x + offsetX * 0.55D, center.y + offsetY * 0.7D, center.z + offsetZ * 0.55D, velocityX * 0.25D, Math.abs(velocityY) * 0.35D, velocityZ * 0.25D);
					case ARCANE -> {
						spawnParticle(client, ParticleTypes.ENCHANT, center.x + offsetX, center.y + 0.35D + offsetY * 0.5D, center.z + offsetZ, -offsetX * 0.08D, 0.03D, -offsetZ * 0.08D);
						if ((i & 1) == 0) {
							spawnParticle(client, ParticleTypes.WITCH, center.x + offsetX * 0.6D, center.y + 0.5D + offsetY * 0.3D, center.z + offsetZ * 0.6D, 0.0D, 0.02D, 0.0D);
						}
					}
					case PORTAL -> {
						spawnParticle(client, ParticleTypes.PORTAL, center.x + offsetX * 0.75D, center.y + offsetY * 0.6D, center.z + offsetZ * 0.75D, -offsetX * 0.15D, 0.04D, -offsetZ * 0.15D);
						if ((i & 1) == 0) {
							spawnParticle(client, ParticleTypes.REVERSE_PORTAL, center.x + offsetX * 0.45D, center.y + 0.25D + offsetY * 0.35D, center.z + offsetZ * 0.45D, 0.0D, 0.03D, 0.0D);
						}
					}
					case FROST -> {
						spawnParticle(client, ParticleTypes.SNOWFLAKE, center.x + offsetX * 0.9D, center.y + 0.2D + Math.abs(offsetY) * 0.6D, center.z + offsetZ * 0.9D, velocityX * 0.15D, 0.02D, velocityZ * 0.15D);
						if ((i & 1) == 0) {
							spawnParticle(client, ParticleTypes.END_ROD, center.x + offsetX * 0.35D, center.y + 0.4D + offsetY * 0.2D, center.z + offsetZ * 0.35D, 0.0D, 0.0D, 0.0D);
						}
					}
					case STAR -> {
						spawnParticle(client, ParticleTypes.END_ROD, center.x + offsetX * 0.55D, center.y + 0.35D + offsetY * 0.5D, center.z + offsetZ * 0.55D, 0.0D, 0.03D, 0.0D);
						if (i < Math.max(4, particles / 3)) {
							spawnParticle(client, ParticleTypes.FIREWORK, center.x + offsetX * 0.25D, center.y + 0.55D + offsetY * 0.25D, center.z + offsetZ * 0.25D, velocityX * 0.08D, 0.05D, velocityZ * 0.08D);
						}
					}
					case SQUID, OFF -> {
					}
				}
			}
		}

		if (mode == KillEffectMode.LIGHTNING) {
			spawnLightningBoltVisual(client, center);
		}

		if (mode == KillEffectMode.THUNDER) {
			BlockPos pos = BlockPos.ofFloored(center.x, center.y, center.z);
			spawnParticle(client, ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
		}

		if (mode == KillEffectMode.TOTEM) {
			spawnRing(client, ParticleTypes.TOTEM_OF_UNDYING, center, 0.65D, Math.max(10, particles), 0.02D);
			spawnRing(client, ParticleTypes.TOTEM_OF_UNDYING, center.add(0.0D, 0.45D, 0.0D), 0.38D, Math.max(8, particles - 2), 0.05D);
			for (int i = 0; i < 6; i++) {
				double angle = (Math.PI * 2.0D * i) / 6.0D;
				spawnParticle(client, ParticleTypes.END_ROD, center.x + Math.cos(angle) * 0.22D, center.y + 0.8D, center.z + Math.sin(angle) * 0.22D, 0.0D, 0.02D, 0.0D);
			}
		}

		if (mode == KillEffectMode.ARCANE) {
			spawnRing(client, ParticleTypes.ENCHANT, center.add(0.0D, 0.2D, 0.0D), 0.9D, Math.max(12, particles), 0.03D);
			spawnRing(client, ParticleTypes.WITCH, center.add(0.0D, 0.7D, 0.0D), 0.45D, Math.max(8, particles / 2), 0.01D);
		}

		if (mode == KillEffectMode.PORTAL) {
			spawnRing(client, ParticleTypes.PORTAL, center.add(0.0D, 0.15D, 0.0D), 0.8D, Math.max(12, particles), 0.06D);
			spawnRing(client, ParticleTypes.REVERSE_PORTAL, center.add(0.0D, 0.6D, 0.0D), 0.42D, Math.max(8, particles / 2), 0.02D);
		}

		if (mode == KillEffectMode.FROST) {
			spawnRing(client, ParticleTypes.SNOWFLAKE, center.add(0.0D, 0.08D, 0.0D), 0.72D, Math.max(12, particles), 0.01D);
			for (int i = 0; i < 7; i++) {
				double angle = (Math.PI * 2.0D * i) / 7.0D;
				spawnParticle(client, ParticleTypes.END_ROD, center.x + Math.cos(angle) * 0.55D, center.y + 0.18D, center.z + Math.sin(angle) * 0.55D, 0.0D, 0.0D, 0.0D);
			}
		}

		if (mode == KillEffectMode.STAR) {
			for (int i = 0; i < 5; i++) {
				double angle = -Math.PI / 2.0D + (Math.PI * 2.0D * i) / 5.0D;
				double outerX = center.x + Math.cos(angle) * 0.75D;
				double outerZ = center.z + Math.sin(angle) * 0.75D;
				spawnParticle(client, ParticleTypes.END_ROD, outerX, center.y + 0.75D, outerZ, 0.0D, 0.02D, 0.0D);
				spawnParticle(client, ParticleTypes.FIREWORK, outerX, center.y + 0.75D, outerZ, 0.0D, 0.04D, 0.0D);
			}
			spawnParticle(client, ParticleTypes.END_ROD, center.x, center.y + 0.9D, center.z, 0.0D, 0.04D, 0.0D);
		}

		if (config.isPlaySound()) {
			playEffectSound(client, center, mode, config.getSoundVolume());
		}

		if (config.isShowKillCounter()) {
			killCount++;
			client.player.sendMessage(Text.literal("Killeff 连杀 +1  当前: " + killCount), true);
		}
	}

	private void spawnSquidVisual(MinecraftClient client, Vec3d center, int riseTicks) {
		if (client.world == null) {
			return;
		}

		SquidEntity squid = new SquidEntity(net.minecraft.entity.EntityType.SQUID, client.world);
		squid.setPosition(center.x, center.y - 0.35D, center.z);
		squid.setYaw(client.world.random.nextFloat() * 360.0F);
		client.world.addEntity(squid);
		squidVisuals.put(squid.getId(), new SquidVisual(center, squid.getYaw(), riseTicks));
	}

	private void playEffectSound(MinecraftClient client, Vec3d position, KillEffectMode mode, float volume) {
		switch (mode) {
			case LIGHTNING -> client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, volume, 1.0F);
			case THUNDER -> {
				client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, volume, 1.0F);
				client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ITEM_TRIDENT_THUNDER.value(), SoundCategory.PLAYERS, Math.max(0.2F, volume - 0.15F), 1.1F);
			}
			case FLAME, SOUL -> client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, volume, 1.0F);
			case SMOKE -> client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, volume, 0.9F);
			case HEART -> client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, volume, 0.8F);
			case BLOOD -> client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, volume, 0.7F);
			case TOTEM -> {
				client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, volume, 1.0F);
				client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, Math.max(0.2F, volume - 0.1F), 1.35F);
			}
			case ARCANE -> client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, volume, 1.05F);
			case PORTAL -> client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, volume, 0.8F);
			case FROST -> client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, volume, 1.55F);
			case STAR -> {
				client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, volume, 1.15F);
				client.world.playSound(client.player, position.x, position.y, position.z, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, Math.max(0.2F, volume - 0.15F), 1.6F);
			}
			case SQUID -> client.player.playSound(SQUID_KILL_SOUND, volume, 1.0F);
			case OFF -> {
			}
		}
	}

	private void spawnLightningBoltVisual(MinecraftClient client, Vec3d center) {
		if (client.world == null) {
			return;
		}

		LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, client.world);
		lightningEntity.setPosition(center.x, center.y, center.z);
		lightningEntity.setCosmetic(true);
		client.world.addEntity(lightningEntity);
	}

	private void spawnRing(MinecraftClient client, net.minecraft.particle.ParticleEffect effect, Vec3d center, double radius, int points, double velocityY) {
		for (int i = 0; i < points; i++) {
			double angle = (Math.PI * 2.0D * i) / (double) points;
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			spawnParticle(client, effect, x, center.y, z, 0.0D, velocityY, 0.0D);
		}
	}

	private void spawnParticle(MinecraftClient client, net.minecraft.particle.ParticleEffect effect, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		if (client.particleManager == null) {
			return;
		}
		client.particleManager.addParticle(effect, x, y, z, velocityX, velocityY, velocityZ);
	}

	private boolean shouldAffectTarget(KilleffConfig config, LivingEntity livingEntity) {
		boolean isPlayer = livingEntity instanceof PlayerEntity;
		if (isPlayer && !config.isAffectPlayers()) {
			return false;
		}
		return isPlayer || config.isAffectMobs();
	}

	private double easeInOut(double x) {
		return x < 0.5D
			? (1.0D - Math.sqrt(1.0D - Math.pow(2.0D * x, 2.0D))) / 2.0D
			: (Math.sqrt(1.0D - Math.pow(-2.0D * x + 2.0D, 2.0D)) + 1.0D) / 2.0D;
	}

	public int getKillCount() {
		return killCount;
	}

	public void resetKillCount() {
		killCount = 0;
		trackedTargets.clear();
		squidVisuals.clear();
	}

	private static final class TrackedTarget {
		private final UUID uuid;
		private final boolean requireOwnKill;
		private Vec3d lastKnownCenter;
		private final boolean playerTarget;
		private long expireTick;

		private TrackedTarget(UUID uuid, boolean requireOwnKill, Vec3d lastKnownCenter, boolean playerTarget, long expireTick) {
			this.uuid = uuid;
			this.requireOwnKill = requireOwnKill;
			this.lastKnownCenter = lastKnownCenter;
			this.playerTarget = playerTarget;
			this.expireTick = expireTick;
		}

		private UUID uuid() {
			return uuid;
		}

		private boolean requireOwnKill() {
			return requireOwnKill;
		}

		private Vec3d lastKnownCenter() {
			return lastKnownCenter;
		}

		private boolean playerTarget() {
			return playerTarget;
		}

		private long expireTick() {
			return expireTick;
		}

		private void updateFrom(LivingEntity entity, long newExpireTick) {
			this.lastKnownCenter = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5D, entity.getZ());
			this.expireTick = newExpireTick;
		}
	}

	private static final class SquidVisual {
		private final Vec3d origin;
		private final float initialYaw;
		private final int maxAge;
		private int age;

		private SquidVisual(Vec3d origin, float initialYaw, int maxAge) {
			this.origin = origin;
			this.initialYaw = initialYaw;
			this.maxAge = maxAge;
		}
	}
}
