package bletch.tektopiatinker.entities;

import bletch.tektopiatinker.core.ModConfig;
import bletch.tektopiatinker.core.ModDetails;
import bletch.tektopiatinker.core.ModEntities;
import bletch.tektopiatinker.entities.ai.EntityAILeaveVillage;
import bletch.tektopiatinker.entities.ai.EntityAIVisitVillage;
import bletch.tektopiatinker.entities.ai.EntityAIWanderVillage;
import bletch.tektopiatinker.utils.LoggerUtils;
import com.leviathanstudio.craftstudio.client.animation.ClientAnimationHandler;
import com.leviathanstudio.craftstudio.common.animation.AnimationHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.INpc;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.tangotek.tektopia.*;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import net.tangotek.tektopia.entities.ai.EntityAIReadBook;
import net.tangotek.tektopia.entities.ai.EntityAIWanderStructure;
import net.tangotek.tektopia.tickjob.TickJob;

import javax.annotation.Nullable;
import java.util.*;

public class EntityTinker extends EntityVillagerTek implements IMerchant, INpc {

    public static final String ENTITY_NAME = "tinker";
    public static final String MODEL_NAME = "tinker";
    public static final String RESOURCE_PATH = "tinker";
    public static final String ANIMATION_MODEL_NAME = MODEL_NAME + "_m";

    protected static final DataParameter<Integer> TINKER_TYPE;
    protected static final DataParameter<String> ANIMATION_KEY;
    protected static final AnimationHandler<EntityTinker> animationHandler;

    private static final List<Integer> tinkerTypes = Arrays.asList(0, 1); // 0 = Structure Tokens, 1 = Profession Tokens

    private BlockPos firstCheck;
    @Nullable
    private EntityPlayer buyingPlayer;
    @Nullable
    private MerchantRecipeList vendorList;

    public EntityTinker(World worldIn) {
        super(worldIn, null, VillagerRole.VENDOR.value | VillagerRole.VISITOR.value);
    }

    public EntityTinker(World worldIn, int tinkerType) {
        this(worldIn);

        this.sleepOffset = 0;

        setTinkerType(tinkerType);
    }

    protected void addTask(int priority, EntityAIBase task) {
        if (task instanceof EntityAIWanderStructure && priority <= 100) {
            return;
        }
        if (task instanceof EntityAIReadBook) {
            return;
        }

        super.addTask(priority, task);
    }

    public void addVillagerPosition() {
    }

    public void attachToVillage(Village village) {
        super.attachToVillage(village);

        LoggerUtils.info("Attaching to village", true);
    }

    protected void bedCheck() {
    }

    public boolean canNavigate() {
        return !this.isTrading() && super.canNavigate();
    }

    protected boolean getCanUseDoors() {
        return true;
    }

    private void checkStuck() {
        if (this.firstCheck.distanceSq(this.getPos()) < 20.0D) {
            LoggerUtils.info("Killing self...failed to find a way to the village.", true);
            this.setDead();
        }
    }

    protected void detachVillage() {
        super.detachVillage();

        LoggerUtils.info("Detaching from village", true);
    }

    public float getAIMoveSpeed() {
        return super.getAIMoveSpeed() * 0.9F;
    }

    @Nullable
    public EntityPlayer getCustomer() {
        return this.buyingPlayer;
    }

    public ITextComponent getDisplayName() {
        ITextComponent itextcomponent = new TextComponentTranslation("entity." + MODEL_NAME + ".name");
        itextcomponent.getStyle().setHoverEvent(this.getHoverEvent());
        itextcomponent.getStyle().setInsertion(this.getCachedUniqueIdString());
        return itextcomponent;
    }

    public BlockPos getPos() {
        return new BlockPos(this);
    }

    @Nullable
    public MerchantRecipeList getRecipes(EntityPlayer player) {
        if (this.vendorList == null) {
            this.populateBuyingList();
        }

        return this.vendorList;
    }

    public int getTinkerType() {
        return this.dataManager.get(TINKER_TYPE);
    }

    public static List<Integer> getTinkerTypes() {
        return tinkerTypes;
    }

    public World getWorld() {
        return this.world;
    }

    protected void initEntityAIBase() {
        setupAITasks();
    }

    public boolean isFleeFrom(Entity e) {
        return false;
    }

    public com.google.common.base.Predicate<Entity> isHostile() {
        return (e) -> false;
    }

    public boolean isLearningTime() {
        return false;
    }

    public boolean isMale() {
        return this.getTinkerType() == 0;
    }

