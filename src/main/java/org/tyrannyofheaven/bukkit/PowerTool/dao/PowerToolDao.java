package org.tyrannyofheaven.bukkit.PowerTool.dao;

import java.util.Map;

import org.tyrannyofheaven.bukkit.PowerTool.ItemKey;
import org.tyrannyofheaven.bukkit.PowerTool.PowerTool;

public interface PowerToolDao {

    public void savePowerTool(ItemKey key, PowerTool powerTool);

    public Map<ItemKey, PowerTool> loadPowerTools(boolean global);

    public void removePowerTool(ItemKey key);

}
