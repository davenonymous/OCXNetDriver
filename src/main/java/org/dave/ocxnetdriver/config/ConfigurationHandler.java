package org.dave.ocxnetdriver.config;

import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.dave.ocxnetdriver.OCXNetDriver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationHandler {
    public static Configuration configuration;

    public static void init(File configFile) {
        if (configuration != null) {
            return;
        }

        configuration = new Configuration(configFile, null);
        loadConfiguration();
    }

    @SubscribeEvent
    public void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equalsIgnoreCase(OCXNetDriver.MODID)) {
            loadConfiguration();
        }
    }

    private static void loadConfiguration() {
        Settings.ignoreEnergyTransferLimits = configuration.getBoolean(
                "ignoreEnergyTransferLimits",
                "General",
                true,
                "Ignore the insertion/extraction limits when transferring energy"
        );

        Settings.maxEnergyTransferTicksPerCall = configuration.getInt(
                "maxEnergyTransferTicksPerCall",
                "General",
                Integer.MAX_VALUE,
                1,
                Integer.MAX_VALUE,
                "How many energy transfer ticks to perform during a single call"
        );

        if(configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static List<IConfigElement> getConfigElements() {
        List<IConfigElement> result = new ArrayList<>();
        result.add(new ConfigElement(configuration.getCategory("General")));
        return result;
    }

    public static class Settings {
        private static boolean ignoreEnergyTransferLimits;
        private static int maxEnergyTransferTicksPerCall;

        public static boolean ignoreEnergyTransferLimits() {
            return ignoreEnergyTransferLimits;
        }

        public static int getMaxEnergyTransferTicksPerCall() {
            return maxEnergyTransferTicksPerCall;
        }
    }
}
