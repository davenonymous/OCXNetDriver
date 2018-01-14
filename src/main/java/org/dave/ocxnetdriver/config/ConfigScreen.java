package org.dave.ocxnetdriver.config;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiConfig;
import org.dave.ocxnetdriver.OCXNetDriver;

public class ConfigScreen extends GuiConfig {
    public ConfigScreen(GuiScreen parentScreen) {
        super(parentScreen, ConfigurationHandler.getConfigElements(), OCXNetDriver.MODID, false, false, "OC XNet Driver");
    }
}
