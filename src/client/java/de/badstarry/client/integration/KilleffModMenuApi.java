package de.badstarry.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.badstarry.client.gui.KilleffConfigScreen;

public final class KilleffModMenuApi implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return KilleffConfigScreen::create;
	}
}
