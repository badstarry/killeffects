package de.badstarry.client.gui;

import de.badstarry.client.KilleffClient;
import de.badstarry.client.config.KilleffConfig;
import de.badstarry.client.effect.KillEffectController;
import de.badstarry.client.effect.KillEffectMode;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class KilleffConfigScreen {
	private KilleffConfigScreen() {
	}

	public static Screen create(Screen parent) {
		KilleffConfig config = KilleffConfig.getInstance();
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(KilleffClient.getConfigScreenTitle())
			.setSavingRunnable(KilleffConfig::saveInstance);

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(Text.literal("通用"));
		ConfigCategory target = builder.getOrCreateCategory(Text.literal("目标过滤"));
		ConfigCategory effect = builder.getOrCreateCategory(Text.literal("特效参数"));

		general.addEntry(entryBuilder.startEnumSelector(Text.literal("击杀特效模式"), KillEffectMode.class, config.getEffectMode())
			.setDefaultValue(KillEffectMode.LIGHTNING)
			.setEnumNameProvider(mode -> Text.literal(((KillEffectMode) mode).getDisplayName()))
			.setSaveConsumer(config::setEffectMode)
			.build());
		general.addEntry(entryBuilder.startBooleanToggle(Text.literal("播放音效"), config.isPlaySound())
			.setDefaultValue(true)
			.setSaveConsumer(config::setPlaySound)
			.build());
		general.addEntry(entryBuilder.startBooleanToggle(Text.literal("显示连杀计数"), config.isShowKillCounter())
			.setDefaultValue(true)
			.setSaveConsumer(value -> {
				config.setShowKillCounter(value);
				if (!value) {
					KillEffectController.getInstance().resetKillCount();
				}
			})
			.build());
		general.addEntry(entryBuilder.startBooleanToggle(Text.literal("仅自己造成的击杀才触发"), config.isOnlyOwnKills())
			.setDefaultValue(true)
			.setTooltip(Text.literal("开启后，仅当最后一次攻击来自本地玩家时才会播放击杀特效。"))
			.setSaveConsumer(config::setOnlyOwnKills)
			.build());

		target.addEntry(entryBuilder.startBooleanToggle(Text.literal("对玩家生效"), config.isAffectPlayers())
			.setDefaultValue(true)
			.setSaveConsumer(config::setAffectPlayers)
			.build());
		target.addEntry(entryBuilder.startBooleanToggle(Text.literal("对普通生物生效"), config.isAffectMobs())
			.setDefaultValue(true)
			.setSaveConsumer(config::setAffectMobs)
			.build());

		effect.addEntry(entryBuilder.startIntSlider(Text.literal("粒子数量"), config.getParticles(), 4, 40)
			.setDefaultValue(12)
			.setSaveConsumer(config::setParticles)
			.build());
		effect.addEntry(entryBuilder.startFloatField(Text.literal("音效音量"), config.getSoundVolume())
			.setDefaultValue(0.8F)
			.setMin(0.0F)
			.setMax(1.0F)
			.setSaveConsumer(config::setSoundVolume)
			.build());
		effect.addEntry(entryBuilder.startIntSlider(Text.literal("鱿鱼上升时长（tick）"), config.getSquidRiseTicks(), 8, 40)
			.setDefaultValue(16)
			.setTooltip(Text.literal("仅在鱿鱼升天模式下生效。值越大，鱿鱼升空越慢。"))
			.setSaveConsumer(config::setSquidRiseTicks)
			.build());

		return builder.build();
	}
}
