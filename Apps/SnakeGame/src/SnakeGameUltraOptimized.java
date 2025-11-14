import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import javax.sound.sampled.*;

public class SnakeGameUltraOptimized extends JPanel implements ActionListener {

    // --- CONFIGURAÇÕES ---
    private static final int SCREEN_WIDTH = 600;
    private static final int SCREEN_HEIGHT = 600;
    private static final int UNIT_SIZE = 25;
    private static final int GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);
    private int DELAY = 120;

    private final int[] x = new int[GAME_UNITS];
    private final int[] y = new int[GAME_UNITS];

    private int bodyParts = 6;
    private int applesEaten = 0;
    private int highscore = 0;

    private int appleX;
    private int appleY;

    private char direction = 'R';
    private boolean running = false;
    private boolean paused = false;
    private boolean showGrid = false;

    private Timer timer;
    private Random random = new Random();
    private File scoreFile = new File("highscore.txt");

    // Sons
    private Clip eatSound;
    private Clip gameOverSound;

    public SnakeGameUltraOptimized() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new MyKeyAdapter());
        setDoubleBuffered(true);

        loadSounds();
        loadHighscore();
    }

    // Carregar sons WAV
    private void loadSounds() {
        try {
            eatSound = AudioSystem.getClip();
            eatSound.open(AudioSystem.getAudioInputStream(
                    getClass().getResource("eat.wav")));

            gameOverSound = AudioSystem.getClip();
            gameOverSound.open(AudioSystem.getAudioInputStream(
                    getClass().getResource("gameover.wav")));

        } catch (Exception e) {
            System.out.println("Sons não carregados (usa WAV no mesmo diretório)");
        }
    }

    public void startGame() {
        newApple();
        bodyParts = 6;
        applesEaten = 0;
        direction = 'R';
        running = true;
        paused = false;

        if (timer != null) timer.stop();
        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void loadHighscore() {
        try {
            if (scoreFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(scoreFile));
                highscore = Integer.parseInt(br.readLine());
                br.close();
            }
        } catch (Exception ignored) {}
    }

    private void saveHighscore() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(scoreFile));
            bw.write("" + highscore);
            bw.close();
        } catch (Exception ignored) {}
    }

    private void newApple() {
        appleX = random.nextInt(SCREEN_WIDTH / UNIT_SIZE) * UNIT_SIZE;
        appleY = random.nextInt(SCREEN_HEIGHT / UNIT_SIZE) * UNIT_SIZE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (running) {
            draw(g);
        } else {
            gameOver(g);
        }
    }

    private void draw(Graphics g) {
        if (showGrid) {
            g.setColor(new Color(35, 35, 35));
            for (int i = 0; i < SCREEN_HEIGHT / UNIT_SIZE; i++) {
                g.drawLine(i * UNIT_SIZE, 0, i * UNIT_SIZE, SCREEN_HEIGHT);
                g.drawLine(0, i * UNIT_SIZE, SCREEN_WIDTH, i * UNIT_SIZE);
            }
        }

        // maçã
        g.setColor(Color.RED);
        g.fillOval(appleX, appleY, UNIT_SIZE, UNIT_SIZE);

        // cobra
        for (int i = 0; i < bodyParts; i++) {
            g.setColor(i == 0 ? Color.GREEN : new Color(0, 180, 0));
            g.fillRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
        }

        // Score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Pontos: " + applesEaten, 10, 20);
        g.drawString("Recorde: " + highscore, 10, 40);

        if (paused) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("PAUSADO", SCREEN_WIDTH / 2 - 100, SCREEN_HEIGHT / 2);
        }
    }

    private void move() {
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }

        switch (direction) {
            case 'U' -> y[0] -= UNIT_SIZE;
            case 'D' -> y[0] += UNIT_SIZE;
            case 'L' -> x[0] -= UNIT_SIZE;
            case 'R' -> x[0] += UNIT_SIZE;
        }
    }

    private void checkApple() {
        if (x[0] == appleX && y[0] == appleY) {
            bodyParts++;
            applesEaten++;

            if (eatSound != null) eatSound.start();

            DELAY = Math.max(40, DELAY - 3); // aceleração suave
            timer.setDelay(DELAY);

            newApple();
        }
    }

    private void checkCollisions() {
        // Corpo
        for (int i = 1; i < bodyParts; i++) {
            if (x[0] == x[i] && y[0] == y[i]) {
                running = false;
            }
        }

        // Bordas
        if (x[0] < 0 || x[0] >= SCREEN_WIDTH || y[0] < 0 || y[0] >= SCREEN_HEIGHT) {
            running = false;
        }

        if (!running) {
            timer.stop();
            if (gameOverSound != null) gameOverSound.start();

            if (applesEaten > highscore) {
                highscore = applesEaten;
                saveHighscore();
            }
        }
    }

    private void gameOver(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString("GAME OVER", 150, 250);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Pontos: " + applesEaten, 240, 300);
        g.drawString("Recorde: " + highscore, 230, 330);

        g.setColor(Color.YELLOW);
        g.drawString("Pressione R para reiniciar", 180, 380);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !paused) {
            move();
            checkApple();
            checkCollisions();
        }
        repaint();
    }

    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_LEFT && direction != 'R') direction = 'L';
            if (key == KeyEvent.VK_RIGHT && direction != 'L') direction = 'R';
            if (key == KeyEvent.VK_UP && direction != 'D') direction = 'U';
            if (key == KeyEvent.VK_DOWN && direction != 'U') direction = 'D';

            if (key == KeyEvent.VK_P) paused = !paused;
            if (key == KeyEvent.VK_R) startGame();
            if (key == KeyEvent.VK_G) showGrid = !showGrid;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Super Otimizado");
        SnakeGameUltraOptimized panel = new SnakeGameUltraOptimized();

        frame.add(panel);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        panel.startGame();
    }
}
