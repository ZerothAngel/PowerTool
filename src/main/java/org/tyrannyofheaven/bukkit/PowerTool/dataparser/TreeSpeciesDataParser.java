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

import org.bukkit.TreeSpecies;
import org.tyrannyofheaven.bukkit.PowerTool.ItemDataParser;

public class TreeSpeciesDataParser implements ItemDataParser {

    @Override
    public String toDataName(byte data) {
        TreeSpecies treeSpecies = TreeSpecies.getByData(data);
        if (treeSpecies != null)
            return treeSpecies.name().toLowerCase();
        return null;
    }

    @Override
    public Byte parseDataName(String dataName) {
        TreeSpecies treeSpecies = null;
        try {
            int index = Integer.valueOf(dataName);
            treeSpecies = TreeSpecies.values()[index];
        }
        catch (NumberFormatException e) {
        }
        catch (IndexOutOfBoundsException e) {
        }
        if (treeSpecies == null) {
            try {
                treeSpecies = TreeSpecies.valueOf(dataName.toUpperCase());
            }
            catch (IllegalArgumentException e) {
            }
        }
        if (treeSpecies != null)
            return treeSpecies.getData();
        return null;
    }

}
