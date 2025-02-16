package gay.avturtle.chicken;

import gay.avturtle.NewBloom;
import gay.avturtle.blocks.ChickenNest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class FindNestGoal extends MoveToBlockGoal {
    public static final Block TARGET_BLOCK = NewBloom.CHICKEN_NEST.get();

    public final Chicken chicken;
    public FindNestGoal(Chicken chicken, double speedModifier, int range, int verticalRange) {
        super(chicken, speedModifier, range, verticalRange);
        this.chicken = chicken;
    }

    @Override
    public boolean canUse() {
        return ((ChickenInterface)chicken).getEggTimer() <= 0 && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !((ChickenInterface)chicken).layEgg();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        ChunkAccess chunkaccess = level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
        return chunkaccess != null && chunkaccess.getBlockState(pos).is(TARGET_BLOCK) && ChickenNest.hasFreeSlot(chunkaccess.getBlockState(pos));
    }


}
