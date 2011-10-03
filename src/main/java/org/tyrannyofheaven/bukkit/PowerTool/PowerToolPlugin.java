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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;

public class PowerToolPlugin extends JavaPlugin {

    private static final String DEFAULT_PLAYER_TOKEN = "%p";

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Map<String, PlayerState> playerStates = new HashMap<String, PlayerState>();

    private String playerToken;

    @Override
    public void onDisable() {
        synchronized (playerStates) {
            playerStates.clear();
        }

        log(this, "%s disabled.", getDescription().getVersion());
    }

    @Override
    public void onEnable() {
        playerToken = getConfiguration().getString("player-token", DEFAULT_PLAYER_TOKEN);
        boolean debug = getConfiguration().getBoolean("debug", false);
        getConfiguration().setProperty("player-token", playerToken);
        getConfiguration().setProperty("debug", debug);
        getConfiguration().save();

        getLogger().setLevel(debug ? Level.FINE : null);

        (new ToHCommandExecutor<PowerToolPlugin>(this, new Commands(this))).registerCommands();

        (new PowerToolPlayerListener(this)).registerEvents();
        (new PowerToolEntityListener(this)).registerEvents();

        log(this, "%s enabled.", getDescription().getVersion());
    }

    Logger getLogger() {
        return logger;
    }

    String getPlayerToken() {
        return playerToken;
    }

    PowerTool getPowerTool(Player player, int itemId, boolean create) {
        PlayerState ps = getPlayerState(player, create);

        if (ps != null)
            return ps.getPowerTool(itemId, create);
        else
            return null;
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

    void removePowerTool(Player player, int itemId) {
        PlayerState ps = getPlayerState(player, false);
        
        if (ps != null)
            ps.removePowerTool(itemId);
    }

    void forgetPlayer(Player player) {
        synchronized (playerStates) {
            playerStates.remove(player.getName());
        }
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
