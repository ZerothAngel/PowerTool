package org.tyrannyofheaven.bukkit.PowerTool.dao;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;
import org.tyrannyofheaven.bukkit.PowerTool.PowerTool;
import org.tyrannyofheaven.bukkit.PowerTool.PowerToolAction;
import org.tyrannyofheaven.bukkit.PowerTool.PowerToolPlugin;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;

public class YamlPowerToolDao implements PowerToolDao {

    private static final String UNKNOWN_MATERIAL_MSG = "Unknown material '%s'; power tool ignored";

    private final PowerToolPlugin plugin;

    private final Configuration config;
    
    public YamlPowerToolDao(PowerToolPlugin plugin, File file) {
        this.plugin = plugin;
        config = new Configuration(file);
    }
    
    public YamlPowerToolDao(PowerToolPlugin plugin, Configuration config) {
        this.plugin = plugin;
        this.config = config;
    }

    private String getMaterialPath(Player player, int itemId) {
        return String.format("%s.%s", getBasePath(player), PowerToolPlugin.getMaterialName(Material.getMaterial(itemId)));
    }

    private String getBasePath(Player player) {
        String basePath;
        if (player != null) {
            basePath = String.format("players.%s", player.getName());
        }
        else {
            basePath = "powertools";
        }
        return basePath;
    }

    @Override
    public PowerTool loadPowerTool(Player player, int itemId) {
        Map<String, ConfigurationNode> nodes = config.getNodes(getBasePath(player));
        if (nodes != null) {
            // Have to iterate since the keys can have many forms...
            for (Map.Entry<String, ConfigurationNode> me : nodes.entrySet()) {
                Material material = ToHUtils.matchMaterial(me.getKey());
                if (material != null) {
                    if (material.getId() == itemId) {
                        return loadPowerTool(player == null, me.getValue());
                    }
                }
                else
                    warn(plugin, UNKNOWN_MATERIAL_MSG, me.getKey());
            }
        }
        return null;
    }

    private PowerTool loadPowerTool(boolean global, ConfigurationNode node) {
        if (node != null) {
            PowerTool pt = new PowerTool(global);
            for (PowerToolAction action : PowerToolAction.values()) {
                String command = node.getString(action.getDisplayName());
                if (ToHStringUtils.hasText(command))
                    pt.setCommand(action, command, command.contains(plugin.getPlayerToken()));
            }
            return pt;
        }
        return null;
    }

    @Override
    public Map<Integer, PowerTool> loadPowerTools(Player player) {
        Map<Integer, PowerTool> powerTools = new HashMap<Integer, PowerTool>();
        Map<String, ConfigurationNode> nodes = config.getNodes(getBasePath(player));
        if (nodes != null) {
            for (Map.Entry<String, ConfigurationNode> me : nodes.entrySet()) {
                Material material = ToHUtils.matchMaterial(me.getKey());
                if (material != null) {
                    PowerTool pt = loadPowerTool(player == null, me.getValue());
                    if (pt != null)
                        powerTools.put(material.getId(), pt);
                }
                else
                    warn(plugin, UNKNOWN_MATERIAL_MSG, me.getKey());
            }
        }
        return powerTools;
    }

    @Override
    public void removePowerTool(Player player, int itemId) {
        Map<String, ConfigurationNode> nodes = config.getNodes(getBasePath(player));
        if (nodes != null) {
            // Have to iterate since the keys can have many forms...
            for (Map.Entry<String, ConfigurationNode> me : nodes.entrySet()) {
                Material material = ToHUtils.matchMaterial(me.getKey());
                if (material != null) {
                    if (material.getId() == itemId) {
                        // TODO This can probably be done better...
                        config.removeProperty(String.format("%s.%s", getBasePath(player), me.getKey()));
                        config.save();
                        break;
                    }
                }
                else
                    warn(plugin, UNKNOWN_MATERIAL_MSG, me.getKey());
            }
        }
    }

    @Override
    public void savePowerTool(Player player, int itemId, PowerTool powerTool) {
        Map<String, ConfigurationNode> nodes = config.getNodes(getBasePath(player));
        if (nodes != null) {
            // Have to iterate since the keys can have many forms... (TODO screaming for a refactoring)
            for (Map.Entry<String, ConfigurationNode> me : nodes.entrySet()) {
                Material material = ToHUtils.matchMaterial(me.getKey());
                if (material != null) {
                    if (material.getId() == itemId) {
                        // Remove this node first.
                        config.removeProperty(String.format("%s.%s", getBasePath(player), me.getKey()));

                        // Do the actual save.
                        String materialPath = getMaterialPath(player, itemId);
                        for (PowerToolAction action : PowerToolAction.values()) {
                            PowerTool.Command command = powerTool.getCommand(action);
                            if (command != null) {
                                config.setProperty(String.format("%s.%s", materialPath, action.getDisplayName()), command.getCommand());
                            }
                        }
                        
                        // Save
                        config.save();
                    }
                }
                else
                    warn(plugin, UNKNOWN_MATERIAL_MSG, me.getKey());
            }
        }
    }

}
