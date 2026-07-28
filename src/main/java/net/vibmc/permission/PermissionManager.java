package net.vibmc.permission;

import net.vibmc.entity.PlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private final Map<String, Permission> permissions;
    private final Map<UUID, Set<String>> playerPermissions;

    public PermissionManager() {
        this.permissions = new ConcurrentHashMap<>();
        this.playerPermissions = new ConcurrentHashMap<>();
        registerDefaults();
    }

    private void registerDefaults() {
        register(new Permission("vibmc.*", "All vib-MC permissions"));
        register(new Permission("vibmc.command.help", "Access /help"));
        register(new Permission("vibmc.command.tp", "Access /tp"));
        register(new Permission("vibmc.command.gamemode", "Access /gamemode"));
        register(new Permission("vibmc.command.time", "Access /time"));
        register(new Permission("vibmc.command.weather", "Access /weather"));
        register(new Permission("vibmc.command.give", "Access /give"));
        register(new Permission("vibmc.command.kill", "Access /kill"));
        register(new Permission("vibmc.command.say", "Access /say"));
        register(new Permission("vibmc.command.seed", "Access /seed"));
        register(new Permission("vibmc.command.save", "Access /save-all"));
        register(new Permission("vibmc.command.stop", "Access /stop"));
        register(new Permission("vibmc.command.list", "Access /list"));
        register(new Permission("vibmc.admin", "Admin privileges"));
    }

    public void register(Permission permission) {
        permissions.put(permission.getName(), permission);
    }

    public boolean hasPermission(PlayerEntity player, String permission) {
        if (permission == null || permission.isEmpty()) return true;

        Set<String> perms = playerPermissions.get(player.getUuid());
        if (perms != null) {
            if (perms.contains("*") || perms.contains("vibmc.*")) return true;
            if (perms.contains(permission)) return true;

            // Check wildcard parents
            for (String p : perms) {
                if (p.endsWith(".*")) {
                    String base = p.substring(0, p.length() - 2);
                    if (permission.startsWith(base)) return true;
                }
            }
        }

        // OP check
        return isOp(player);
    }

    public void setPermission(PlayerEntity player, String permission) {
        playerPermissions.computeIfAbsent(player.getUuid(), k -> ConcurrentHashMap.newKeySet())
                .add(permission);
    }

    public void removePermission(PlayerEntity player, String permission) {
        Set<String> perms = playerPermissions.get(player.getUuid());
        if (perms != null) {
            perms.remove(permission);
        }
    }

    public boolean isOp(PlayerEntity player) {
        return false; // OP status not fully implemented
    }

    public void setOp(PlayerEntity player, boolean op) {
        // Would persist to ops.json
    }

    public Permission getPermission(String name) {
        return permissions.get(name);
    }

    public Collection<Permission> getPermissions() {
        return permissions.values();
    }
}
