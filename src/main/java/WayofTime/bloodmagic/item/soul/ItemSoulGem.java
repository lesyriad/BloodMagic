package WayofTime.bloodmagic.item.soul;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import WayofTime.bloodmagic.BloodMagic;
import WayofTime.bloodmagic.api.Constants;
import WayofTime.bloodmagic.api.iface.IMultiWillTool;
import WayofTime.bloodmagic.api.soul.EnumDemonWillType;
import WayofTime.bloodmagic.api.soul.IDemonWill;
import WayofTime.bloodmagic.api.soul.IDemonWillGem;
import WayofTime.bloodmagic.api.soul.PlayerDemonWillHandler;
import WayofTime.bloodmagic.api.util.helper.NBTHelper;
import WayofTime.bloodmagic.client.IMeshProvider;
import WayofTime.bloodmagic.client.mesh.CustomMeshDefinitionWillGem;
import WayofTime.bloodmagic.util.helper.TextHelper;

public class ItemSoulGem extends Item implements IDemonWillGem, IMeshProvider, IMultiWillTool
{
    public static String[] names = { "petty", "lesser", "common", "greater", "grand" };

    public ItemSoulGem()
    {
        super();

        setUnlocalizedName(Constants.Mod.MODID + ".soulGem.");
        setHasSubtypes(true);
        setMaxStackSize(1);
        setCreativeTab(BloodMagic.tabBloodMagic);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        return super.getUnlocalizedName(stack) + names[stack.getItemDamage()];
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
    {
        EnumDemonWillType type = this.getCurrentType(stack);
        double drain = Math.min(this.getWill(type, stack), this.getMaxWill(type, stack) / 10);

        double filled = PlayerDemonWillHandler.addDemonWill(type, player, drain, stack);
        this.drainWill(type, stack, filled, true);

        return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemMeshDefinition getMeshDefinition()
    {
        return new CustomMeshDefinitionWillGem("ItemSoulGem");
    }

    @Nullable
    @Override
    public ResourceLocation getCustomLocation()
    {
        return null;
    }

    @Override
    public List<String> getVariants()
    {
        List<String> ret = new ArrayList<String>();
        for (EnumDemonWillType type : EnumDemonWillType.values())
        {
            ret.add("type=petty_" + type.getName().toLowerCase());
            ret.add("type=lesser_" + type.getName().toLowerCase());
            ret.add("type=common_" + type.getName().toLowerCase());
            ret.add("type=greater_" + type.getName().toLowerCase());
            ret.add("type=grand_" + type.getName().toLowerCase());
        }

        return ret;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item id, CreativeTabs creativeTab, List<ItemStack> list)
    {
        for (int i = 0; i < names.length; i++)
        {
            ItemStack emptyStack = new ItemStack(this, 1, i);

            list.add(emptyStack);
        }
        for (EnumDemonWillType type : EnumDemonWillType.values())
        {
            for (int i = 0; i < names.length; i++)
            {
                ItemStack fullStack = new ItemStack(this, 1, i);
                setWill(type, fullStack, getMaxWill(EnumDemonWillType.DEFAULT, fullStack));
                list.add(fullStack);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced)
    {
        if (!stack.hasTagCompound())
            return;

        EnumDemonWillType type = this.getCurrentType(stack);
        tooltip.add(TextHelper.localize("tooltip.BloodMagic.soulGem." + names[stack.getItemDamage()]));
        tooltip.add(TextHelper.localize("tooltip.BloodMagic.will", getWill(type, stack)));
        tooltip.add(TextHelper.localizeEffect("tooltip.BloodMagic.currentType." + getCurrentType(stack).getName().toLowerCase()));

        super.addInformation(stack, player, tooltip, advanced);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack)
    {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    {
        EnumDemonWillType type = this.getCurrentType(stack);
        double maxWill = getMaxWill(type, stack);
        if (maxWill <= 0)
        {
            return 1;
        }
        return 1.0 - (getWill(type, stack) / maxWill);
    }

    @Override
    public ItemStack fillDemonWillGem(ItemStack soulGemStack, ItemStack soulStack)
    {
        if (soulStack != null && soulStack.getItem() instanceof IDemonWill)
        {
            EnumDemonWillType thisType = this.getCurrentType(soulGemStack);
            if (thisType != ((IDemonWill) soulStack.getItem()).getType(soulStack))
            {
                return soulStack;
            }
            IDemonWill soul = (IDemonWill) soulStack.getItem();
            double soulsLeft = getWill(thisType, soulGemStack);

            if (soulsLeft < getMaxWill(thisType, soulGemStack))
            {
                double newSoulsLeft = Math.min(soulsLeft + soul.getWill(thisType, soulStack), getMaxWill(thisType, soulGemStack));
                soul.drainWill(thisType, soulStack, newSoulsLeft - soulsLeft);

                setWill(thisType, soulGemStack, newSoulsLeft);
                if (soul.getWill(thisType, soulStack) <= 0)
                {
                    return null;
                }
            }
        }

        return soulStack;
    }

    @Override
    public double getWill(EnumDemonWillType type, ItemStack soulGemStack)
    {
        if (!type.equals(getCurrentType(soulGemStack)))
        {
            return 0;
        }

        NBTTagCompound tag = soulGemStack.getTagCompound();

        return tag.getDouble(Constants.NBT.SOULS);
    }

    @Override
    public void setWill(EnumDemonWillType type, ItemStack soulGemStack, double souls)
    {
        setCurrentType(type, soulGemStack);

        NBTTagCompound tag = soulGemStack.getTagCompound();

        tag.setDouble(Constants.NBT.SOULS, souls);
    }

    @Override
    public double drainWill(EnumDemonWillType type, ItemStack soulGemStack, double drainAmount, boolean doDrain)
    {
        EnumDemonWillType currentType = this.getCurrentType(soulGemStack);
        if (currentType != type)
        {
            return 0;
        }
        double souls = getWill(type, soulGemStack);

        double soulsDrained = Math.min(drainAmount, souls);

        if (doDrain)
        {
            setWill(type, soulGemStack, souls - soulsDrained);
        }

        return soulsDrained;
    }

    @Override
    public int getMaxWill(EnumDemonWillType type, ItemStack soulGemStack)
    {
        EnumDemonWillType currentType = getCurrentType(soulGemStack);
        if (!type.equals(currentType) && currentType != EnumDemonWillType.DEFAULT)
        {
            return 0;
        }

        switch (soulGemStack.getMetadata())
        {
        case 0:
            return 64;
        case 1:
            return 256;
        case 2:
            return 1024;
        case 3:
            return 4096;
        case 4:
            return 16384;
        }
        return 64;
    }

    @Override
    public EnumDemonWillType getCurrentType(ItemStack soulGemStack)
    {
        NBTHelper.checkNBT(soulGemStack);

        NBTTagCompound tag = soulGemStack.getTagCompound();

        if (!tag.hasKey(Constants.NBT.WILL_TYPE))
        {
            return EnumDemonWillType.DEFAULT;
        }

        return EnumDemonWillType.valueOf(tag.getString(Constants.NBT.WILL_TYPE).toUpperCase(Locale.ENGLISH));
    }

    public void setCurrentType(EnumDemonWillType type, ItemStack soulGemStack)
    {
        NBTHelper.checkNBT(soulGemStack);

        NBTTagCompound tag = soulGemStack.getTagCompound();

        if (type == EnumDemonWillType.DEFAULT)
        {
            if (tag.hasKey(Constants.NBT.WILL_TYPE))
            {
                tag.removeTag(Constants.NBT.WILL_TYPE);
            }

            return;
        }

        tag.setString(Constants.NBT.WILL_TYPE, type.toString());
    }

    @Override
    public double fillWill(EnumDemonWillType type, ItemStack stack, double fillAmount, boolean doFill)
    {
        if (!type.equals(getCurrentType(stack)) && this.getWill(getCurrentType(stack), stack) > 0)
        {
            return 0;
        }

        double current = this.getWill(type, stack);
        double maxWill = this.getMaxWill(type, stack);

        double filled = Math.min(fillAmount, maxWill - current);

        if (doFill)
        {
            this.setWill(type, stack, filled + current);
        }

        return filled;
    }
}
