package org.tyrannyofheaven.bukkit.PowerTool.dao;

import java.util.Map;

import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.PowerTool.PowerTool;

public interface PowerToolDao {

    public void savePowerTool(Player player, int itemId, PowerTool powerTool);

    public PowerTool loadPowerTool(Player player, int itemId);
    
    public Map<Integer, PowerTool> loadPowerTools(Player player);

    public void removePowerTool(Player player, int itemId);

}
