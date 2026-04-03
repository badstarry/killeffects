package de.badstarry.client.effect;

public enum KillEffectMode {
	LIGHTNING("纯闪电"),
	THUNDER("雷暴"),
	FLAME("火焰"),
	SMOKE("烟雾"),
	HEART("爱心"),
	SOUL("灵魂"),
	BLOOD("血色碎片"),
	TOTEM("图腾") ,
	ARCANE("附魔环") ,
	PORTAL("虚空传送") ,
	FROST("寒霜") ,
	STAR("星辉") ,
	SQUID("鱿鱼升天"),
	OFF("关闭");

	private final String displayName;

	KillEffectMode(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
