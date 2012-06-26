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

import org.bukkit.CoalType;
import org.tyrannyofheaven.bukkit.PowerTool.ItemDataParser;

public class CoalTypeDataParser implements ItemDataParser {

    @Override
    public String toDataName(byte data) {
        CoalType coalType = CoalType.getByData(data);
        if (coalType != null)
            return coalType.name().toLowerCase();
        return null;
    }

    @Override
    public Byte parseDataName(String dataName) {
        CoalType coalType = null;
        try {
            int index = Integer.valueOf(dataName);
            coalType = CoalType.values()[index];
        }
        catch (NumberFormatException e) {
        }
        catch (IndexOutOfBoundsException e) {
        }
        if (coalType == null) {
            try {
                coalType = CoalType.valueOf(dataName.toUpperCase());
            }
            catch (IllegalArgumentException e) {
            }
        }
        if (coalType != null)
            return coalType.getData();
        return null;
    }

}
