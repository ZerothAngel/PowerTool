/*
 * Copyright 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
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

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;

public class SubCommands {

    private static final String MODIFY_GLOBAL_ERROR_MSG = "`rCannot modify a global power tool!";

    private final PowerToolPlugin plugin;

    private final List<String> disallowedCommands;

    SubCommands(PowerToolPlugin plugin) {
        this.plugin = plugin;

        // Commands not allowed
        PluginCommand command = plugin.getCommand("powertool");
        disallowedCommands = new ArrayList<String>(1 + command.getAliases().size());
        disallowedCommands.add(command.getName());
        disallowedCommands.addAll(command.getAliases());
    }

    @Command(value={ "left", "l" }, description="Bind a command to the current item (left-click)", varargs="command")
    @Require("powertool.use")
    public void left(CommandSender sender, String[] args) {
        Player player = playerCheck(sender);
        if (player == null) return;
        
        setPowerTool(player, args, PowerToolAction.LEFT_CLICK);
    }

    @Command(value={ "right", "r" }, description="Bind a command to the current item (right-click)", varargs="command")
    @Require("powertool.use")
    public void right(CommandSender sender, String[] args) {
        Player player = playerCheck(sender);
        if (player == null) return;

        setPowerTool(player, args, PowerToolAction.RIGHT_CLICK);
    }

    @Command(value={ "clear", "clr", "c" }, description="Clear all binds from the current item")
    @Require("powertool.use")
    public void clear(CommandSender sender, @Option("-a") Boolean all, @Option(value="item", optional=true) String item) {
        Player player = playerCheck(sender);
        if (player == null) return;


        if (!all) {
            sendMessage(player, colorize("`1(Add `w-a`1 to clear all binds from all items)"));

            // Get item in hand
            int itemId;
            if (item == null) {
                itemId = player.getItemInHand().getTypeId();
                if (itemId == Material.AIR.getId()) {
                    sendMessage(player, colorize("`rYou aren't holding anything!"));
                    return;
                }
            }
            else {
                Material material = ToHUtils.matchMaterial(item);
                if (material == null) {
                    sendMessage(player, colorize("`rInvalid item ID or name."));
                    return;
                }
                itemId = material.getId();
            }

            // Clear all actions
            if (plugin.removePowerTool(player, itemId)) {
                sendMessage(player, colorize("`yPower tool cleared."));
                plugin.removePersistentPowerTool(player, itemId);
            }
            else
                sendMessage(player, colorize(MODIFY_GLOBAL_ERROR_MSG));
        }
        else {
            if (plugin.clearPowerTools(player))
                plugin.clearPersistentPowerTools(player);
            sendMessage(player, colorize("`yAll power tools cleared."));
        }
    }

    @Command(value={ "list", "ls" }, description="List all binds for all items")
    @Require("powertool.use")
    public void list(CommandSender sender, @Option(value="page", optional=true) Integer page) {
        Player player = playerCheck(sender);
        if (player == null) return;

        List<Map.Entry<Integer, PowerTool>> powertools = new ArrayList<Map.Entry<Integer, PowerTool>>(plugin.getPowerTools(player).entrySet());
        // Sort by itemId
        Collections.sort(powertools, new Comparator<Map.Entry<Integer, PowerTool>>() {
            @Override
            public int compare(Entry<Integer, PowerTool> a, Entry<Integer, PowerTool> b) {
                return a.getKey() - b.getKey();
            }
        });

        if (!powertools.isEmpty()) {
            final int TOOLS_PER_PAGE = 5;
            
            if (page == null) page = 1;

            // Paginate
            int pages = (powertools.size() + TOOLS_PER_PAGE - 1) / TOOLS_PER_PAGE;
            if (page < 1) page = 1;
            if (page > pages) page = pages;

            page--; // make 0-index

            // Slice the list
            int end = page * TOOLS_PER_PAGE + TOOLS_PER_PAGE;
            if (end > powertools.size()) end = powertools.size();
            powertools = powertools.subList(page * TOOLS_PER_PAGE, end);

            for (Map.Entry<Integer, PowerTool> me : powertools) {
                boolean headerSent = false;

                for (PowerToolAction action : PowerToolAction.values()) {
                    PowerTool.Command command = me.getValue().getCommand(action);
                    if (command != null) {
                        if (!headerSent) {
                            sendMessage(player, colorize("`y%s:"), PowerToolPlugin.getMaterialName(Material.getMaterial(me.getKey())));
                            headerSent = true;
                        }

                        sendMessage(player, colorize("  `y%s: `g%s"), action.getDisplayName(), command);
                    }
                }
            }
            
            if (pages > 1)
                sendMessage(player, colorize("`y----- Page %d of %d -----"), page + 1, pages);
        }
        else {
            sendMessage(player, colorize("`yYou have no power tools defined."));
        }
    }

    @Command(value="reload", description="Re-read config.yml")
    @Require("powertool.reload")
    public void reload(CommandSender sender) {
        plugin.reload();
        sendMessage(sender, colorize("`yconfig.yml reloaded."));
    }

    private Player playerCheck(CommandSender sender) {
        // Doesn't make sense for non-players
        if (!(sender instanceof Player)) {
            sendMessage(sender, colorize("`rSilly %s, power tools are for players!"), sender.getName());
            return null;
        }
        return (Player)sender;
    }

    private void setPowerTool(Player player, String[] args, PowerToolAction action) {
        // Get item in hand
        int itemId = player.getItemInHand().getTypeId();
        if (itemId == Material.AIR.getId()) {
            sendMessage(player, colorize("`rYou aren't holding anything!"));
            return;
        }

        if (args.length == 0) {
            // Clear the command
            PowerTool pt = plugin.getPowerTool(player, itemId, false);
            if (pt != null) {
                if (pt.isGlobal()) {
                    // TODO admin permissions?
                    sendMessage(player, colorize(MODIFY_GLOBAL_ERROR_MSG));
                    return;
                }
                pt.clearCommand(action);
                if (pt.isEmpty()) {
                    plugin.removePowerTool(player, itemId);
                    plugin.removePersistentPowerTool(player, itemId);
                }
                else {
                    // Re-save
                    plugin.savePersistentPowerTool(player, itemId, pt);
                }
            }
            sendMessage(player, colorize("`yPower tool (`Y%s`y) cleared."), action.getDisplayName());
            
        }
        else {
            // Validate command
            for (String name : disallowedCommands) {
                if (name.equalsIgnoreCase(args[0])) {
                    sendMessage(player, colorize("`rRecursion not allowed!"));
                    return;
                }
            }

            // Check for tokens
            boolean hasPlayerToken = false;
            boolean hasLocationToken = false;
            boolean hasAirToken = false;
            for (String arg : args) {
                if (arg.contains(plugin.getPlayerToken())) {
                    hasPlayerToken = true;
                }
                // FIXME gotta be a better way to do this
                else if (arg.contains(plugin.getXToken()) ||
                        arg.contains(plugin.getYToken()) ||
                        arg.contains(plugin.getZToken())) {
                    hasLocationToken = true;
                }
                else if (arg.contains(plugin.getYAirToken())) {
                    hasLocationToken = true;
                    hasAirToken = true;
                }
            }
            if (hasPlayerToken && hasLocationToken) {
                sendMessage(player, colorize("`rCannot use player and coordinate tokens simultaneously!"));
                return;
            }

            PowerTool pt = plugin.getPowerTool(player, itemId, false);
            if (pt == null) {
                // Only check limit if we have to create a new one
                if (plugin.isOverLimit(player)) {
                    sendMessage(player, colorize("`rYou have reached your power tool limit."));
                    return;
                }
                // Create a brand new power tool
                pt = plugin.getPowerTool(player, itemId, true);
            }

            // Set the command
            if (pt.isGlobal()) {
                // TODO admin permissions?
                sendMessage(player, colorize(MODIFY_GLOBAL_ERROR_MSG));
                return;
            }
            pt.setCommand(action, delimitedString(" ", (Object[])args), hasPlayerToken, hasLocationToken, hasAirToken);
            sendMessage(player, colorize("`yPower tool (`Y%s`y) set."), action.getDisplayName());
            
            plugin.savePersistentPowerTool(player, itemId, pt);
        }
    }

}
