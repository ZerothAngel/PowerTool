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

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;
import static org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils.requirePermission;

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

    private static final String MODIFY_GLOBAL_ERROR_MSG = "`rCannot modify a global power tool!";

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
    public void powertool(CommandSender sender, String[] args, @Option({"-r", "--right"}) Boolean right, @Option({"-c", "--clear"}) Boolean clear, @Option({"-h", "--help"}) Boolean help, HelpBuilder helpBuilder,
            @Option({"-R", "--reload"}) Boolean reload) {
        if (reload != null && reload) {
            requirePermission(sender, "powertool.reload");
            plugin.reload();
            sendMessage(sender, colorize("`yconfig.yml reloaded."));
            return;
        }

        // Doesn't make sense for non-players
        if (!(sender instanceof Player)) {
            sendMessage(sender, colorize("`rSilly %s, power tools are for players!"), sender.getName());
            return;
        }

        // Show help, if requested
        if (help != null && help) {
            helpBuilder.withCommandSender(sender)
                .withHandler(this)
                .forSiblingCommand("powertool")
                .show();
            return;
        }

        Player player = (Player)sender;

        // Get item in hand
        int itemId = player.getItemInHand().getTypeId();
        if (itemId == Material.AIR.getId()) {
            sendMessage(player, colorize("`rYou aren't holding anything!"));
            return;
        }

        if (clear != null && clear) {
            // Clear all actions
            if (plugin.removePowerTool(player, itemId))
                sendMessage(player, colorize("`yPower tool cleared."));
            else
                sendMessage(player, colorize(MODIFY_GLOBAL_ERROR_MSG));
            return;
        }

        // Determine action
        PowerToolAction action = PowerToolAction.LEFT_CLICK;
        if (right != null && right)
            action = PowerToolAction.RIGHT_CLICK;
        
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

            // Check for tokens
            boolean hasPlayerToken = false;
            for (String arg : args) {
                if (plugin.getPlayerToken().equals(arg)) {
                    hasPlayerToken = true;
                    break;
                }
            }

            // Set the command
            PowerTool pt = plugin.getPowerTool(player, itemId, true);
            if (pt.isGlobal()) {
                // TODO admin permissions?
                sendMessage(player, colorize(MODIFY_GLOBAL_ERROR_MSG));
                return;
            }
            pt.setCommand(action, delimitedString(" ", (Object[])args), hasPlayerToken);
            sendMessage(player, colorize("`yPower tool (`Y%s`y) set."), action.getDisplayName());
        }
    }

}
