package co.eci.snake.concurrency;

import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Snake;
import co.eci.snake.core.engine.GameClock;

import java.util.concurrent.ThreadLocalRandom;

public final class SnakeRunner implements Runnable {
    private final Snake snake;
    private final Board board;
    private final GameClock clock;

    private final int baseSleepMs;
    private final int turboSleepMs;
    private int turboTicks = 0;

    public SnakeRunner(Snake snake, Board board, GameClock clock) {
        this.snake = snake;
        this.board = board;
        this.clock = clock;
        this.baseSleepMs = Integer.getInteger("snake.baseSleepMs", 80);
        this.turboSleepMs = Integer.getInteger("snake.turboSleepMs", 40);
    }

    @Override public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && snake.isAlive()) {
                clock.waitIfPaused();

                maybeTurn();
                Board.MoveResult res = board.step(snake);
                if (res == Board.MoveResult.HIT_OBSTACLE) {
                    randomTurn();
                } else if (res == Board.MoveResult.ATE_TURBO) {
                    turboTicks = Math.min(200, turboTicks + 60);
                }

                int sleep = (turboTicks > 0) ? turboSleepMs : baseSleepMs;
                if (turboTicks > 0) turboTicks--;
                Thread.sleep(sleep);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            // Prevent task from silently dying; in a real app you'd log this
            Thread.currentThread().interrupt();
        }
    }

    private void maybeTurn() {
        double p = (turboTicks > 0) ? 0.05 : 0.10;
        if (ThreadLocalRandom.current().nextDouble() < p) randomTurn();
    }

    private void randomTurn() {
        Direction[] dirs = Direction.values();
        snake.turn(dirs[ThreadLocalRandom.current().nextInt(dirs.length)]);
    }
}
