package bletch.tektopiatinker.entities;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.leviathanstudio.craftstudio.client.animation.ClientAnimationHandler;
import com.leviathanstudio.craftstudio.common.animation.AnimationHandler;

import bletch.tektopiatinker.core.ModConfig;
import bletch.tektopiatinker.core.ModDetails;
import bletch.tektopiatinker.core.ModEntities;
import bletch.tektopiatinker.entities.ai.EntityAIVisitMerchantStall;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IMerchant;
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
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.tangotek.tektopia.ModItems;
import net.tangotek.tektopia.ProfessionType;
import net.tangotek.tektopia.TekVillager;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillagerRole;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import net.tangotek.tektopia.entities.ai.EntityAIGenericMove;
import net.tangotek.tektopia.entities.ai.EntityAIReadBook;
import net.tangotek.tektopia.entities.ai.EntityAIWanderStructure;
import net.tangotek.tektopia.tickjob.TickJob;

public class EntityTinker extends EntityVillagerTek implements IMerchant {
	
	public static final String ENTITY_NAME = "tinker";
	public static final String MODEL_NAME = "tinker";
	public static final String RESOURCE_PATH = "tinker";
	public static final String ANIMATION_MODEL_NAME = MODEL_NAME + "_m";

	protected static final DataParameter<String> ANIMATION_KEY;
	protected static final AnimationHandler<EntityTinker> animationHandler;

	private BlockPos firstCheck;
	@Nullable
	private EntityPlayer buyingPlayer;
	@Nullable
	private MerchantRecipeList vendorList;

	public EntityTinker(World worldIn) {
		super(worldIn, (ProfessionType)null, VillagerRole.VENDOR.value | VillagerRole.VISITOR.value);
		this.sleepOffset = getSleepOffset();
	}

	public EntityTinker(World worldIn, Village village) {
		super(worldIn, (ProfessionType)null, VillagerRole.VENDOR.value | VillagerRole.VISITOR.value);
		this.sleepOffset = getSleepOffset();
		attachToVillage(village);
	}

	protected void setupServerJobs() {
		super.setupServerJobs();

		this.addJob(new TickJob(100, 0, false, () -> {
			this.prepStuck();
		}));
		
		this.addJob(new TickJob(400, 0, false, () -> {
			this.checkStuck();
		}));
		
		this.addJob(new TickJob(50, 0, true, () -> {
			if (this.isSleepingTime()) {
				this.setDead();
			}
		}));
		
		this.addJob(new TickJob(300, 100, true, () -> {
			if (!this.hasVillage() || !this.getVillage().isValid()) {
				this.debugOut("Killing self...no village");
				this.setDead();
			}
		}));
	}

	private void prepStuck() {
		this.firstCheck = this.getPos();
	}

	private void checkStuck() {
		if (this.firstCheck.distanceSq(this.getPos()) < 20.0D) {
			this.debugOut("Tinker failed to find a way to the village.");
			this.setDead();
		}
	}
	
	// initEntityAI
	protected void func_184651_r() {
		super.func_184651_r();

		this.addTask(50, new EntityAIGenericMove(this, (p) -> {
			return p.hasVillage() && Village.isNightTime(this.world);
		}, (v) -> {
			return this.village.getEdgeNode();
		}, EntityVillagerTek.MovementMode.WALK, (Runnable)null, () -> {
			this.debugOut("Killing self...left the village");
			this.setDead();
		}));
		
		this.addTask(50, new EntityAIVisitMerchantStall(this, (p) -> {
			return this.hasVillage();
		}, 3, 60));
		
		this.addTask(50, new EntityAIGenericMove(this, (p) -> {
			return !Village.isNightTime(this.world) && p.hasVillage() && !this.isTrading();
		}, (v) -> {
			return this.village.getLastVillagerPos();
		}, EntityVillagerTek.MovementMode.WALK, (Runnable)null, (Runnable)null));
	}

	protected void initEntityAIBase() {
	}

	public boolean canNavigate() {
		return this.isTrading() ? false : super.canNavigate();
	}
	
	// getNewNavigator
	protected PathNavigate func_175447_b(World worldIn) {
		return super.func_175447_b(worldIn);
	}
	
	// getAIMoveSpeed
	public float func_70689_ay() {
		return super.func_70689_ay() * 0.9F;
	}

	protected void addTask(int priority, EntityAIBase task) {
		if (!(task instanceof EntityAIWanderStructure) || priority > 100) {
			if (!(task instanceof EntityAIReadBook)) {
				super.addTask(priority, task);
			}
		}
	}

