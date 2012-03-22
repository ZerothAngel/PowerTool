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

import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.HelpBuilder;
import org.tyrannyofheaven.bukkit.util.command.Require;

public class Commands {

    private final SubCommands subCommands;

    Commands(PowerToolPlugin plugin) {
        subCommands = new SubCommands(plugin);
    }

    @Command("powertool")
    @Require({ "powertool.use", "powertool.reload" })
    public SubCommands powertool(CommandSender sender, String[] args, HelpBuilder helpBuilder) {
        if (args.length == 0) {
            helpBuilder.withCommandSender(sender)
                .withHandler(subCommands)
                .forCommand("left")
                .forCommand("right")
                .forCommand("clear")
                .forCommand("list")
                .forCommand("reload")
                .show();
            return null;
        }
        
        return subCommands;
    }

}
