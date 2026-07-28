package net.vibmc.item;

import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private static final Map<String, ItemType> REGISTRY = new HashMap<>();

    static {
        for (var field : ItemType.class.getFields()) {
            try {
                Object value = field.get(null);
                if (value instanceof ItemType item) {
                    REGISTRY.put(item.getName(), item);
                }
            } catch (IllegalAccessException e) {
                // skip
            }
        }
    }

    public static ItemType getItem(String name) {
        return REGISTRY.getOrDefault(name, ItemType.AIR);
    }

    public static ItemType getItem(int id) {
        return ItemType.getById(id);
    }

    public static ItemStack createStack(String name, int amount) {
        ItemType type = getItem(name);
        if (type == ItemType.AIR) return null;
        return new ItemStack(type, amount);
    }
}
