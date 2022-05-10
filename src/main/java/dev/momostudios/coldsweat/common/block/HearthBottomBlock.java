package dev.momostudios.coldsweat.common.block;

import dev.momostudios.coldsweat.common.te.HearthTileEntity;
import dev.momostudios.coldsweat.core.init.BlockInit;
import dev.momostudios.coldsweat.core.init.TileEntityInit;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;

public class HearthBottomBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 2);
    public static final IntegerProperty LAVA = IntegerProperty.create("lava", 0, 2);

    private static final Map<Direction, VoxelShape> SHAPES = new HashMap<Direction, VoxelShape>();

    public static Properties getProperties()
    {
        return Properties
                .create(Material.ROCK)
                .sound(SoundType.STONE)
                .hardnessAndResistance(2f, 10f)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(1)
                .notSolid()
                .setLightLevel(s -> 0)
                .setOpaque((bs, br, bp) -> false);
    }

    public static Item.Properties getItemProperties()
    {
        return new Item.Properties().group(ColdSweatGroup.COLD_SWEAT).maxStackSize(1);
    }

    public HearthBottomBlock(Properties properties)
    {
        super(properties);
        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(WATER, 0).with(LAVA, 0));
        runCalculation(VoxelShapes.or(
            makeCuboidShape(3, 0, 3.5, 13, 18, 12.5), // Shell
            makeCuboidShape(4, 18, 5, 9, 27, 10), // Exhaust
            makeCuboidShape(-1, 3, 6, 17, 11, 10))); // Canisters
    }

    static void calculateShapes(Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[] { shape, VoxelShapes.empty() };

        int times = (to.getHorizontalIndex() - Direction.NORTH.getHorizontalIndex() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.or(buffer[1],
                VoxelShapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }

        SHAPES.put(to, buffer[0]);
    }

    static void runCalculation(VoxelShape shape) {
        for (Direction direction : Direction.values()) {
            calculateShapes(direction, shape);
        }
    }

    @Override
    public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos)
    {
        return worldIn.isAirBlock(pos) && worldIn.isAirBlock(pos.up());
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
    {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        if (!worldIn.isRemote)
        {
            if (worldIn.getTileEntity(pos) instanceof HearthTileEntity)
            {
                HearthTileEntity te = (HearthTileEntity) worldIn.getTileEntity(pos);
                ItemStack stack = player.getHeldItem(hand);
                int itemFuel = te.getItemFuel(stack);
                int hearthFuel = itemFuel > 0 ? te.getHotFuel() : te.getColdFuel();

                if (itemFuel != 0 && hearthFuel + Math.abs(itemFuel) * 0.75 < HearthTileEntity.MAX_FUEL)
                {
                    if (!player.isCreative())
                    {
                        if (stack.hasContainerItem())
                        {
                            ItemStack container = stack.getContainerItem();
                            stack.shrink(1);
                            player.inventory.addItemStackToInventory(container);
                        }
                        else
                        {
                            stack.shrink(1);
                        }
                    }
                    te.addFuel(itemFuel);
                    te.updateFuelState();

                    worldIn.playSound(null, pos, itemFuel > 0 ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY,
                            SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);
                }
                else
                {
                    NetworkHooks.openGui((ServerPlayerEntity) player, te, pos);
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
    {
        if (worldIn.isAirBlock(pos.up()))
        {
            worldIn.setBlockState(pos.up(), BlockInit.HEARTH_TOP.get().getDefaultState().with(HearthTopBlock.FACING, state.get(FACING)), 2);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        if (worldIn.getBlockState(pos.up()).getBlock() != BlockInit.HEARTH_TOP.get())
        {
            worldIn.destroyBlock(pos, false);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public INamedContainerProvider getContainer(BlockState state, World worldIn, BlockPos pos) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        return tileEntity instanceof INamedContainerProvider ? (INamedContainerProvider) tileEntity : null;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return TileEntityInit.HEARTH_TILE_ENTITY_TYPE.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        List<ItemStack> dropsOriginal = super.getDrops(state, builder);
        if (!dropsOriginal.isEmpty())
            return dropsOriginal;
        return Collections.singletonList(new ItemStack(this, 1));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.matchesBlock(newState.getBlock()))
        {
            if (world.getBlockState(pos.up()).getBlock() == BlockInit.HEARTH_TOP.get())
            {
                world.destroyBlock(pos.up(), false);
            }

            TileEntity tileentity = world.getTileEntity(pos);
            if (tileentity instanceof HearthTileEntity) {
                InventoryHelper.dropInventoryItems(world, pos, (HearthTileEntity) tileentity);
                world.updateComparatorOutputLevel(pos, this);
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction)
    {
        return state.with(FACING, direction.rotate(state.get(FACING)));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATER, LAVA);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite()).with(WATER, 0).with(LAVA, 0);
    }
}
