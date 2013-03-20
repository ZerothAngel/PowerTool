/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.PowerTool;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.error;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;
import org.tyrannyofheaven.bukkit.PowerTool.dao.PowerToolDao;
import org.tyrannyofheaven.bukkit.PowerTool.dao.YamlPowerToolDao;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.VersionInfo;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;

public class PowerToolPlugin extends JavaPlugin {

    private static final String PLAYER_METADATA_KEY = "PowerTool.PlayerState";

    private static final String DEFAULT_PLAYER_TOKEN = "%p";

    private static final String DEFAULT_X_TOKEN = "%x";
    
    private static final String DEFAULT_Y_TOKEN = "%y";
    
    private static final String DEFAULT_Z_TOKEN = "%z";
    
    private static final String DEFAULT_Y_AIR_TOKEN = "%Y";

    private static final boolean DEFAULT_VERBOSE = true;

    private static final boolean DEFAULT_OMIT_FIRST_SLASH = true;

    public static final int MAX_TRACE_DISTANCE = 100;

    private VersionInfo versionInfo;

    private final Map<ItemKey, PowerTool> globalPowerTools = new HashMap<ItemKey, PowerTool>();

    private FileConfiguration config;

    private PowerToolDao dao;

    private String playerToken;

    private String xToken;
    
    private String yToken;
    
    private String zToken;
    
    private String yAirToken;

    private final List<GroupOption> groupOptions = new ArrayList<GroupOption>();

    private GroupOption defaultGroupOption;

    private boolean verbose;
    
    private boolean omitFirstSlash;

    @Override
    public void onLoad() {
        versionInfo = ToHUtils.getVersion(this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removeMetadata(PLAYER_METADATA_KEY, this);
        }

        log(this, "%s disabled.", versionInfo.getVersionString());
    }

    @Override
    public void onEnable() {
        config = ToHFileUtils.getConfig(this);
        config.options().header(null);

        // Set up DAO
        dao = new YamlPowerToolDao(this, new File(getDataFolder(), "config.yml"), config);

        // Read/create config
        readConfig();

        // Upgrade/save config
        ToHFileUtils.upgradeConfig(this, config);

        // Install command handler
        (new ToHCommandExecutor<PowerToolPlugin>(this, new Commands(this))).registerCommands();

        // Install event listeners
        getServer().getPluginManager().registerEvents(new PowerToolListener(this), this);

        // Load persistent power tools for anyone currently online
        for (Player player : getServer().getOnlinePlayers())
            loadPersistentPowerTools(player);

        log(this, "%s enabled.", versionInfo.getVersionString());
    }

    private void readConfig() {
        getLogger().setLevel(config.getBoolean("debug", false) ? Level.FINE : null);

        playerToken = config.getString("player-token", DEFAULT_PLAYER_TOKEN);
        xToken = config.getString("x-token", DEFAULT_X_TOKEN);
        yToken = config.getString("y-token", DEFAULT_Y_TOKEN);
        zToken = config.getString("z-token", DEFAULT_Z_TOKEN);
        yAirToken = config.getString("y-air-token", DEFAULT_Y_AIR_TOKEN);
        verbose = config.getBoolean("verbose", DEFAULT_VERBOSE);
        omitFirstSlash = config.getBoolean("omit-first-slash", DEFAULT_OMIT_FIRST_SLASH);

        // Group options
        groupOptions.clear();
        defaultGroupOption = new GroupOption("default");
        List<?> opts = config.getList("options");
        if (opts == null) opts = Collections.emptyList();
        for (Object o : opts) {
            if (o instanceof Map<?, ?>) {
                Map<?, ?> opt = (Map<?, ?>)o;
                Object nameObj = opt.get("name");
                if (nameObj == null) {
                    warn(this, "Missing name in options section");
                    continue;
                }
                String name = nameObj.toString();
                
                GroupOption groupOption = new GroupOption(name);

                Object limitObj = opt.get("limit");
                if (!(limitObj instanceof Number)) {
                    warn(this, "Limit for %s in options section must be a number; defaulting to -1", name);
                }
                else {
                    groupOption.setLimit(((Number)limitObj).intValue());
                }
                
                if ("default".equalsIgnoreCase(name)) {
                    defaultGroupOption = groupOption;
                }
                else
                    groupOptions.add(groupOption);
            }
            else
                warn(this, "options section must be a list of maps");
        }
        debug(this, "defaultGroupOption = %s", defaultGroupOption);
        debug(this, "groupOptions = %s", groupOptions);

        // Read global powertools
        globalPowerTools.clear();
        Map<ItemKey, PowerTool> powertools = getDao().loadPowerTools(null);
        for (PowerTool pt : powertools.values()) {
            // Set global flag
            pt.setGlobal(true);
        }
        globalPowerTools.putAll(powertools);
    }

    PowerToolDao getDao() {
        return dao;
    }

    public String getPlayerToken() {
        return playerToken;
    }

    public String getXToken() {
        return xToken;
    }
    
    public String getYToken() {
        return yToken;
    }
    
    public String getZToken() {
        return zToken;
    }
    
