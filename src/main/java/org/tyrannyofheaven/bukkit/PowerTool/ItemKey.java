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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.tyrannyofheaven.bukkit.PowerTool.dataparser.CoalTypeDataParser;
import org.tyrannyofheaven.bukkit.PowerTool.dataparser.DyeColorDataParser;
import org.tyrannyofheaven.bukkit.PowerTool.dataparser.GrassSpeciesDataParser;
import org.tyrannyofheaven.bukkit.PowerTool.dataparser.SandstoneTypeDataParser;
import org.tyrannyofheaven.bukkit.PowerTool.dataparser.TreeSpeciesDataParser;
import org.tyrannyofheaven.bukkit.util.ToHUtils;

public class ItemKey implements java.io.Serializable, java.lang.Comparable<ItemKey> {

    private static final long serialVersionUID = -769978887125760083L;

    private final int itemId;
    
    private final byte data;

    private final String displayName;

    private static final Map<Integer, ItemDataParser> itemDataParsers;

    static {
        Map<Integer, ItemDataParser> idps = new HashMap<Integer, ItemDataParser>();
        ItemDataParser idp;
        
        idp = new TreeSpeciesDataParser();
        idps.put(Material.WOOD.getId(), idp);
        idps.put(Material.SAPLING.getId(), idp);
        idps.put(Material.LOG.getId(), idp);
        idps.put(Material.LEAVES.getId(), idp);
        
        idp = new DyeColorDataParser();
        idps.put(Material.WOOL.getId(), idp);
        idps.put(Material.INK_SACK.getId(), idp);

        idps.put(Material.SANDSTONE.getId(), new SandstoneTypeDataParser());
        idps.put(Material.LONG_GRASS.getId(), new GrassSpeciesDataParser());
        idps.put(Material.COAL.getId(), new CoalTypeDataParser());

        // TBD
//      Material.DOUBLE_STEP,
//      Material.STEP,
//      Material.MONSTER_EGGS,
//      Material.SMOOTH_BRICK,
//      Material.MONSTER_EGG,

        itemDataParsers = Collections.unmodifiableMap(idps);
    }

    private ItemKey(int itemId, byte data, String displayName) {
        this.itemId = itemId;
        this.data = data;
        this.displayName = displayName;
    }

    public int getItemId() {
        return itemId;
    }

    public byte getData() {
        return data;
    }

    private boolean hasData() {
        return itemDataParsers.containsKey(getItemId());
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ItemKey fromItemStack(ItemStack itemStack, boolean useDisplayNames) {
        String displayName = null;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (useDisplayNames && itemMeta != null && itemMeta.hasDisplayName())
            displayName = itemMeta.getDisplayName();
        return new ItemKey(itemStack.getTypeId(), itemStack.getData().getData(), displayName);
    }

    public static ItemKey fromString(String str, boolean useDisplayNames) {
        String[] parts = str.split("/", 2);
        String rest = parts[0];
        String displayName = (useDisplayNames && parts.length == 2) ? parts[1] : null;
 
        parts = rest.split(":", 2);
        Material material = ToHUtils.matchMaterial(parts[0]);
        if (material == null) return null;
        Byte data = null;
        if (parts.length == 2) {
            // Has data
            ItemDataParser idp = itemDataParsers.get(material.getId());
            if (idp != null) {
                data = idp.parseDataName(parts[1]);
                if (data == null) return null;
            }
        }
        if (data == null)
            return new ItemKey(material.getId(), (byte)0, displayName);
        else
            return new ItemKey(material.getId(), data, displayName);
    }

    @Override
    public int compareTo(ItemKey o) {
        int diff = getItemId() - o.getItemId();
        if (diff != 0) return diff;
        if (hasData()) {
            diff = getData() - o.getData();
        }
        if (diff != 0) return diff;
        
        if (getDisplayName() == null) {
            if (o.getDisplayName() == null)
                return 0;
            else
                return -1;
        }
        else {
            if (o.getDisplayName() == null)
                return 1;
            else
                return getDisplayName().compareTo(o.getDisplayName());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ItemKey)) return false;
        ItemKey o = (ItemKey)obj;
        return o.getItemId() == getItemId() &&
                (hasData() ? o.getData() == getData() : true) &&
                (getDisplayName() == null ? o.getDisplayName() == null : getDisplayName().equals(o.getDisplayName()));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + getItemId();
        result = 37 * result + (hasData() ? (int)getData() : 0);
        result = 37 * result + (getDisplayName() == null ? 0 : getDisplayName().hashCode());
        return result;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Material material = Material.getMaterial(getItemId());
        sb.append(material.name().toLowerCase().replaceAll("_", ""));
        if (hasData() && getData() != 0) {
            String dataName = itemDataParsers.get(getItemId()).toDataName(getData());
            if (dataName != null) {
                sb.append(':');
                sb.append(dataName);
            }
        }
        if (getDisplayName() != null) {
            sb.append('/');
            sb.append(getDisplayName());
        }
        return sb.toString();
    }

}