	public void addVillagerPosition() {
	}

	public void setCustomer(@Nullable EntityPlayer player) {
		this.buyingPlayer = player;
		this.getNavigator().clearPath();
	}

	@Nullable
	public EntityPlayer getCustomer() {
		return this.buyingPlayer;
	}

	public boolean isTrading() {
		return this.buyingPlayer != null;
	}

	protected void bedCheck() {
	}

	public boolean isSleepingTime() {
		return isSleepingTime(this.world);
	}

	public boolean isWorkTime() {
		return isWorkTime(this.world) && !this.world.isRaining();
	}

	public boolean isLearningTime() {
		return false;
	}

	@Nullable
	public MerchantRecipeList getRecipes(EntityPlayer player) {
		if (this.vendorList == null) {
			this.populateBuyingList();
		}

		return this.vendorList;
	}

	protected boolean getCanUseDoors() {
		return true;
	}
	
	// processInteract
	public boolean func_184645_a(EntityPlayer player, EnumHand hand) {
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
	
	protected void populateBuyingList() {
		if (this.vendorList == null && this.hasVillage()) {
			this.vendorList = new MerchantRecipeList();
			
			List<Item> itemList = Arrays.asList(ModItems.structureTokens);
			itemList.sort((i1, i2) -> i1.getRegistryName().compareTo(i2.getRegistryName()));

			int emeraldsPerTinker = Math.max(1, Math.min(64, ModConfig.tinker.emeraldsPerTinker));
			int tinkersperday = Math.max(1, Math.min(99999, ModConfig.tinker.tinkersperday));
			
			// create the merchant recipe list
			for (Item item : itemList) {
				if (item == null || item == ModItems.structureTownHall)
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

	@SideOnly(Side.CLIENT)
	public void setRecipes(@Nullable MerchantRecipeList recipeList) {
	}

	public World getWorld() {
		return this.world;
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

	public ITextComponent getDisplayName() {
		ITextComponent itextcomponent = new TextComponentTranslation("entity." + MODEL_NAME + ".name", new Object[0]);
		itextcomponent.getStyle().setHoverEvent(this.getHoverEvent());
		itextcomponent.getStyle().setInsertion(this.getCachedUniqueIdString());
		return itextcomponent;
	}

	public com.google.common.base.Predicate<Entity> isHostile() {
		return (e) -> {
			return false;
		};
	}

	public boolean isFleeFrom(Entity e) {
		return false;
	}

	public BlockPos getPos() {
		return new BlockPos(this);
	}

	// writeEntityToNBT
	public void func_70014_b(NBTTagCompound compound) {
		super.func_70014_b(compound);
		
		if (this.vendorList != null) {
			compound.setTag("Offers", this.vendorList.getRecipiesAsTags());
		}
	}

	// readEntityFromNBT
	public void func_70037_a(NBTTagCompound compound) {
		super.func_70037_a(compound);

		if (compound.hasKey("Offers", 10)) {
			NBTTagCompound nbttagcompound = compound.getCompoundTag("Offers");
			this.vendorList = new MerchantRecipeList(nbttagcompound);
		}
	}

	static {
		ANIMATION_KEY = EntityDataManager.createKey(EntityTinker.class, DataSerializers.STRING);
		
		animationHandler = TekVillager.getNewAnimationHandler(EntityTinker.class);
		setupCraftStudioAnimations(animationHandler, ANIMATION_MODEL_NAME);
	}
	
	public static int getSleepOffset() {
		return 0;
	}

	public static boolean isSleepingTime(World world) {
		return Village.isTimeOfDay(world, (long)(SLEEP_START_TIME + getSleepOffset()), (long)(SLEEP_END_TIME + getSleepOffset()));
	}

	public static boolean isWorkTime(World world) {
		return Village.isTimeOfDay(world, (long)WORK_START_TIME, (long)WORK_END_TIME, (long)getSleepOffset());
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
		this.dataManager.set(ANIMATION_KEY, "");
	}

	@Override
	protected void entityInit() {
		this.dataManager.register(ANIMATION_KEY, "");
		super.entityInit();
	}

	protected void updateClientAnimation(String animationName) {
		ClientAnimationHandler<EntityTinker> clientAnimationHandler = (ClientAnimationHandler<EntityTinker>)this.getAnimationHandler();
		
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
		return animationName == this.dataManager.get(ANIMATION_KEY);
	}
}