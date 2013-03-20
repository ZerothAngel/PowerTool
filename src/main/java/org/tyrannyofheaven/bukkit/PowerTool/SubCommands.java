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
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
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
    @Require("powertool.create")
    public void left(CommandSender sender, String[] args) {
        Player player = playerCheck(sender);
        if (player == null) return;
        
        setPowerTool(player, args, PowerToolAction.LEFT_CLICK);
    }

    @Command(value={ "right", "r" }, description="Bind a command to the current item (right-click)", varargs="command")
    @Require("powertool.create")
    public void right(CommandSender sender, String[] args) {
        Player player = playerCheck(sender);
        if (player == null) return;

        setPowerTool(player, args, PowerToolAction.RIGHT_CLICK);
    }

    @Command(value={ "clear", "clr", "c" }, description="Clear all binds from the current item")
    @Require("powertool.create")
    public void clear(CommandSender sender, @Option("-a") Boolean all, @Option(value="item", optional=true) String item) {
        Player player = playerCheck(sender);
        if (player == null) return;


        if (!all) {
            sendMessage(player, colorize("`1(Add `w-a`1 to clear all binds from all items)"));

            // Get item in hand
            ItemStack itemStack;
            if (item == null) {
                itemStack = player.getItemInHand();
                if (itemStack.getTypeId() == Material.AIR.getId()) {
                    sendMessage(player, colorize("`rYou aren't holding anything!"));
                    return;
                }
            }
            else {
                ItemKey matchedKey = ItemKey.fromString(item);
                if (matchedKey == null) {
                    sendMessage(player, colorize("`rInvalid item ID or name."));
                    return;
                }
                itemStack = new ItemStack(matchedKey.getItemId(), 1, (short)0);
                Material material = Material.getMaterial(matchedKey.getItemId());
                // From ItemStack.createData
                MaterialData data;
                if (material == null)
                    data = new MaterialData(matchedKey.getItemId(), matchedKey.getData()); // Eh, when would this happen?
                else
                    data = material.getNewData(matchedKey.getData());
                itemStack.setData(data);
            }

            // Clear all actions
            if (plugin.removePowerTool(player, itemStack)) {
                sendMessage(player, colorize("`yPower tool cleared."));
                plugin.removePersistentPowerTool(player, itemStack);
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
    @Require("powertool.create")
    public void list(CommandSender sender, @Option(value="page", optional=true) Integer page) {
        Player player = playerCheck(sender);
        if (player == null) return;

        List<Map.Entry<ItemKey, PowerTool>> powertools = new ArrayList<Map.Entry<ItemKey, PowerTool>>(plugin.getPowerTools(player).entrySet());
        // Sort by itemId
        Collections.sort(powertools, new Comparator<Map.Entry<ItemKey, PowerTool>>() {
            @Override
            public int compare(Entry<ItemKey, PowerTool> a, Entry<ItemKey, PowerTool> b) {
                return a.getKey().compareTo(b.getKey());
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

            for (Map.Entry<ItemKey, PowerTool> me : powertools) {
                boolean headerSent = false;

                for (PowerToolAction action : PowerToolAction.values()) {
                    PowerTool.Command command = me.getValue().getCommand(action);
                    if (command != null) {
                        if (!headerSent) {
                            sendMessage(player, colorize("`y%s:"), me.getKey());
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

    @Command(value="on", description="Turn power tools on for this session")
    public void enable(CommandSender sender) {
        Player player = playerCheck(sender);
        if (player == null) return;
        
        plugin.setEnabled(player, true);
        sendEnableMessage(player, true);
    }

    @Command(value="off", description="Turn power tools off for this session")
    public void disable(CommandSender sender) {
        Player player = playerCheck(sender);
        if (player == null) return;
        
        plugin.setEnabled(player, false);
        sendEnableMessage(player, false);
    }

    @Command(value={ "toggle", "t" }, description="Toggle power tools availability")
    public void toggle(CommandSender sender) {
        Player player = playerCheck(sender);
        if (player == null) return;
        
        boolean enabled = plugin.toggleEnabled(player);
        sendEnableMessage(player, enabled);
    }

    private void sendEnableMessage(Player player, boolean enabled) {
        sendMessage(player, colorize("`yPower tools %s`y."), enabled ? colorize("`genabled") : colorize("`rdisabled"));
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
        ItemStack itemStack = player.getItemInHand();
        if (itemStack.getTypeId() == Material.AIR.getId()) {
            sendMessage(player, colorize("`rYou aren't holding anything!"));
            return;
        }

        if (args.length == 0) {
            // Clear the command
            PowerTool pt = plugin.getPowerTool(player, itemStack, false);
            if (pt != null) {
                if (pt.isGlobal()) {
                    // TODO admin permissions?
                    sendMessage(player, colorize(MODIFY_GLOBAL_ERROR_MSG));
                    return;
                }
                pt.clearCommand(action);
                if (pt.isEmpty()) {
                    plugin.removePowerTool(player, itemStack);
                    plugin.removePersistentPowerTool(player, itemStack);
                }
                else {
                    // Re-save
                    plugin.savePersistentPowerTool(player, itemStack, pt);
                }
            }
            sendMessage(player, colorize("`yPower tool (`Y%s`y) cleared."), action.getDisplayName());
            
        }
        else {
            if (!plugin.isOmitFirstSlash()) {
                // Require initial slash
                if (args[0] != null && !args[0].startsWith("/")) {
                    sendMessage(player, colorize("`rBound command must begin with slash."));
                    return;
                }

                // Then remove it
                args[0] = args[0].substring(1);
            }

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

            PowerTool pt = plugin.getPowerTool(player, itemStack, false);
            if (pt == null) {
                // Only check limit if we have to create a new one
                if (plugin.isOverLimit(player)) {
                    sendMessage(player, colorize("`rYou have reached your power tool limit."));
                    return;
                }
                // Create a brand new power tool
                pt = plugin.getPowerTool(player, itemStack, true);
            }

            // Set the command
            if (pt.isGlobal()) {
                // TODO admin permissions?
                sendMessage(player, colorize(MODIFY_GLOBAL_ERROR_MSG));
                return;
            }
            pt.setCommand(action, delimitedString(" ", (Object[])args), hasPlayerToken, hasLocationToken, hasAirToken);
            sendMessage(player, colorize("`yPower tool (`Y%s`y) set."), action.getDisplayName());
            
            plugin.savePersistentPowerTool(player, itemStack, pt);
        }
    }

}
