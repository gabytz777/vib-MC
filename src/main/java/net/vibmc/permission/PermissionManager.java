package net.vibmc.permission;

import net.vibmc.entity.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private final ConcurrentHashMap<UUID, Set<String>> permissions = new ConcurrentHashMap<>();

    public boolean hasPermission(PlayerEntity player, String permission) {
        if (permission == null || permission.isEmpty()) return true;
        Set<String> grants = permissions.get(player.getUuid());
        if (grants == null || grants.isEmpty()) return true;
        if (grants.contains("*")) return true;
        return grants.contains(permission);
    }

    public void grantPermission(PlayerEntity player, String permission) {
        permissions.computeIfAbsent(player.getUuid(), k -> ConcurrentHashMap.newKeySet()).add(permission);
    }

    public void revokePermission(PlayerEntity player, String permission) {
        Set<String> grants = permissions.get(player.getUuid());
        if (grants != null) grants.remove(permission);
    }

    public void setPermissions(PlayerEntity player, Set<String> granted) {
        permissions.put(player.getUuid(), granted);
    }

    public Set<String> getPermissions(PlayerEntity player) {
        return permissions.getOrDefault(player.getUuid(), Set.of());
    }
}
