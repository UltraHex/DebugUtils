package net.ultrahex.mc.debugutils;

import net.ultrahex.mc.debugutils.command.RecalcLightingCommand;

import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
        event.registerServerCommand(new RecalcLightingCommand());
    }
}
