package net.vibmc.entity;

import java.util.ArrayList;
import java.util.List;

public class EntityManager {
    private final List<Entity> entities = new ArrayList<>();

    public void add(Entity entity) {
        entities.add(entity);
    }

    public void tick() {
        for (Entity entity : new ArrayList<>(entities)) {
            entity.tick();
            if (entity.isDead()) {
                entities.remove(entity);
            }
        }
    }

    public List<Entity> entities() {
        return List.copyOf(entities);
    }
}
