package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.util.TempHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

public class WaterTempModifier extends TempModifier
{
    public WaterTempModifier()
    {
        addArgument("strength", 0.01);
    }

    public WaterTempModifier(double strength)
    {
        addArgument("strength", strength);
    }

    @Override
    public Function<Temperature, Temperature> calculate(Player player)
    {
        double maxTemp = ConfigCache.getInstance().maxTemp;
        double minTemp = ConfigCache.getInstance().minTemp;

        double strength = this.<Double>getArgument("strength");
        double worldTemp = TempHelper.getTemperature(player, Temperature.Type.WORLD).get();
        double returnRate = Math.min(-0.001, -0.001 - (worldTemp / 800));
        double addAmount = player.isInWaterOrBubble() ? 0.01 : player.level.isRainingAt(player.blockPosition()) ? 0.005 : returnRate;

        setArgument("strength", CSMath.clamp(strength + addAmount, 0d, Math.abs(CSMath.average(maxTemp, minTemp) - worldTemp) / 2));

        // If the strength is 0, this TempModifier expires~
        if (strength <= 0.0)
        {
            this.expires(this.getTicksExisted() - 1);
        }

        if (!player.isInWater())
        {
            if (Math.random() < strength)
            {
                double randX = player.getBbWidth() * (Math.random() - 0.5);
                double randY = player.getBbHeight() * Math.random();
                double randZ = player.getBbWidth() * (Math.random() - 0.5);
                player.level.addParticle(ParticleTypes.FALLING_WATER, player.getX() + randX, player.getY() + randY, player.getZ() + randZ, 0, 0, 0);
            }
        }

        return temp -> temp.add(-this.<Double>getArgument("strength"));
    }

    @Override
    public String getID()
    {
        return "cold_sweat:water";
    }
}