    public String getYAirToken() {
        return yAirToken;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isOmitFirstSlash() {
        return omitFirstSlash;
    }

    PowerTool getPowerTool(Player player, ItemStack item, boolean create) {
        ItemKey key = ItemKey.fromItemStack(item);

        // Fetch global PowerTool first
        PowerTool pt = globalPowerTools.get(key);

        // If not defined, fetch player-specific PowerTool
        if (pt == null) {
            PlayerState ps = getPlayerState(player, create);

            if (ps != null)
                pt = ps.getPowerTool(key, create);
        }

        return pt;
    }

    private PlayerState getPlayerState(Player player, boolean create) {
        PlayerState ps = null;
        for (MetadataValue mv : player.getMetadata(PLAYER_METADATA_KEY)) {
            if (mv.getOwningPlugin() == this) {
                ps = (PlayerState)mv.value();
                break;
            }
        }
        if (create && ps == null) {
            ps = new PlayerState();
            player.setMetadata(PLAYER_METADATA_KEY, new FixedMetadataValue(this, ps));
        }
        return ps;
    }

    boolean removePowerTool(Player player, ItemStack item) {
        ItemKey key = ItemKey.fromItemStack(item);
        if (globalPowerTools.containsKey(key)) return false;

        PlayerState ps = getPlayerState(player, false);
        
        if (ps != null)
            ps.removePowerTool(key);
        return true;
    }

    boolean clearPowerTools(Player player) {
        PlayerState ps = getPlayerState(player, false);
        if (ps != null) {
            boolean empty = ps.getPowerTools().isEmpty();
            ps.getPowerTools().clear();
            return !empty;
        }
        return false;
    }

    Map<ItemKey, PowerTool> getPowerTools(Player player) {
        PlayerState ps = getPlayerState(player, false);
        if (ps == null) return Collections.emptyMap();
        return ps.getPowerTools();
    }

    boolean getEnabled(Player player) {
        PlayerState ps = getPlayerState(player, false);
        if (ps != null)
            return ps.isEnabled();
        return true; // so we check global power tools
    }

    void setEnabled(Player player, boolean enabled) {
        PlayerState ps = getPlayerState(player, true);
        ps.setEnabled(enabled);
    }

    boolean toggleEnabled(Player player) {
        PlayerState ps = getPlayerState(player, true);
        boolean enabled = !ps.isEnabled(); // toggle
        ps.setEnabled(enabled);
        return enabled;
    }

    boolean shouldExecute(Player player) {
        PlayerState ps = getPlayerState(player, false);
        if (ps == null) return true; // Kinda strange, but eh...
        World world = player.getWorld();
        if (world.getName().equals(ps.getLastExecuteWorld()) && world.getTime() == ps.getLastExecuteTime())
            return false; // Already executed this power tool during this tick
        return true;
    }

    void forgetPlayer(Player player) {
        player.removeMetadata(PLAYER_METADATA_KEY, this);
    }

    void execute(Player player, String commandString) {
        debug(this, "Executing command: %s", commandString);
        try {
            PlayerCommandPreprocessEvent pcpe = new PlayerCommandPreprocessEvent(player, "/" + commandString);
            getServer().getPluginManager().callEvent(pcpe);
            
            if (pcpe.isCancelled()) {
                debug(this, "Execution cancelled: %s", commandString);
                return;
            }

            getServer().dispatchCommand(player, pcpe.getMessage().substring(1));
        }
        catch (CommandException e) {
            error(this, "Execution failed: %s", commandString, e);
        }
        PlayerState ps = getPlayerState(player, true);
        World world = player.getWorld();
        ps.setLastExecuteWorld(world.getName());
        ps.setLastExecuteTime(world.getTime());
    }

    // Performs coordinate token substitutions.
    // block may be null, in which case a BlockIterator will be run on the
    // player to find the block they are looking at.
    // May return null in odd cases.
    String substituteLocation(Player player, Block block, String command, boolean hasAirToken) {
        if (block == null) {
            for (Iterator<Block> i = new BlockIterator(player, MAX_TRACE_DISTANCE); i.hasNext();) {
                Block check = i.next();
                if (!check.isEmpty()) {
                    block = check;
                    break;
                }
            }
        }

        // Y == 0 (bedrock) when tracing toward the sky. I guess BlockIterator
        // wraps around?
        // Not sure when block will be AIR (since we explicitly check above),
        // but doesn't hurt to check!
        if (block == null || block.getY() == 0 || block.isEmpty())
            return null;

        // FIXME better way?
        command = command.replace(getXToken(), Integer.toString(block.getX()));
        command = command.replace(getYToken(), Integer.toString(block.getY()));
        command = command.replace(getZToken(), Integer.toString(block.getZ()));
        
        if (hasAirToken) {
            // Iterate blocks upwards until we find air
            while (block != null && !block.isEmpty()) {
                block = block.getRelative(0, 1, 0);
            }
            if (block == null)
                return null;
            command = command.replace(getYAirToken(), Integer.toString(block.getY()));
        }
        
        return command;
    }

    synchronized void reload() {
        config = ToHFileUtils.getConfig(this);
        if (getDao() instanceof YamlPowerToolDao) { // groan
            ((YamlPowerToolDao)getDao()).setConfig(config);
        }
        readConfig();
    }

    public static String getMaterialName(Material material) {
        if (material == null)
            throw new IllegalArgumentException("material cannot be null");
        return material.name().toLowerCase().replaceAll("_", "");
    }

    void savePersistentPowerTool(Player player, ItemStack item, PowerTool powerTool) {
        File playerConfigFile = getPlayerConfigFile(player);
        if (playerConfigFile == null) return;

        PowerToolDao playerDao = new YamlPowerToolDao(this, playerConfigFile);

        // Since each player has their own file, save at the global scope.
        ItemKey key = ItemKey.fromItemStack(item);
        debug(this, "Saving persistent power tool (%s) for %s", key, player.getName());
        playerDao.savePowerTool(null, key, powerTool);
    }

    void removePersistentPowerTool(Player player, ItemStack item) {
        File playerConfigFile = getPlayerConfigFile(player);
        if (playerConfigFile == null) return;

        PowerToolDao playerDao = new YamlPowerToolDao(this, playerConfigFile);
        
        ItemKey key = ItemKey.fromItemStack(item);
        debug(this, "Removing persistent power tool (%s) for %s", key, player.getName());
        playerDao.removePowerTool(null, key);
    }

    void clearPersistentPowerTools(Player player) {
        File playerConfigFile = getPlayerConfigFile(player);
        if (playerConfigFile == null) return;
        
        // Just be lazy and delete
        debug(this, "Clearing persistent power tools for %s", player.getName());
        if (playerConfigFile.exists() && !playerConfigFile.delete()) {
            error(this, "Unable to delete player configuration file: %s", playerConfigFile);
        }
    }

    void loadPersistentPowerTools(Player player) {
        File playerConfigFile = getPlayerConfigFile(player);
        if (playerConfigFile == null) return;
        
        if (playerConfigFile.exists()) {
            debug(this, "Loading persistent power tools for %s", player.getName());
            PowerToolDao playerDao = new YamlPowerToolDao(this, playerConfigFile);
            Map<ItemKey, PowerTool> powerTools = playerDao.loadPowerTools(null);
            if (!powerTools.isEmpty()) {
                // Load into player state
                PlayerState ps = getPlayerState(player, true);
                ps.getPowerTools().clear();
                ps.getPowerTools().putAll(powerTools);
            }
        }
    }

    private File getPlayerConfigFile(Player player) {
        File playerConfigDir = new File(getDataFolder(), "players");
        if (!playerConfigDir.exists() && !playerConfigDir.mkdirs()) {
            error(this, "Unable to create player configuration directory: %s", playerConfigDir);
            return null;
        }

        return new File(playerConfigDir, player.getName() + ".yml");
    }

    boolean isOverLimit(Player player) {
        // Figure out player's group
        GroupOption groupOption = null;
        for (GroupOption go : groupOptions) {
            // Check if it's explicitly set so we avoid defaulted values
            if (player.isPermissionSet(go.getName()) && player.hasPermission(go.getName())) {
                groupOption = go;
                break;
            }
        }

        // Use default, if necessary
        if (groupOption == null)
            groupOption = defaultGroupOption;

        debug(this, "Player %s using group option %s", player.getName(), groupOption);

        // Count the player's current number of power tools
        int current;
        PlayerState ps = getPlayerState(player, false);
        if (ps == null)
            current = 0;
        else
            current = ps.getPowerTools().size();

        int limit = groupOption.getLimit();
        return limit > -1 && current >= limit;
    }

    private static class PlayerState {

        private final Map<ItemKey, PowerTool> powerTools = new HashMap<ItemKey, PowerTool>();

        private boolean enabled = true;

        private String lastExecuteWorld;

        private long lastExecuteTime;

        public Map<ItemKey, PowerTool> getPowerTools() {
            return powerTools;
        }

        public PowerTool getPowerTool(ItemKey key, boolean create) {
            PowerTool pt = powerTools.get(key);
            if (create && pt == null) {
                pt = new PowerTool();
                powerTools.put(key, pt);
            }
            
            return pt;
        }

        public void removePowerTool(ItemKey key) {
            powerTools.remove(key);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLastExecuteWorld() {
            return lastExecuteWorld;
        }

        public void setLastExecuteWorld(String lastExecuteWorld) {
            this.lastExecuteWorld = lastExecuteWorld;
        }

        public long getLastExecuteTime() {
            return lastExecuteTime;
        }

        public void setLastExecuteTime(long lastExecuteTime) {
            this.lastExecuteTime = lastExecuteTime;
        }
        
    }

    private static class GroupOption {

        private final String name;

        private int limit = -1;
        
        private GroupOption(String name) {
            if (!hasText(name))
                throw new IllegalArgumentException("name must have a value");
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }
        
        @Override
        public String toString() {
            return String.format("GroupOption[name=%s, limit=%d]", getName(), getLimit());
        }

    }

}
