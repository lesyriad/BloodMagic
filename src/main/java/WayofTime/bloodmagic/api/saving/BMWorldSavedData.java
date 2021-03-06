package WayofTime.bloodmagic.api.saving;

import WayofTime.bloodmagic.api.util.helper.PlayerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;

import java.util.*;

public class BMWorldSavedData extends WorldSavedData
{
    public static final String ID = "BloodMagic-SoulNetworks";

    private Map<UUID, SoulNetwork> soulNetworks = new HashMap<UUID, SoulNetwork>();

    public BMWorldSavedData(String id)
    {
        super(id);
    }

    public BMWorldSavedData()
    {
        this(ID);
    }

    public SoulNetwork getNetwork(EntityPlayer player)
    {
        return getNetwork(PlayerHelper.getUUIDFromPlayer(player));
    }

    public SoulNetwork getNetwork(UUID playerId)
    {
        if (!soulNetworks.containsKey(playerId))
            soulNetworks.put(playerId, SoulNetwork.newEmpty(playerId).setParent(this));
        return soulNetworks.get(playerId);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        NBTTagList networkData = tagCompound.getTagList("networkData", 10);

        for (int i = 0; i < networkData.tagCount(); i++)
        {
            NBTTagCompound data = networkData.getCompoundTagAt(i);
            SoulNetwork network = SoulNetwork.fromNBT(data);
            network.setParent(this);
            soulNetworks.put(network.getPlayerId(), network);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        NBTTagList networkData = new NBTTagList();
        for (SoulNetwork soulNetwork : soulNetworks.values())
            networkData.appendTag(soulNetwork.serializeNBT());

        tagCompound.setTag("networkData", networkData);

        return tagCompound;
    }
}
