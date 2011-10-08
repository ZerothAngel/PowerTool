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
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.registerEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PowerToolPlayerListener extends PlayerListener {

    private static final Map<Action, PowerToolAction> actionMap;

    private final PowerToolPlugin plugin;

    static {
        Map<Action, PowerToolAction> am = new HashMap<Action, PowerToolAction>();

        am.put(Action.LEFT_CLICK_AIR, PowerToolAction.LEFT_CLICK);
        am.put(Action.LEFT_CLICK_BLOCK, PowerToolAction.LEFT_CLICK);
        am.put(Action.RIGHT_CLICK_AIR, PowerToolAction.RIGHT_CLICK);
        am.put(Action.RIGHT_CLICK_BLOCK, PowerToolAction.RIGHT_CLICK);

        actionMap = Collections.unmodifiableMap(am);
    }

    PowerToolPlayerListener(PowerToolPlugin plugin) {
        this.plugin = plugin;
    }

    void registerEvents() {
        registerEvent("PLAYER_INTERACT", this, Priority.Normal, plugin);
        registerEvent("PLAYER_INTERACT_ENTITY", this, Priority.Normal, plugin);
        registerEvent("PLAYER_QUIT", this, Priority.Monitor, plugin);
        registerEvent("PLAYER_ITEM_HELD", this, Priority.Monitor, plugin);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        // NB: Don't care if it's canceled or not.
        // Interaction will be canceled if it doesn't hit a block, which is
        // something we care about.

        if (!event.hasItem()) return; // no bare fists (for now...)

        PowerTool pt = plugin.getPowerTool(event.getPlayer(), event.getItem().getTypeId(), false);
        if (pt != null) {
            PowerToolAction action = actionMap.get(event.getAction());
            if (action != null) {
                PowerTool.Command command = pt.getCommand(action);
                if (command != null && !command.hasPlayerToken()) {
                    plugin.execute(event.getPlayer(), command.getCommand());
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        int itemId = event.getPlayer().getItemInHand().getTypeId();
        
        if (itemId == Material.AIR.getId()) return;
        
        PowerTool pt = plugin.getPowerTool(event.getPlayer(), itemId, false);
        if (pt != null) {
            PowerTool.Command command = pt.getCommand(PowerToolAction.RIGHT_CLICK);
            if (command != null) {
                String commandString = null;
                
                if (command.hasPlayerToken()) {
                    // Player is required, only execute if player was target
                    Entity entity = event.getRightClicked();
                    if (entity instanceof Player) {
                        Player clicked = (Player)entity;
                        debug(plugin, "%s right-clicked %s", event.getPlayer().getName(), clicked.getName());
    
                        commandString = command.getCommand().replace(plugin.getPlayerToken(), clicked.getName());
                    }
                }
                
                if (commandString != null) {
                    plugin.execute(event.getPlayer(), commandString);
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.forgetPlayer(event.getPlayer());
    }

    @Override
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        int itemId = event.getPlayer().getInventory().getItem(event.getNewSlot()).getTypeId();
        
        if (itemId == Material.AIR.getId()) return;

        PowerTool pt = plugin.getPowerTool(event.getPlayer(), itemId, false);
        if (pt != null) {
            boolean headerSent = false;

            for (PowerToolAction action : PowerToolAction.values()) {
                PowerTool.Command command = pt.getCommand(action);
                if (command != null) {
                    if (!headerSent) {
                        sendMessage(event.getPlayer(), colorize("`yPower tool:"));
                        headerSent = true;
                    }
                    
                    sendMessage(event.getPlayer(), colorize("  `y%s: `g%s"), action.getDisplayName(), command);
                }
            }
        }
    }

}
