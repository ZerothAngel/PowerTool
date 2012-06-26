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

import org.bukkit.DyeColor;
import org.tyrannyofheaven.bukkit.PowerTool.ItemDataParser;

public class DyeColorDataParser implements ItemDataParser {

    private static Map<String, DyeColor> reverseMap = new HashMap<String, DyeColor>();
    
    static {
        for (DyeColor dyeColor : DyeColor.values()) {
            reverseMap.put(dyeColor.name().toLowerCase().replaceAll("-", ""), dyeColor);
        }
    }

    @Override
    public String toDataName(byte data) {
        DyeColor dyeColor = DyeColor.getByData(data);
        if (dyeColor != null)
            return dyeColor.name().toLowerCase().replaceAll("-", "");
        return null;
    }

    @Override
    public Byte parseDataName(String dataName) {
        DyeColor dyeColor = null;
        try {
            int index = Integer.valueOf(dataName);
            dyeColor = DyeColor.values()[index];
        }
        catch (NumberFormatException e) {
        }
        catch (IndexOutOfBoundsException e) {
        }
        if (dyeColor == null) {
            try {
                dyeColor = DyeColor.valueOf(dataName.toUpperCase());
            }
            catch (IllegalArgumentException e) {
            }
        }
        if (dyeColor == null) {
            dyeColor = reverseMap.get(dataName.toLowerCase());
        }
        if (dyeColor != null)
            return dyeColor.getData();
        return null;
    }

}
