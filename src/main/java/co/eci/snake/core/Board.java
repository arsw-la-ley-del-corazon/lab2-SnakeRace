package co.eci.snake.core;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Board {
    private final int width;
    private final int height;

    // Internal sets guarded by rwLock
    private final Set<Position> mice = new HashSet<>();
    private final Set<Position> obstacles = new HashSet<>();
    private final Set<Position> turbo = new HashSet<>();
    private final Map<Position, Position> teleports = new HashMap<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public enum MoveResult { MOVED, ATE_MOUSE, ATE_TURBO, HIT_OBSTACLE }

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        // Seed with some items
        for (int i=0;i<6;i++) addMouse();
        for (int i=0;i<4;i++) addObstacle(randomEmpty());
        for (int i=0;i<2;i++) addTurbo(randomEmpty());
    }

    public int width() { return width; }
    public int height() { return height; }

    /** Safe snapshots for UI rendering (avoid iterating live sets). */
    public List<Position> miceSnapshot() {
        rwLock.readLock().lock();
        try { return new ArrayList<>(mice); }
        finally { rwLock.readLock().unlock(); }
    }
    public List<Position> obstaclesSnapshot() {
        rwLock.readLock().lock();
        try { return new ArrayList<>(obstacles); }
        finally { rwLock.readLock().unlock(); }
    }
    public List<Position> turboSnapshot() {
        rwLock.readLock().lock();
        try { return new ArrayList<>(turbo); }
        finally { rwLock.readLock().unlock(); }
    }
    public Map<Position, Position> teleportsSnapshot() {
        rwLock.readLock().lock();
        try { return new HashMap<>(teleports); }
        finally { rwLock.readLock().unlock(); }
    }

    public void addTeleportPair(Position a, Position b) {
        rwLock.writeLock().lock();
        try {
            teleports.put(a, b);
            teleports.put(b, a);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void addObstacle(Position p) {
        rwLock.writeLock().lock();
        try { obstacles.add(p); } finally { rwLock.writeLock().unlock(); }
    }

    public void addMouse() {
        Position p = randomEmpty();
        rwLock.writeLock().lock();
        try { mice.add(p); } finally { rwLock.writeLock().unlock(); }
    }

    public void addTurbo(Position p) {
        rwLock.writeLock().lock();
        try { turbo.add(p); } finally { rwLock.writeLock().unlock(); }
    }

    /** Single-thread critical section: move 'snake' one step based on its current direction. */
    public MoveResult step(Snake snake) {
        rwLock.writeLock().lock();
        try {
            if (!snake.isAlive()) return MoveResult.MOVED;
            Position head = snake.head();
            Direction d = snake.direction();
            Position next = new Position(head.x() + d.dx, head.y() + d.dy).wrap(width, height);

            if (obstacles.contains(next)) {
                // Bounce is handled by caller (e.g., change direction); do not advance
                return MoveResult.HIT_OBSTACLE;
            }

            // Teleport if present
            if (teleports.containsKey(next)) {
                next = teleports.get(next);
            }

            boolean ateMouse = mice.remove(next);
            boolean ateTurbo = turbo.remove(next);

            snake.advance(next, ateMouse);

            if (ateMouse) {
                // Spawn a new obstacle elsewhere and a new mouse
                addObstacle(randomEmpty());
                addMouse();
                return MoveResult.ATE_MOUSE;
            }
            if (ateTurbo) {
                // Replenish turbo tile somewhere else
                addTurbo(randomEmpty());
                return MoveResult.ATE_TURBO;
            }
            return MoveResult.MOVED;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private Position randomEmpty() {
        var rnd = ThreadLocalRandom.current();
        // We only consider board objects; snakes are not tracked here—callers avoid overlaps at creation time.
        for (int guard=0; guard< width*height*3; guard++) {
            Position p = new Position(rnd.nextInt(width), rnd.nextInt(height));
            rwLock.readLock().lock();
            try {
                if (!mice.contains(p) && !obstacles.contains(p) && !turbo.contains(p) && !teleports.containsKey(p)) {
                    return p;
                }
            } finally {
                rwLock.readLock().unlock();
            }
        }
        // Fallback
        return new Position(0,0);
    }
}
