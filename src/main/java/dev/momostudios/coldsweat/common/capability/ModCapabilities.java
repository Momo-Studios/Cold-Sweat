package dev.momostudios.coldsweat.common.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ModCapabilities
{
    public static final Capability<ITemperatureCap> PLAYER_TEMPERATURE = CapabilityManager.get(new CapabilityToken<>() {});
}
