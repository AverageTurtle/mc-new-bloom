package gay.avturtle.mixin;

import gay.avturtle.NewBloom;
import gay.avturtle.blocks.ChickenNest;
import gay.avturtle.chicken.ChickenInterface;
import gay.avturtle.chicken.FindNestGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public class ChickenMixin extends Animal implements ChickenInterface {

    public boolean new_bloom$_fertilized = false;
    private int  new_bloom$eggLayTime = this.random.nextInt(6000) + 6000;

    protected ChickenMixin(EntityType<? extends Animal> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    public void new_bloom$registerGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(2, new FindNestGoal((Chicken) (Object)this, 1.2, 24, 8));
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    public void new_bloom$aiStep(CallbackInfo ci) {
        ((Chicken)(Object)this).eggTime = 6000;
        new_bloom$eggLayTime--;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void new_bloom$readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if(tag.contains("new_bloom:EggLayTime")) {
            this.new_bloom$eggLayTime = tag.getInt("new_bloom:EggLayTime");
        }
        if(tag.contains("new_bloom:Fertilized")) {
            this.new_bloom$_fertilized = tag.getBoolean("new_bloom:Fertilized");
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void new_bloom$addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("new_bloom:EggLayTime", this.new_bloom$eggLayTime);
        tag.putBoolean("new_bloom:Fertilized", this.new_bloom$_fertilized);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal mate) {
        super.spawnChildFromBreeding(level, mate);
        new_bloom$eggLayTime = 0;
        new_bloom$_fertilized = true;
        super.finalizeSpawnChildFromBreeding(level, mate, null);
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public int getEggTimer() {
        return new_bloom$eggLayTime;
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public boolean layEgg() {
        BlockState state = getInBlockState();
        if(state.is(NewBloom.CHICKEN_NEST) && ChickenNest.hasFreeSlot(state)) {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);

            level().setBlockAndUpdate(blockPosition(), ChickenNest.fillFirstFreeSlot(state, new_bloom$_fertilized));

            new_bloom$eggLayTime = random.nextInt(6000) + 6000;
            new_bloom$_fertilized = false;
            return true;
        }
        return false;

    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.CHICKEN_FOOD);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }
}
