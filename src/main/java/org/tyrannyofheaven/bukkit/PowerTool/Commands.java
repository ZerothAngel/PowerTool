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

import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;

public class Commands {

    private final PowerToolPlugin plugin;

    private final List<String> disallowedCommands;

    Commands(PowerToolPlugin plugin) {
        this.plugin = plugin;

        // Commands not allowed
        PluginCommand command = plugin.getCommand("powertool");
        disallowedCommands = new ArrayList<String>(1 + command.getAliases().size());
        disallowedCommands.add(command.getName());
        disallowedCommands.addAll(command.getAliases());
    }

    @Command(value="powertool", description="Associate a command with the current item", varargs="command")
    @Require("powertool.use")
    public void powertool(CommandSender sender, String[] args, @Option({"-l", "--left"}) Boolean left, @Option({"-r", "--right"}) Boolean right, @Option({"-h", "--help"}) Boolean help, HelpBuilder helpBuilder) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, colorize("`rSilly console, power tools are for players!"));
            return;
        }
        
        Player player = (Player)sender;

        if (help != null && help) {
            helpBuilder.withCommandSender(player)
                .withHandler(this)
                .forSiblingCommand("powertool")
                .show();
            return;
        }

        // Get item in hand
        int itemId = player.getItemInHand().getTypeId();
        if (itemId == Material.AIR.getId()) {
            sendMessage(player, colorize("`rYou aren't holding anything!"));
            return;
        }

        // Determine action
        PowerToolAction action = PowerToolAction.LEFT_CLICK;
        
        if (left != null && left)
            action = PowerToolAction.LEFT_CLICK;
        else if (right != null && right)
            action = PowerToolAction.RIGHT_CLICK;
        // TODO what if more than one flag selected?
        
        if (args.length == 0) {
            // Clear the command
            PowerTool pt = plugin.getPowerTool(player, itemId, false);
            if (pt != null) {
                pt.setCommand(action, null);
                if (pt.isEmpty())
                    plugin.removePowerTool(player, itemId);
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

            // Set the command
            plugin.getPowerTool(player, itemId, true).setCommand(action, delimitedString(" ", (Object[])args));
            sendMessage(player, colorize("`yPower tool (`Y%s`y) set."), action.getDisplayName());
        }
    }

}
