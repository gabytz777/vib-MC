package net.vibmc.entity.ai;

import net.vibmc.server.util.Position;
import net.vibmc.world.World;

import java.util.*;

public class Pathfinding {
    private List<Position> path;
    private int pathIndex;

    public Pathfinding() {
        this.path = new ArrayList<>();
        this.pathIndex = 0;
    }

    public List<Position> findPath(World world, double startX, double startY, double startZ,
                                    double targetX, double targetY, double targetZ, int maxSteps) {
        Set<Long> visited = new HashSet<>();
        Map<Long, Position> cameFrom = new HashMap<>();
        Map<Long, Double> gScore = new HashMap<>();
        PriorityQueue<Node> openSet = new PriorityQueue<>();

        Position start = new Position((int) startX, (int) startY, (int) startZ);
        Position target = new Position((int) targetX, (int) targetY, (int) targetZ);

        long startKey = start.toLong();
        gScore.put(startKey, 0.0);
        openSet.add(new Node(start, 0, heuristic(start, target)));
        visited.add(startKey);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxSteps) {
            Node current = openSet.poll();
            iterations++;

            if (current.pos.distance(target) <= 1.5) {
                return reconstructPath(cameFrom, current.pos);
            }

            for (Position neighbor : getNeighbors(current.pos)) {
                long neighborKey = neighbor.toLong();
                if (visited.contains(neighborKey)) continue;

                if (!isPassable(world, neighbor)) continue;

                double tentativeG = gScore.get(current.pos.toLong()) + 1.0;
                if (!gScore.containsKey(neighborKey) || tentativeG < gScore.get(neighborKey)) {
                    cameFrom.put(neighborKey, current.pos);
                    gScore.put(neighborKey, tentativeG);
                    double f = tentativeG + heuristic(neighbor, target);
                    openSet.add(new Node(neighbor, tentativeG, f));
                    visited.add(neighborKey);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<Position> getNeighbors(Position pos) {
        List<Position> neighbors = new ArrayList<>();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        neighbors.add(new Position(x + 1, y, z));
        neighbors.add(new Position(x - 1, y, z));
        neighbors.add(new Position(x, y, z + 1));
        neighbors.add(new Position(x, y, z - 1));
        neighbors.add(new Position(x + 1, y + 1, z));
        neighbors.add(new Position(x - 1, y + 1, z));
        neighbors.add(new Position(x, y + 1, z + 1));
        neighbors.add(new Position(x, y + 1, z - 1));
        neighbors.add(new Position(x + 1, y - 1, z));
        neighbors.add(new Position(x - 1, y - 1, z));
        neighbors.add(new Position(x, y - 1, z + 1));
        neighbors.add(new Position(x, y - 1, z - 1));
        return neighbors;
    }

    private boolean isPassable(World world, Position pos) {
        return world.getBlock(pos).isAir();
    }

    private double heuristic(Position a, Position b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) +
                         Math.pow(a.getY() - b.getY(), 2) +
                         Math.pow(a.getZ() - b.getZ(), 2));
    }

    private List<Position> reconstructPath(Map<Long, Position> cameFrom, Position current) {
        List<Position> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current.toLong())) {
            current = cameFrom.get(current.toLong());
            path.add(0, current);
        }
        return path;
    }

    public List<Position> getPath() { return path; }
    public int getPathIndex() { return pathIndex; }

    public Position getNextTarget() {
        if (pathIndex < path.size()) {
            return path.get(pathIndex);
        }
        return null;
    }

    public void advance() {
        if (pathIndex < path.size()) {
            pathIndex++;
        }
    }

    public boolean isPathComplete() {
        return pathIndex >= path.size();
    }

    private static class Node implements Comparable<Node> {
        final Position pos;
        final double g;
        final double f;

        Node(Position pos, double g, double f) {
            this.pos = pos;
            this.g = g;
            this.f = f;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }
    }
}
