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

import java.util.HashMap;
import java.util.Map;

import org.tyrannyofheaven.bukkit.util.ToHStringUtils;

public class PowerTool {

    private final Map<PowerToolAction, Command> commandMap = new HashMap<PowerToolAction, Command>();

    private final boolean global;

    public PowerTool() {
        this(false);
    }

    public PowerTool(boolean global) {
        this.global = global;
    }

    public Command getCommand(PowerToolAction action) {
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        return commandMap.get(action);
    }

    public void setCommand(PowerToolAction action, String command, boolean hasPlayerToken) {
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        if (!ToHStringUtils.hasText(command))
            throw new IllegalArgumentException("command must have a value");
        commandMap.put(action, new Command(command, hasPlayerToken));
    }

    public void clearCommand(PowerToolAction action) {
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        commandMap.remove(action);
    }

    public boolean isEmpty() {
        return commandMap.isEmpty();
    }

    public boolean isGlobal() {
        return global;
    }

    public static class Command {
        
        private final boolean playerToken;
        
        private final String command;
        
        private Command(String command, boolean playerToken) {
            this.playerToken = playerToken;
            this.command = command;
        }

        public boolean hasPlayerToken() {
            return playerToken;
        }

        public String getCommand() {
            return command;
        }
        
        @Override
        public String toString() {
            return getCommand();
        }

    }

}
