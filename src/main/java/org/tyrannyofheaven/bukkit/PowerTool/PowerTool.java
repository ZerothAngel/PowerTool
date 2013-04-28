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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.tyrannyofheaven.bukkit.util.ToHStringUtils;

public class PowerTool {

    private final Map<PowerToolAction, Command> commandMap = new HashMap<PowerToolAction, Command>();

    private boolean global;

    private boolean runAsConsole;

    private Map<String, Boolean> permissions = Collections.emptyMap();

    public Command getCommand(PowerToolAction action) {
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        return commandMap.get(action);
    }

    public void setCommand(PowerToolAction action, String command, boolean hasPlayerToken, boolean hasLocationToken, boolean hasAirToken) {
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        if (!ToHStringUtils.hasText(command))
            throw new IllegalArgumentException("command must have a value");
        commandMap.put(action, new Command(command, hasPlayerToken, hasLocationToken, hasAirToken));
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

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public boolean isRunAsConsole() {
        return isGlobal() && runAsConsole; // paranoid
    }

    public void setRunAsConsole(boolean runAsConsole) {
        this.runAsConsole = runAsConsole;
    }

    public Map<String, Boolean> getPermissions() {
        if (isGlobal())
            return permissions;
        else
            return Collections.emptyMap();
    }
    
    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = Collections.unmodifiableMap(permissions);
    }
    
    public void setPermissions(Collection<String> permissions) {
        Map<String, Boolean> permMap = new LinkedHashMap<String, Boolean>();
        for (String permission : permissions)
            permMap.put(permission, Boolean.TRUE);
        this.permissions = Collections.unmodifiableMap(permMap);
    }

    public void setPermission(String permission, boolean value) {
        this.permissions = Collections.singletonMap(permission, value);
    }

    public void setPermission(String permission) {
        this.permissions = Collections.singletonMap(permission, Boolean.TRUE);
    }

    public static class Command {
        
        private final boolean playerToken;
        
        private final boolean locationToken;
        
        private final boolean airToken;

        private final String command;
        
        private Command(String command, boolean playerToken, boolean locationToken, boolean airToken) {
            this.playerToken = playerToken;
            this.command = command;
            this.locationToken = locationToken;
            this.airToken = airToken;
        }

        public boolean hasPlayerToken() {
            return playerToken;
        }

        public boolean hasLocationToken() {
            return locationToken;
        }
        
        public boolean hasAirToken() {
            return airToken;
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
