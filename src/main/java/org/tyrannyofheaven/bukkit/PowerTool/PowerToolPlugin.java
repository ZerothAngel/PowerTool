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
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;
import org.tyrannyofheaven.bukkit.PowerTool.dao.PowerToolDao;
import org.tyrannyofheaven.bukkit.PowerTool.dao.YamlPowerToolDao;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.VersionInfo;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;

public class PowerToolPlugin extends JavaPlugin {

    private static final String DEFAULT_PLAYER_TOKEN = "%p";

    private static final String DEFAULT_X_TOKEN = "%x";
    
    private static final String DEFAULT_Y_TOKEN = "%y";
    
    private static final String DEFAULT_Z_TOKEN = "%z";
    
    private static final String DEFAULT_Y_AIR_TOKEN = "%Y";

    private static final boolean DEFAULT_VERBOSE = true;

    public static final int MAX_TRACE_DISTANCE = 100;

    private final Logger logger = Logger.getLogger(getClass().getName());

    private VersionInfo versionInfo;

    private final Map<Integer, PowerTool> globalPowerTools = new HashMap<Integer, PowerTool>();

    private final Map<String, PlayerState> playerStates = new HashMap<String, PlayerState>();

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

    @Override
    public void onLoad() {
        versionInfo = ToHUtils.getVersion(this);
    }

    @Override
    public void onDisable() {
        synchronized (playerStates) {
            playerStates.clear();
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

        log(this, "%s enabled.", versionInfo.getVersionString());
    }

    private void readConfig() {
        logger.setLevel(config.getBoolean("debug", false) ? Level.FINE : null);

        playerToken = config.getString("player-token", DEFAULT_PLAYER_TOKEN);
        xToken = config.getString("x-token", DEFAULT_X_TOKEN);
        yToken = config.getString("y-token", DEFAULT_Y_TOKEN);
        zToken = config.getString("z-token", DEFAULT_Z_TOKEN);
        yAirToken = config.getString("y-air-token", DEFAULT_Y_AIR_TOKEN);
        verbose = config.getBoolean("verbose", DEFAULT_VERBOSE);

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
        Map<Integer, PowerTool> powertools = getDao().loadPowerTools(null);
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

    PowerTool getPowerTool(Player player, int itemId, boolean create) {
        // Fetch global PowerTool first
        PowerTool pt = globalPowerTools.get(itemId);

        // If not defined, fetch player-specific PowerTool
        if (pt == null) {
            PlayerState ps = getPlayerState(player, create);

            if (ps != null)
                pt = ps.getPowerTool(itemId, create);
        }

        return pt;
    }

    private PlayerState getPlayerState(Player player, boolean create) {
        PlayerState ps;
        synchronized (playerStates) {
            ps = playerStates.get(player.getName());
            if (create && ps == null) {
                ps = new PlayerState();
                playerStates.put(player.getName(), ps);
            }
        }
        return ps;
    }

    boolean removePowerTool(Player player, int itemId) {
        if (globalPowerTools.containsKey(itemId)) return false;

        PlayerState ps = getPlayerState(player, false);
        
        if (ps != null)
            ps.removePowerTool(itemId);
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

    Map<Integer, PowerTool> getPowerTools(Player player) {
        PlayerState ps = getPlayerState(player, false);
        if (ps == null) return Collections.emptyMap();
        return ps.getPowerTools();
    }

    void forgetPlayer(Player player) {
        synchronized (playerStates) {
            playerStates.remove(player.getName());
        }
    }

    void execute(Player player, String commandString) {
        debug(this, "Executing command: %s", commandString);
        try {
            getServer().dispatchCommand(player, commandString);
        }
        catch (CommandException e) {
            error(this, "Execution failed: %s", commandString, e);
        }
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

    void savePersistentPowerTool(Player player, int itemId, PowerTool powerTool) {
        File playerConfigFile = getPlayerConfigFile(player);
        if (playerConfigFile == null) return;

        PowerToolDao playerDao = new YamlPowerToolDao(this, playerConfigFile);

        // Since each player has their own file, save at the global scope.
        debug(this, "Saving persistent power tool (%d) for %s", itemId, player.getName());
        playerDao.savePowerTool(null, itemId, powerTool);
    }

    void removePersistentPowerTool(Player player, int itemId) {
        File playerConfigFile = getPlayerConfigFile(player);
        if (playerConfigFile == null) return;

        PowerToolDao playerDao = new YamlPowerToolDao(this, playerConfigFile);
        
        debug(this, "Removing persistent power tool (%d) for %s", itemId, player.getName());
        playerDao.removePowerTool(null, itemId);
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
            Map<Integer, PowerTool> powerTools = playerDao.loadPowerTools(null);
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

        private final Map<Integer, PowerTool> powerTools = new HashMap<Integer, PowerTool>();

        public Map<Integer, PowerTool> getPowerTools() {
            return powerTools;
        }

        public PowerTool getPowerTool(int itemId, boolean create) {
            PowerTool pt = powerTools.get(itemId);
            if (create && pt == null) {
                pt = new PowerTool();
                powerTools.put(itemId, pt);
            }
            
            return pt;
        }

        public void removePowerTool(int itemId) {
            powerTools.remove(itemId);
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
