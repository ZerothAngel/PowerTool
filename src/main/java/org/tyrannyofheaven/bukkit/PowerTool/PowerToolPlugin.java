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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
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

    private static final int MAX_TRACE_DISTANCE = 100;

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
        (new PowerToolPlayerListener(this)).registerEvents();
        (new PowerToolEntityListener(this)).registerEvents();

        log(this, "%s enabled.", versionInfo.getVersionString());
    }

    private void readConfig() {
        playerToken = config.getString("player-token", DEFAULT_PLAYER_TOKEN);
        xToken = config.getString("x-token", DEFAULT_X_TOKEN);
        yToken = config.getString("y-token", DEFAULT_Y_TOKEN);
        zToken = config.getString("z-token", DEFAULT_Z_TOKEN);
        yAirToken = config.getString("y-air-token", DEFAULT_Y_AIR_TOKEN);
        verbose = config.getBoolean("verbose", DEFAULT_VERBOSE);
        boolean debug = config.getBoolean("debug", false);

        // Read global powertools
        globalPowerTools.clear();
        globalPowerTools.putAll(getDao().loadPowerTools(null));

        logger.setLevel(debug ? Level.FINE : null);
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

    private static class PlayerState {

        private final Map<Integer, PowerTool> powerTools = new HashMap<Integer, PowerTool>();

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

}
