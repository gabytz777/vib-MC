package net.vibmc.advancement;

import net.vibmc.item.ItemStack;

import java.util.*;

public class Advancement {
    private final String id;
    private final String title;
    private final String description;
    private final ItemStack icon;
    private final Advancement parent;
    private final String frame;
    private final boolean toast;
    private final boolean hidden;
    private final Map<String, Object> criteria;
    private final Set<Advancement> children;

    public Advancement(String id, String title, String description, ItemStack icon,
                       Advancement parent, String frame, boolean toast, boolean hidden) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.parent = parent;
        this.frame = frame;
        this.toast = toast;
        this.hidden = hidden;
        this.criteria = new LinkedHashMap<>();
        this.children = new HashSet<>();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ItemStack getIcon() { return icon; }
    public Advancement getParent() { return parent; }
    public String getFrame() { return frame; }
    public boolean isToast() { return toast; }
    public boolean isHidden() { return hidden; }

    public void addCriterion(String name, Object condition) {
        criteria.put(name, condition);
    }

    public Map<String, Object> getCriteria() { return criteria; }
    public Set<Advancement> getChildren() { return children; }
    void addChild(Advancement child) { children.add(child); }
}
