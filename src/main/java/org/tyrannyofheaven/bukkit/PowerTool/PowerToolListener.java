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

        // NB: Don't care if it's canceled or not.
        // Interaction will be canceled if it doesn't hit a block, which is
        // something we care about.

        if (!event.hasItem()) return; // no bare fists (for now...)

        PowerTool pt = plugin.getPowerTool(event.getPlayer(), event.getItem().getTypeId(), false);
        if (pt != null) {
            PowerToolAction action = actionMap.get(event.getAction());
            if (action != null) {
                PowerTool.Command command = pt.getCommand(action);
                if (command != null) {
                    String commandString = command.getCommand();
                    if (command.hasPlayerToken()) {
                        Player targetedPlayer = findPlayerInSight(event.getPlayer());
                        if (targetedPlayer != null) {
                            debug(plugin, "%s %sed %s", event.getPlayer().getName(), action.getDisplayName(), targetedPlayer.getName());
                            commandString = commandString.replace(plugin.getPlayerToken(), targetedPlayer.getName());
                        }
                        else {
                            commandString = null;
                        }
                    }
                    else if (command.hasLocationToken()) {
                        commandString = plugin.substituteLocation(event.getPlayer(), event.getClickedBlock(), commandString, command.hasAirToken());
                    }
                    if (commandString != null) {
                        plugin.execute(event.getPlayer(), commandString);
                        event.setCancelled(true);
                    }
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

        ItemStack itemStack = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (itemStack == null) return;
        
        int itemId = itemStack.getTypeId();
        
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

    // Returns the closest player that the given player is looking at.
    // Does this by extending a cylinder from the given player's eyes out to
    // 100-blocks. The cylinder has a radius of sqrt(1/2). For each player
    // within 100 blocks, 2 points are tested: location + .5Y and location +
    // 1.5Y (basically, the Y-midpoints of a player's bottom block and top
    // block).
    // Math voodoo provided by:
    // http://www.flipcode.com/archives/Fast_Point-In-Cylinder_Test.shtml
    private Player findPlayerInSight(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector origin = eyeLocation.toVector();
        Vector end = eyeLocation.getDirection().multiply(PowerToolPlugin.MAX_TRACE_DISTANCE);
        
        final double lengthSquared = Math.pow(PowerToolPlugin.MAX_TRACE_DISTANCE, 2); // length of cylinder squared
        final double radiusSquared = 0.5; // cylinder radius of 1/2 sqrt(2). Basically from block midpoint to a corner.

        Player closest = null;
        double closestDistance = Double.MAX_VALUE;

        // FIXME I feel like this call can be improved by limiting the bounding box...
        for (Entity e : player.getNearbyEntities(PowerToolPlugin.MAX_TRACE_DISTANCE, PowerToolPlugin.MAX_TRACE_DISTANCE, PowerToolPlugin.MAX_TRACE_DISTANCE)) {
            if (!(e instanceof Player)) continue; // Only care about Players

            Player other = (Player)e;
            Location otherLoc = other.getLocation();
            // Compare against two points: Y-midpoint of bottom block, Y-midpoint of top block
            // This assumes players are 2 blocks high...
            Vector otherVec1 = new Vector(otherLoc.getX(), otherLoc.getY() + 0.5, otherLoc.getZ());
            Vector otherVec2 = new Vector(otherLoc.getX(), otherLoc.getY() + 1.5, otherLoc.getZ());
            // Vector from origin
            otherVec1.subtract(origin);
            otherVec2.subtract(origin);

            double dot1 = end.dot(otherVec1);
            double dot2 = end.dot(otherVec2);
            if ((dot1 < 0.0 || dot1 > lengthSquared) && (dot2 < 0.0 || dot2 > lengthSquared)) {
                // Beyond end caps of cylinder
                continue;
            }
            else {
                double distanceSquared1 = otherVec1.lengthSquared() - dot1 * dot1 / lengthSquared;
                double distanceSquared2 = otherVec2.lengthSquared() - dot2 * dot2 / lengthSquared;
                if (distanceSquared1 > radiusSquared && distanceSquared2 > radiusSquared) continue; // Outside cylinder

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