    public boolean isSleepingTime() {
        return false;
    }

    public static Boolean isTinkerTypeValid(int tinkerType) {
        for (int value : tinkerTypes) {
            if (value == tinkerType)
                return true;
        }

        return false;
    }

    public boolean isTrading() {
        return this.buyingPlayer != null;
    }

    public boolean isWorkTime() {
        return isWorkTime(this.world, this.sleepOffset) && !this.world.isRaining();
    }

    protected void populateBuyingList() {
        if (this.vendorList == null && this.hasVillage()) {
            this.vendorList = new MerchantRecipeList();

            List<Item> itemList;

            switch (this.getTinkerType()) {
                case 0:
                    itemList = Arrays.asList(ModItems.structureTokens);
                    break;
                case 1:
                    itemList = new ArrayList<>(ModItems.professionTokens.values());
                    break;
                default:
                    return;
            }

            itemList.sort(Comparator.comparing(IForgeRegistryEntry.Impl::getRegistryName));

            int emeraldsPerTinker = Math.max(0, Math.min(64, ModConfig.tinker.emeraldsPerTinker));
            int tinkersperday = Math.max(1, Math.min(99999, ModConfig.tinker.tinkersperday));

            // create the merchant recipe list
            for (Item item : itemList) {
                if (item == null || item == ModItems.structureTownHall || item == ModItems.itemNitWit || item == ModItems.itemChild || item == ModItems.itemNomad)
                    continue;

                ItemStack itemStackSell = new ItemStack(item);
                ModItems.bindItemToVillage(itemStackSell, this.getVillage());

                if (emeraldsPerTinker == 0) {
                    this.vendorList.add(new MerchantRecipe(new ItemStack(item), ItemStack.EMPTY, itemStackSell, 0, tinkersperday));
                } else {
                    this.vendorList.add(new MerchantRecipe(new ItemStack(item), new ItemStack(Items.EMERALD, emeraldsPerTinker), itemStackSell, 0, tinkersperday));
                }
            }
        }
    }

    private void prepStuck() {
        this.firstCheck = this.getPos();
    }

    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (this.isEntityAlive() && !this.isTrading() && !this.isChild() && !player.isSneaking() && !this.world.isRemote) {
            if (this.vendorList == null) {
                this.populateBuyingList();
            }

            if (this.vendorList != null && !this.vendorList.isEmpty()) {
                this.setCustomer(player);
                player.displayVillagerTradeGui(this);
                this.getNavigator().clearPath();
            }
        }

