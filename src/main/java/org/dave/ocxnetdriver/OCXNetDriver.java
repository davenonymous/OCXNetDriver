package org.dave.ocxnetdriver;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.dave.ocxnetdriver.converter.ConverterBlockPos;
import org.dave.ocxnetdriver.driver.controller.DriverXnetController;

@Mod(modid = OCXNetDriver.MODID, version = OCXNetDriver.VERSION, dependencies = "required-after:opencomputers;required-after:xnet", acceptedMinecraftVersions = "[1.12,1.13)")
public class OCXNetDriver {
    public static final String MODID = "ocxnetdriver";
    public static final String VERSION = "1.0";

    @Mod.Instance
    public static OCXNetDriver instance;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        li.cil.oc.api.Driver.add(new DriverXnetController());
        li.cil.oc.api.Driver.add(new ConverterBlockPos());
    }
}
