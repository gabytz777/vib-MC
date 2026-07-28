package net.vibmc.advancement;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancementManager {
    private final Map<String, Advancement> advancements;
    private final Map<UUID, Set<String>> playerProgress;

    public AdvancementManager() {
        this.advancements = new LinkedHashMap<>();
        this.playerProgress = new ConcurrentHashMap<>();
        registerDefaults();
    }

    private void registerDefaults() {
        Advancement root = new Advancement(
            "minecraft:story/root",
            "Minecraft",
            "The heart and story of the game",
            new ItemStack(ItemType.COOKED_BEEF),
            null, "task", false, false
        );
        register(root);

        Advancement stoneAge = new Advancement(
            "minecraft:story/stone_age",
            "Stone Age",
            "Mine stone with your new pickaxe",
            new ItemStack(ItemType.COBBLESTONE),
            root, "task", true, false
        );
        register(stoneAge);

        Advancement ironAge = new Advancement(
            "minecraft:story/iron_tools",
            "Iron Age",
            "Smelt an iron ingot",
            new ItemStack(ItemType.IRON_INGOT),
            stoneAge, "task", true, false
        );
        register(ironAge);

        Advancement diamond = new Advancement(
            "minecraft:story/diamonds",
            "Diamonds!",
            "Acquire diamonds",
            new ItemStack(ItemType.DIAMOND),
            ironAge, "task", true, false
        );
        register(diamond);
    }

    public void register(Advancement advancement) {
        advancements.put(advancement.getId(), advancement);
        if (advancement.getParent() != null) {
            advancement.getParent().addChild(advancement);
        }
    }

    public Advancement getAdvancement(String id) {
        return advancements.get(id);
    }

    public Collection<Advancement> getAdvancements() {
        return advancements.values();
    }

    public void grantAdvancement(PlayerEntity player, String advancementId) {
        Advancement adv = advancements.get(advancementId);
        if (adv == null) return;

        Set<String> progress = playerProgress.computeIfAbsent(player.getUuid(), k -> ConcurrentHashMap.newKeySet());
        progress.add(advancementId);
        player.sendMessage("{\"text\":\"§aAdvancement Made: " + adv.getTitle() + "\"}");
    }

    public boolean hasAdvancement(PlayerEntity player, String advancementId) {
        Set<String> progress = playerProgress.get(player.getUuid());
        return progress != null && progress.contains(advancementId);
    }

    public Set<String> getPlayerAdvancements(PlayerEntity player) {
        return playerProgress.getOrDefault(player.getUuid(), Collections.emptySet());
    }
}
