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
package org.tyrannyofheaven.bukkit.PowerTool.dataparser;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GrassSpecies;
import org.tyrannyofheaven.bukkit.PowerTool.ItemDataParser;

public class GrassSpeciesDataParser implements ItemDataParser {

    private static Map<String, GrassSpecies> reverseMap = new HashMap<String, GrassSpecies>();
    
    static {
        for (GrassSpecies grassSpecies : GrassSpecies.values()) {
            reverseMap.put(grassSpecies.name().toLowerCase().replaceAll("-", ""), grassSpecies);
        }
    }

    @Override
    public String toDataName(byte data) {
        GrassSpecies grassSpecies = GrassSpecies.getByData(data);
        if (grassSpecies != null)
            return grassSpecies.name().toLowerCase().replaceAll("-", "");
        return null;
    }

    @Override
    public Byte parseDataName(String dataName) {
        GrassSpecies grassSpecies;
        try {
            grassSpecies = GrassSpecies.valueOf(dataName.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            grassSpecies = null;
        }
        if (grassSpecies == null) {
            grassSpecies = reverseMap.get(dataName.toLowerCase());
        }
        if (grassSpecies != null)
            return grassSpecies.getData();
        return null;
    }

}
