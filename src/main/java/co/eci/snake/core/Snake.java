package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class Snake {
    private final Deque<Position> body = new ArrayDeque<>();
    private volatile Direction direction;
    private volatile boolean alive = true;
    private int maxLength = 5;

    private Snake(Position start, Direction dir) {
        body.addFirst(start);
        this.direction = dir;
    }

    public static Snake of(int x, int y, Direction dir) {
        return new Snake(new Position(x,y), dir);
    }

    public boolean isAlive() { return alive; }
    public void kill() { alive = false; }

    public Direction direction() { return direction; }

    public void turn(Direction d) {
        // No-op if opposite immediate reversal is undesired; allow any for simplicity
        this.direction = d;
    }

    public synchronized Position head() {
        return body.peekFirst();
    }

    public synchronized List<Position> snapshot() {
        return new ArrayList<>(body);
    }

    /** Advance head to newHead; if grow=true, increase length, else maintain maxLength. */
    public synchronized void advance(Position newHead, boolean grow) {
        if (!alive) return;
        body.addFirst(newHead);
        if (grow) maxLength++;
        while (body.size() > maxLength) body.removeLast();
    }

    /** Simple length API for stats/paint order (optional). */
    public synchronized int length() {
        return body.size();
    }
}
