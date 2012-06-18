package org.tyrannyofheaven.bukkit.PowerTool.dao;

import java.util.Map;

import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.PowerTool.ItemKey;
import org.tyrannyofheaven.bukkit.PowerTool.PowerTool;

public interface PowerToolDao {

    public void savePowerTool(Player player, ItemKey key, PowerTool powerTool);

    public PowerTool loadPowerTool(Player player, ItemKey key);
    
    public Map<ItemKey, PowerTool> loadPowerTools(Player player);

    public void removePowerTool(Player player, ItemKey key);

}
