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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PowerToolListener implements Listener {

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

    PowerToolListener(PowerToolPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().hasPermission("powertool.use")) return;
        if (!plugin.getEnabled(event.getPlayer())) return;

        // NB: Don't care if it's canceled or not.
        // Interaction will be canceled if it doesn't hit a block, which is
        // something we care about.

        if (!event.hasItem()) return; // no bare fists (for now...)

        PowerTool pt = plugin.getPowerTool(event.getPlayer(), event.getItem(), false);
        if (pt != null) {
            PowerToolAction action = actionMap.get(event.getAction());
            if (action != null) {
                PowerTool.Command command = pt.getCommand(action);
                if (command != null) {
                    debug(plugin, "Power tool candidate: %s", command);
                    if (plugin.shouldExecute(event.getPlayer())) {
                        String commandString = command.getCommand();
                        if (command.hasPlayerToken()) {
                            Player targetedPlayer = findPlayerInSight(event.getPlayer());
                            if (targetedPlayer != null) {
                                debug(plugin, "%s %sed %s", event.getPlayer().getName(), action.getDisplayName(), targetedPlayer.getName());
                                commandString = commandString.replace(plugin.getPlayerToken(), targetedPlayer.getName());
                            }
                            else {
                                debug(plugin, "No player target");
                                commandString = null;
                            }
                        }
                        else if (command.hasLocationToken()) {
                            commandString = plugin.substituteLocation(event.getPlayer(), event.getClickedBlock(), commandString, command.hasAirToken());
                        }
                        if (commandString != null) {
                            plugin.execute(event.getPlayer(), commandString);
                        }
                    }
                    else {
                        debug(plugin, "Already executed");
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player)event.getDamager();

            if (!attacker.hasPermission("powertool.use")) return;
            if (!plugin.getEnabled(attacker)) return;

            int itemId = attacker.getItemInHand().getTypeId();

            if (itemId == Material.AIR.getId()) return;

            PowerTool pt = plugin.getPowerTool(attacker, attacker.getItemInHand(), false);
            if (pt != null) {
                PowerTool.Command command = pt.getCommand(PowerToolAction.LEFT_CLICK);
                if (command != null) {
                    debug(plugin, "Power tool candidate*: %s", command);
                    if (plugin.shouldExecute(attacker)) {
                        String commandString = null;

                        if (command.hasPlayerToken()) {
                            // Player target required
                            if (event.getEntity() instanceof Player) {
                                Player victim = (Player)event.getEntity();
                                debug(plugin, "%s left-clicked* %s", attacker.getName(), victim.getName());

                                commandString = command.getCommand().replace(plugin.getPlayerToken(), victim.getName());
                            }
                        }
                        else {
                            // A left-click is a left-click
                            commandString = command.getCommand();
                        }

                        if (commandString != null) {
                            if (command.hasLocationToken()) {
                                commandString = plugin.substituteLocation(attacker, null, commandString, command.hasAirToken());
                            }
                            if (commandString != null) {
                                plugin.execute(attacker, commandString);
                            }
                        }
                    }
                    else {
                        debug(plugin, "Already executed*");
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.loadPersistentPowerTools(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.forgetPlayer(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        if (!plugin.isVerbose()) return;

        if (!event.getPlayer().hasPermission("powertool.use")) return;
        if (!plugin.getEnabled(event.getPlayer())) return;

        ItemStack itemStack = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (itemStack == null) return;
        
        int itemId = itemStack.getTypeId();
        
        if (itemId == Material.AIR.getId()) return;

        PowerTool pt = plugin.getPowerTool(event.getPlayer(), itemStack, false);
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

    // Determines closest player in-line with crosshairs.
    // Uses ray-AABB intersection test.
    private Player findPlayerInSight(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector origin = eyeLocation.toVector();
        Vector direction = eyeLocation.getDirection().multiply(PowerToolPlugin.MAX_TRACE_DISTANCE);
        
        Player closest = null;
        double closestDistance = Double.MAX_VALUE;

        // FIXME I feel like this call can be improved by limiting the bounding box...
        for (Entity e : player.getNearbyEntities(PowerToolPlugin.MAX_TRACE_DISTANCE, PowerToolPlugin.MAX_TRACE_DISTANCE, PowerToolPlugin.MAX_TRACE_DISTANCE)) {
            if (!(e instanceof Player)) continue; // Only care about Players

            Player other = (Player)e;
            Location otherLoc = other.getLocation();

            // Determine bounds of 1x2x1 AABB
            Vector minB = new Vector(otherLoc.getX() - 0.5, otherLoc.getY(), otherLoc.getZ() - 0.5);
            Vector maxB = new Vector(otherLoc.getX() + 0.5, otherLoc.getY() + 2.0, otherLoc.getZ() + 0.5);

            if (Utils.hitBoundingBox(minB, maxB, origin, direction, null)) {
                // Player is within crosshairs
                // If they're closer than the current closest, remember them
                double distanceSquared = origin.distanceSquared(otherLoc.toVector());
                if (distanceSquared < closestDistance) {
                    closest = other;
                    closestDistance = distanceSquared;
                }
            }
        }
        
        return closest;
    }

}
