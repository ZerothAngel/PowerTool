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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.PowerTool.dao.PowerToolDao;
import org.tyrannyofheaven.bukkit.PowerTool.dao.YamlPowerToolDao;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.VersionInfo;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;

public class PowerToolPlugin extends JavaPlugin {

    private static final String DEFAULT_PLAYER_TOKEN = "%p";

    private final Logger logger = Logger.getLogger(getClass().getName());

    private VersionInfo versionInfo;

    private final Map<Integer, PowerTool> globalPowerTools = new HashMap<Integer, PowerTool>();

    private final Map<String, PlayerState> playerStates = new HashMap<String, PlayerState>();

    private FileConfiguration config;

    private PowerToolDao dao;

    private String playerToken;

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

        // Re-save config
        ToHFileUtils.saveConfig(this, config);

        // Install command handler
        (new ToHCommandExecutor<PowerToolPlugin>(this, new Commands(this))).registerCommands();

        // Install event listeners
        (new PowerToolPlayerListener(this)).registerEvents();
        (new PowerToolEntityListener(this)).registerEvents();

        log(this, "%s enabled.", versionInfo.getVersionString());
    }

    private void readConfig() {
        playerToken = config.getString("player-token", DEFAULT_PLAYER_TOKEN);
        boolean debug = config.getBoolean("debug", false);

        // Read global powertools
        globalPowerTools.clear();
        globalPowerTools.putAll(getDao().loadPowerTools(null));

        getLogger().setLevel(debug ? Level.FINE : null);
    }

    Logger getLogger() {
        return logger;
    }

    PowerToolDao getDao() {
        return dao;
    }

    public String getPlayerToken() {
        return playerToken;
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
