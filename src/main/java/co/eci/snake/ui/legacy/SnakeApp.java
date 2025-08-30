package co.eci.snake.ui.legacy;

import co.eci.snake.concurrency.SnakeRunner;
import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Position;
import co.eci.snake.core.Snake;
import co.eci.snake.core.engine.GameClock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class SnakeApp {
    private final JFrame frame;
    private final GamePanel gamePanel;
    private final JButton actionButton = new JButton("Pause");
    private final Board board;
    private final List<Snake> snakes;
    private final GameClock clock;
    private volatile boolean paused = false;

    public SnakeApp() {
        int width = Integer.getInteger("board.width", 60);
        int height = Integer.getInteger("board.height", 36);
        this.board = new Board(width, height);

        int n = Integer.getInteger("snakes", 2);
        this.snakes = new ArrayList<>(n);
        // Place snakes spaced across the board
        for (int i=0; i<n; i++) {
            int x = (i * (width / Math.max(1, n))) % Math.max(1, width-1) + 1;
            int y = (i * 3) % Math.max(1, height-1) + 1;
            Direction d = Direction.values()[i % Direction.values().length];
            snakes.add(Snake.of(x, y, d));
        }

        this.gamePanel = new GamePanel(board, snakes);
        this.frame = new JFrame("SnakeRace — ARSW");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(gamePanel, BorderLayout.CENTER);
        frame.add(actionButton, BorderLayout.SOUTH);
        frame.setSize(new Dimension(900, 580));
        frame.setLocationRelativeTo(null);

        // Clock ticks repaint (EDT-safe)
        this.clock = new GameClock(Long.getLong("tick.ms", 40L), () -> SwingUtilities.invokeLater(gamePanel::repaint));
        clock.start();

        var exec = Executors.newVirtualThreadPerTaskExecutor();
        snakes.forEach(s -> exec.submit(new SnakeRunner(s, board, clock)));

        actionButton.addActionListener((ActionEvent e) -> togglePause());

        gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "pause");
        gamePanel.getActionMap().put("pause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { togglePause(); }
        });

        // Player controls for first two snakes if present
        if (snakes.size() > 0) {
            InputMap im = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = gamePanel.getActionMap();
            im.put(KeyStroke.getKeyStroke("UP"), "p1_up");
            im.put(KeyStroke.getKeyStroke("DOWN"), "p1_down");
            im.put(KeyStroke.getKeyStroke("LEFT"), "p1_left");
            im.put(KeyStroke.getKeyStroke("RIGHT"), "p1_right");
            am.put("p1_up", new TurnAction(snakes.get(0), Direction.UP));
            am.put("p1_down", new TurnAction(snakes.get(0), Direction.DOWN));
            am.put("p1_left", new TurnAction(snakes.get(0), Direction.LEFT));
            am.put("p1_right", new TurnAction(snakes.get(0), Direction.RIGHT));
        }
        if (snakes.size() > 1) {
            InputMap im = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = gamePanel.getActionMap();
            im.put(KeyStroke.getKeyStroke('W'), "p2_up");
            im.put(KeyStroke.getKeyStroke('S'), "p2_down");
            im.put(KeyStroke.getKeyStroke('A'), "p2_left");
            im.put(KeyStroke.getKeyStroke('D'), "p2_right");
            am.put("p2_up", new TurnAction(snakes.get(1), Direction.UP));
            am.put("p2_down", new TurnAction(snakes.get(1), Direction.DOWN));
            am.put("p2_left", new TurnAction(snakes.get(1), Direction.LEFT));
            am.put("p2_right", new TurnAction(snakes.get(1), Direction.RIGHT));
        }

        frame.setVisible(true);
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            actionButton.setText("Resume");
            clock.pause();
        } else {
            actionButton.setText("Pause");
            clock.resume();
        }
    }

    private static final class TurnAction extends AbstractAction {
        private final Snake snake;
        private final Direction dir;
        private TurnAction(Snake s, Direction d){ this.snake = s; this.dir = d; }
        @Override public void actionPerformed(ActionEvent e) { snake.turn(dir); }
    }

    private static final class GamePanel extends JPanel {
        private final Board board;
        private final List<Snake> snakes;
        private final int cell = Integer.getInteger("cell.size", 16);

        private GamePanel(Board board, List<Snake> snakes) {
            this.board = board;
            this.snakes = snakes;
            setBackground(Color.BLACK);
            setFocusable(true);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = board.width();
            int h = board.height();

            // Draw grid background
            g2.setColor(new Color(30, 30, 30));
            for (int x=0; x<w; x++) g2.drawLine(x*cell, 0, x*cell, h*cell);
            for (int y=0; y<h; y++) g2.drawLine(0, y*cell, w*cell, y*cell);

            // Draw board elements from snapshots to avoid CME
            for (Position p : board.obstaclesSnapshot()) {
                g2.setColor(new Color(180, 60, 60));
                g2.fillRect(p.x()*cell, p.y()*cell, cell, cell);
            }
            for (Position p : board.miceSnapshot()) {
                g2.setColor(new Color(80, 200, 80));
                g2.fillOval(p.x()*cell + cell/4, p.y()*cell + cell/4, cell/2, cell/2);
            }
            for (Position p : board.turboSnapshot()) {
                g2.setColor(new Color(240, 240, 90));
                g2.fillRect(p.x()*cell+2, p.y()*cell+2, cell-4, cell-4);
            }
            for (Map.Entry<Position,Position> e : board.teleportsSnapshot().entrySet()) {
                Position p = e.getKey();
                g2.setColor(new Color(200, 80, 220));
                g2.drawRect(p.x()*cell+2, p.y()*cell+2, cell-4, cell-4);
            }

            // Draw snakes using per-snake snapshot
            int idx = 0;
            for (Snake s : snakes) {
                var parts = s.snapshot();
                if (parts.isEmpty()) continue;
                // Head
                g2.setColor(new Color(90, 160, 230));
                Position head = parts.get(0);
                g2.fillRect(head.x()*cell+1, head.y()*cell+1, cell-2, cell-2);
                // Body
                g2.setColor(new Color(70, 120, 180));
                for (int i=1;i<parts.size();i++) {
                    Position p = parts.get(i);
                    g2.fillRect(p.x()*cell+2, p.y()*cell+2, cell-4, cell-4);
                }
                idx++;
            }
            g2.dispose();
        }
    }

    public static void launch() {
        SwingUtilities.invokeLater(SnakeApp::new);
    }
}

