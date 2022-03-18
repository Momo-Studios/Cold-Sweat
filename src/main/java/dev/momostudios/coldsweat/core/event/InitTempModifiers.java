package dev.momostudios.coldsweat.core.event;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.event.core.BlockEffectRegisterEvent;
import dev.momostudios.coldsweat.api.event.core.TempModifierRegisterEvent;
import dev.momostudios.coldsweat.api.temperature.modifier.*;
import dev.momostudios.coldsweat.api.temperature.block_effect.*;
import dev.momostudios.coldsweat.api.registry.BlockEffectRegistry;
import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.util.math.CSMath;

import java.util.List;

@Mod.EventBusSubscriber
public class InitTempModifiers
{
    // Trigger TempModifierEvent.Init
    @SubscribeEvent
    public static void registerTempModifiers(WorldEvent.Load event)
    {
        TempModifierRegistry.flush();
        BlockEffectRegistry.flush();

        try { MinecraftForge.EVENT_BUS.post(new TempModifierRegisterEvent()); }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Registering TempModifiers failed!");
            e.printStackTrace();
        }

        try { MinecraftForge.EVENT_BUS.post(new BlockEffectRegisterEvent()); }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Registering BlockEffects failed!");
            e.printStackTrace();
        }
    }

    // Register BlockEffects
    @SubscribeEvent
    public static void registerBlockEffects(BlockEffectRegisterEvent event)
    {
        event.register(new LavaBlockEffect());
        event.register(new FurnaceBlockEffect());
        event.register(new CampfireBlockEffect());
        event.register(new FireBlockEffect());
        event.register(new IceBlockEffect());
        event.register(new IceboxBlockEffect());
        event.register(new BoilerBlockEffect());
        event.register(new SoulFireBlockEffect());
        event.register(new SoulCampfireBlockEffect());
        event.register(new NetherPortalBlockEffect());
        event.register(new MagmaBlockEffect());

        // Auto-generate BlockEffects from config
        for (List<Object> effectBuilder : ColdSweatConfig.getInstance().getBlockEffects())
        {
            try
            {
                // Check if required fields are present
                if (!(effectBuilder.get(0) instanceof String)
                ||  !(effectBuilder.get(1) instanceof Number)
                ||  !(effectBuilder.get(2) instanceof Number))
                {
                    throw new Exception("Invalid BlockEffect format");
                }

                String[] blockIDs = ((String) effectBuilder.get(0)).split(",");

                double temp;
                try { temp = (double) effectBuilder.get(1); } catch (Exception e) { temp = (int) effectBuilder.get(1); }

                double range;
                try { range = (double) effectBuilder.get(2); } catch (Exception e) { range = (int) effectBuilder.get(2); }

                boolean weaken;
                try { weaken = (boolean) effectBuilder.get(3); } catch (Exception e) { weaken = true; }

                double maxTemp;
                try { maxTemp = (double) effectBuilder.get(4); } catch (Exception e) { maxTemp = Double.MAX_VALUE; }

                double minTemp;
                try { minTemp = (double) effectBuilder.get(5); } catch (Exception e) { minTemp = -Double.MAX_VALUE; }

                final double finalTemp = temp;
                final double finalRange = range;
                final boolean finalWeaken = weaken;
                final double finalMaxTemp = maxTemp;
                final double finalMinTemp = minTemp;

                event.register(
                        new BlockEffect()
                        {
                            @Override
                            public double getTemperature(Player player, BlockState state, BlockPos pos, double distance)
                            {
                                return finalWeaken ? CSMath.blend(finalTemp, 0, distance, 0.5, finalRange) : finalTemp;
                            }

                            @Override
                            public boolean hasBlock(BlockState block)
                            {
                                for (String id : blockIDs)
                                {
                                    if (block.getBlock().getRegistryName().toString().equals(id))
                                        return true;
                                }
                                return false;
                            }

                            @Override
                            public double maxEffect()
                            {
                                return finalMaxTemp;
                            }

                            @Override
                            public double minEffect()
                            {
                                return finalMinTemp;
                            }
                        });
            }
            catch (Exception e)
            {
                ColdSweat.LOGGER.warn("Invalid configuration for BlockEffects in config file \"main.toml\"");
                e.printStackTrace();
                break;
            }
        }
    }

    // Register TempModifiers
    @SubscribeEvent
    public static void registerTempModifiers(TempModifierRegisterEvent event)
    {
        String sereneseasons = "dev.momostudios.coldsweat.api.temperature.modifier.compat.SereneSeasonsTempModifier";
        String betterweather = "dev.momostudios.coldsweat.api.temperature.modifier.compat.BetterWeatherTempModifier";

        event.register(new BlockTempModifier());
        event.register(new BiomeTempModifier());
        event.register(new DepthTempModifier());
        event.register(new InsulationTempModifier());
        event.register(new MountTempModifier());
        event.register(new TimeTempModifier());
        event.register(new WaterskinTempModifier());
        event.register(new HellLampTempModifier());
        event.register(new WaterTempModifier());
        event.register(new HearthTempModifier());
        event.register(new FoodTempModifier());

        if (ModList.get().isLoaded("sereneseasons"))
        {
            try { event.register((TempModifier) Class.forName(sereneseasons).getConstructor().newInstance()); }
            catch (Exception e) {}
        }
        //if (ModList.get().isLoaded("betterweather")) event.addModifier((TempModifier) Class.forName(betterweather).newInstance());

        if (ModList.get().isLoaded("sereneseasons") && ModList.get().isLoaded("betterweather"))
        {
            ColdSweat.LOGGER.warn("Multiple seasons mods are present! This may cause issues!");
        }
    }
}
