package gay.avturtle.blocks;

import gay.avturtle.NewBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChickenNest extends Block  {

    public enum EggSlot implements StringRepresentable {
        EMPTY("empty"),
        FILLED("filled"),
        FERTILIZED("fertilized");
        private final String name;

        EggSlot(String name) {
            this.name = name;
        }
        public String toString() {
            return this.name;
        }
        @Override
        public String getSerializedName() {
            return this.toString();
        }
    }
    static final EnumProperty<EggSlot> NORTH_EAST = EnumProperty.create("north_east", EggSlot.class);
    static final EnumProperty<EggSlot> NORTH_WEST = EnumProperty.create("north_west", EggSlot.class);
    static final EnumProperty<EggSlot> SOUTH_EAST = EnumProperty.create("south_east", EggSlot.class);
    static final EnumProperty<EggSlot> SOUTH_WEST = EnumProperty.create("south_west", EggSlot.class);

    public static boolean hasFreeSlot(BlockState state) {
        return state.getValue(NORTH_EAST) == EggSlot.EMPTY ||
                state.getValue(NORTH_WEST) == EggSlot.EMPTY ||
                state.getValue(SOUTH_EAST) == EggSlot.EMPTY ||
                state.getValue(SOUTH_WEST) == EggSlot.EMPTY;
    }

    public static BlockState fillFirstFreeSlot(BlockState state, boolean fertile) {
        EggSlot new_slot_state = fertile ? EggSlot.FERTILIZED : EggSlot.FILLED;
        if (state.getValue(NORTH_EAST) == EggSlot.EMPTY) return state.setValue(NORTH_EAST, new_slot_state);
        if (state.getValue(NORTH_WEST) == EggSlot.EMPTY) return state.setValue(NORTH_WEST, new_slot_state);
        if (state.getValue(SOUTH_EAST) == EggSlot.EMPTY) return state.setValue(SOUTH_EAST, new_slot_state);
        if (state.getValue(SOUTH_WEST) == EggSlot.EMPTY) return state.setValue(SOUTH_WEST, new_slot_state);
        NewBloom.LOGGER.warn("Attempted to fill a full nest!");
        return state;
    }

    public ChickenNest(Properties properties) {
        super(properties);
        registerDefaultState(this.getStateDefinition().any().setValue(NORTH_EAST, EggSlot.EMPTY).setValue(NORTH_WEST, EggSlot.EMPTY).setValue(SOUTH_EAST, EggSlot.EMPTY).setValue(SOUTH_WEST, EggSlot.EMPTY));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if(level.random.nextInt(1) != 0) {
            return;
        }

        boolean hatching = false;
        if(state.getValue(NORTH_EAST) == EggSlot.FERTILIZED) {
            hatching = true;
            level.setBlockAndUpdate(pos, state.setValue(NORTH_EAST, EggSlot.EMPTY));
        }
        if(state.getValue(NORTH_WEST) == EggSlot.FERTILIZED && !hatching) {
            hatching = true;
            level.setBlockAndUpdate(pos, state.setValue(NORTH_WEST, EggSlot.EMPTY));
        }
        if(state.getValue(SOUTH_EAST) == EggSlot.FERTILIZED && !hatching) {
            hatching = true;
            level.setBlockAndUpdate(pos, state.setValue(SOUTH_EAST, EggSlot.EMPTY));
        }
        if(state.getValue(SOUTH_WEST) == EggSlot.FERTILIZED && !hatching) {
            hatching = true;
            level.setBlockAndUpdate(pos, state.setValue(SOUTH_WEST, EggSlot.EMPTY));
        }

        if(hatching) {
            level.playSound((Entity) null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F,  0.9F + random.nextFloat() * 0.2F);
            Chicken chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.BREEDING);
            if(chicken != null) {
                chicken.setBaby(true);
                chicken.moveTo((double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.3, 0.0F, 0.0F);
                level.addFreshEntity(chicken);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH_EAST);
        builder.add(NORTH_WEST);
        builder.add(SOUTH_EAST);
        builder.add(SOUTH_WEST);
    }

    private enum EggRegion {
        NORTH_EAST,
        NORTH_WEST,
        SOUTH_EAST,
        SOUTH_WEST,
    }
    private EggRegion getRegion(BlockHitResult hitResult) {
        BlockPos blockpos = hitResult.getBlockPos().relative(Direction.UP);
        Vec3 local_coord = hitResult.getLocation().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
        if(local_coord.z < 0.5) { // northern region
            if(local_coord.x > 0.5) return EggRegion.NORTH_EAST;
            else return EggRegion.NORTH_WEST;
        } else { // southern region
            if(local_coord.x > 0.5) return EggRegion.SOUTH_EAST;
            else return EggRegion.SOUTH_WEST;
        }
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        EggRegion region = getRegion(hitResult);
        if(region == EggRegion.NORTH_EAST) {
            if(state.getValue(NORTH_EAST) == EggSlot.FILLED || state.getValue(NORTH_EAST) == EggSlot.FERTILIZED) {
                level.setBlockAndUpdate(pos, state.setValue(NORTH_EAST, EggSlot.EMPTY));
                player.addItem(new ItemStack(Items.EGG, 1));
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        if(region == EggRegion.NORTH_WEST) {
            if(state.getValue(NORTH_WEST) == EggSlot.FILLED || state.getValue(NORTH_WEST) == EggSlot.FERTILIZED) {
                level.setBlockAndUpdate(pos, state.setValue(NORTH_WEST, EggSlot.EMPTY));
                player.addItem(new ItemStack(Items.EGG, 1));
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        if(region == EggRegion.SOUTH_EAST) {
            if(state.getValue(SOUTH_EAST) == EggSlot.FILLED || state.getValue(SOUTH_EAST) == EggSlot.FERTILIZED) {
                level.setBlockAndUpdate(pos, state.setValue(SOUTH_EAST, EggSlot.EMPTY));
                player.addItem(new ItemStack(Items.EGG, 1));
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        if(region == EggRegion.SOUTH_WEST) {
            if(state.getValue(SOUTH_WEST) == EggSlot.FILLED || state.getValue(SOUTH_WEST) == EggSlot.FERTILIZED) {
                level.setBlockAndUpdate(pos, state.setValue(SOUTH_WEST, EggSlot.EMPTY));
                player.addItem(new ItemStack(Items.EGG, 1));
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(0f, 0f, 0f, 1f, 0.15f, 1f);
    }
}