        return true;
    }

    public void setCustomer(@Nullable EntityPlayer player) {
        this.buyingPlayer = player;
        this.getNavigator().clearPath();
    }

    @SideOnly(Side.CLIENT)
    public void setRecipes(@Nullable MerchantRecipeList recipeList) {
    }

    public void setTinkerType(int recyclerType) {
        this.dataManager.set(TINKER_TYPE, isTinkerTypeValid(recyclerType) ? recyclerType : 0);
    }

    protected void setupAITasks() {
        this.addTask(30, new EntityAILeaveVillage(this,
                (e) -> !e.isWorkTime(),
                (e) -> e.getVillage().getEdgeNode(),
                EntityVillagerTek.MovementMode.WALK, null,
                () -> {
                    LoggerUtils.info("Killing self...left the village", true);
                    this.setDead();
                }
        ));

        this.addTask(40, new EntityAIWanderVillage(this,
                (e) -> e.isWorkTime(), 3, 60));

        this.addTask(50, new EntityAIVisitVillage(this,
                (e) -> e.isWorkTime() && !this.isTrading(),
                (e) -> e.getVillage().getLastVillagerPos(),
                EntityVillagerTek.MovementMode.WALK, null, null));
    }

    protected void setupServerJobs() {
        super.setupServerJobs();

        this.addJob(new TickJob(100, 0, false,
                () -> this.prepStuck()));

        this.addJob(new TickJob(400, 0, false,
                () -> this.checkStuck()));

        this.addJob(new TickJob(300, 100, true,
                () -> {
                    if (!this.hasVillage() || !this.getVillage().isValid()) {
                        LoggerUtils.info("Killing self...no village", true);
                        this.setDead();
                    }
                }
        ));
    }

    public void useRecipe(MerchantRecipe recipe) {
        recipe.incrementToolUses();
        this.livingSoundTime = -this.getTalkInterval();
        this.playSound(SoundEvents.ENTITY_VILLAGER_YES, this.getSoundVolume(), this.getSoundPitch());
        int i = 3 + this.rand.nextInt(4);
        if (recipe.getToolUses() == 1 || this.rand.nextInt(5) == 0) {
            i += 5;
        }

        if (recipe.getRewardsExp()) {
            this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY + 0.5D, this.posZ, i));
        }
    }

    public void verifySellingItem(ItemStack stack) {
        if (!this.world.isRemote && this.livingSoundTime > -this.getTalkInterval() + 20) {
            this.livingSoundTime = -this.getTalkInterval();
            this.playSound(stack.isEmpty() ? SoundEvents.ENTITY_VILLAGER_NO : SoundEvents.ENTITY_VILLAGER_YES, this.getSoundVolume(), this.getSoundPitch());
        }
    }

    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);

        if (compound.hasKey("tinkerType")) {
            this.setTinkerType(compound.getInteger("tinkerType"));
        }

        if (compound.hasKey("Offers", 10)) {
            NBTTagCompound nbttagcompound = compound.getCompoundTag("Offers");
            this.vendorList = new MerchantRecipeList(nbttagcompound);
        }
    }

    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);

        compound.setInteger("tinkerType", this.getTinkerType());
        if (this.vendorList != null) {
            compound.setTag("Offers", this.vendorList.getRecipiesAsTags());
        }
    }

    static {
        TINKER_TYPE = EntityDataManager.createKey(EntityTinker.class, DataSerializers.VARINT);
        ANIMATION_KEY = EntityDataManager.createKey(EntityTinker.class, DataSerializers.STRING);

        animationHandler = TekVillager.getNewAnimationHandler(EntityTinker.class);
        setupCraftStudioAnimations(animationHandler, ANIMATION_MODEL_NAME);
    }

    public static boolean isWorkTime(World world, int sleepOffset) {
        return Village.isTimeOfDay(world, WORK_START_TIME, WORK_END_TIME, sleepOffset);
    }

    protected static void setupCraftStudioAnimations(AnimationHandler<EntityTinker> animationHandler, String modelName) {
        animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_EAT, modelName, true);
        animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_READ, modelName, true);
        animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_RUN, modelName, true);
        animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_SIT, modelName, true);
        animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_SITCHEER, modelName, true);
        animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_SLEEP, modelName, true);
        animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_WALK, modelName, true);
        animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_WALKSAD, modelName, true);
    }

    @Override
    public AnimationHandler<EntityTinker> getAnimationHandler() {
        return animationHandler;
    }

    @Override
    public void playClientAnimation(String animationName) {
        if (!this.getAnimationHandler().isAnimationActive(ModDetails.MOD_ID, animationName, this)) {
            this.getAnimationHandler().startAnimation(ModDetails.MOD_ID, animationName, this);
        }
    }

    @Override
    public void stopClientAnimation(String animationName) {
        super.stopClientAnimation(animationName);
        if (this.getAnimationHandler().isAnimationActive(ModDetails.MOD_ID, animationName, this)) {
            this.getAnimationHandler().stopAnimation(ModDetails.MOD_ID, animationName, this);
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.dataManager.set(TINKER_TYPE, 0);
        this.dataManager.set(ANIMATION_KEY, "");
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(TINKER_TYPE, 0);
        this.dataManager.register(ANIMATION_KEY, "");
        super.entityInit();
    }

    protected void updateClientAnimation(String animationName) {
        ClientAnimationHandler<EntityTinker> clientAnimationHandler = (ClientAnimationHandler<EntityTinker>) this.getAnimationHandler();

        Set<String> animChannels = clientAnimationHandler.getAnimChannels().keySet();
        animChannels.forEach(a -> clientAnimationHandler.stopAnimation(a, this));

        if (!animationName.isEmpty()) {
            clientAnimationHandler.startAnimation(ModDetails.MOD_ID, animationName, this);
        }
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);

        if (this.isWorldRemote() && ANIMATION_KEY.equals(key)) {
            this.updateClientAnimation(this.dataManager.get(ANIMATION_KEY));
        }
    }

    @Override
    public void stopServerAnimation(String animationName) {
        this.dataManager.set(ANIMATION_KEY, "");
    }

    @Override
    public void playServerAnimation(String animationName) {
        this.dataManager.set(ANIMATION_KEY, animationName);
    }

    @Override
    public boolean isPlayingAnimation(String animationName) {
        return Objects.equals(animationName, this.dataManager.get(ANIMATION_KEY));
    }
}
