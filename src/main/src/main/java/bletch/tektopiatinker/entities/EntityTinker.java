package bletch.tektopiatinker.entities;

import bletch.common.entities.EntityVendorBase;
import bletch.common.entities.ai.EntityAILeaveVillage;
import bletch.common.entities.ai.EntityAIVisitVillage;
import bletch.common.entities.ai.EntityAIWanderVillage;
import bletch.tektopiatinker.core.ModConfig;
import bletch.tektopiatinker.core.ModDetails;
import bletch.tektopiatinker.utils.LoggerUtils;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.tangotek.tektopia.*;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import net.tangotek.tektopia.tickjob.TickJob;

import java.util.*;

@SuppressWarnings("unchecked")
public class EntityTinker extends EntityVendorBase {

    public static final String ENTITY_NAME = "tinker";
    public static final String MODEL_NAME = "tinker";
    public static final String RESOURCE_PATH = "tinker";
    public static final String ANIMATION_MODEL_NAME = MODEL_NAME + "_m";

    protected static final DataParameter<Integer> TINKER_TYPE;

    private static final List<Integer> tinkerTypes = Arrays.asList(0, 1); // 0 = Structure Tokens, 1 = Profession Tokens

    public EntityTinker(World worldIn) {
        super(worldIn, ModDetails.MOD_ID);
    }

    public EntityTinker(World worldIn, int tinkerType) {
        this(worldIn);

        setTinkerType(tinkerType);
    }

    @Override
    public void attachToVillage(Village village) {
        super.attachToVillage(village);

        LoggerUtils.instance.info("Attaching to village", true);
    }

    @Override
    protected void checkStuck() {
        if (this.firstCheck.distanceSq(this.getPos()) < 20.0D) {
            LoggerUtils.instance.info("Killing self...failed to find a way to the village.", true);
            this.setDead();
        }
    }

    @Override
    protected void detachVillage() {
        super.detachVillage();

        LoggerUtils.instance.info("Detaching from village", true);
    }

    @Override
    public ITextComponent getDisplayName() {
        ITextComponent itextcomponent = new TextComponentTranslation("entity." + MODEL_NAME + ".name");
        itextcomponent.getStyle().setHoverEvent(this.getHoverEvent());
        itextcomponent.getStyle().setInsertion(this.getCachedUniqueIdString());
        return itextcomponent;
    }

    public int getTinkerType() {
        return this.dataManager.get(TINKER_TYPE);
    }

    public static List<Integer> getTinkerTypes() {
        return tinkerTypes;
    }

    @Override
    public boolean isMale() {
        return this.getTinkerType() == 0;
    }

    public static Boolean isTinkerTypeValid(int tinkerType) {
        for (int value : tinkerTypes) {
            if (value == tinkerType)
                return true;
        }

        return false;
    }

    @Override
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

    public void setTinkerType(int recyclerType) {
        this.dataManager.set(TINKER_TYPE, isTinkerTypeValid(recyclerType) ? recyclerType : 0);
    }

    @Override
    protected void setupAITasks() {
        this.addTask(30, new EntityAILeaveVillage(this,
                (e) -> !e.isWorkTime(),
                (e) -> e.getVillage().getEdgeNode(),
                EntityVillagerTek.MovementMode.WALK, null,
                () -> {
                    LoggerUtils.instance.info("Killing self...left the village", true);
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

    @Override
    protected void setupServerJobs() {
        super.setupServerJobs();

        this.addJob(new TickJob(100, 0, false,
                () -> this.prepStuck()));

        this.addJob(new TickJob(400, 0, false,
                () -> this.checkStuck()));

        this.addJob(new TickJob(300, 100, true,
                () -> {
                    if (!this.hasVillage() || !this.getVillage().isValid()) {
                        LoggerUtils.instance.info("Killing self...no village", true);
                        this.setDead();
                    }
                }
        ));
    }

    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);

        if (compound.hasKey("tinkerType")) {
            this.setTinkerType(compound.getInteger("tinkerType"));
        }
    }

    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);

        compound.setInteger("tinkerType", this.getTinkerType());
    }

    static {
        TINKER_TYPE = EntityDataManager.createKey(EntityTinker.class, DataSerializers.VARINT);

        setupCraftStudioAnimations(ModDetails.MOD_ID, ANIMATION_MODEL_NAME);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.dataManager.set(TINKER_TYPE, 0);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(TINKER_TYPE, 0);
        super.entityInit();
    }

}
