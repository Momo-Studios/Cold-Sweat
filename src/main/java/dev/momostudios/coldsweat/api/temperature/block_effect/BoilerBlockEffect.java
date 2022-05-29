package dev.momostudios.coldsweat.api.temperature.block_effect;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.registries.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import dev.momostudios.coldsweat.common.block.BoilerBlock;
import dev.momostudios.coldsweat.util.math.CSMath;

public class BoilerBlockEffect extends BlockEffect
{
    public BoilerBlockEffect()
    {
        super(ModBlocks.BOILER);
    }

    @Override
    public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
    {
        if (this.hasBlock(state.getBlock()) && state.get(BoilerBlock.LIT))
        {
            return CSMath.blend(0.27, 0, distance, 0.5, 7);
        }
        return 0;
    }

    @Override
    public double maxEffect() {
        return CSMath.convertUnits(40, Temperature.Units.F, Temperature.Units.MC, false);
    }

    @Override
    public double maxTemperature() {
        return CSMath.convertUnits(212, Temperature.Units.F, Temperature.Units.MC, true);
    }
}
