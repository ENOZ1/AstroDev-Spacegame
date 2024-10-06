import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class MothershipMayhemGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private boolean titleScreen = true;
    private boolean instructionScreen = false; // Tracks if instructions screen is active
    private boolean gameWon = false, gameLost = false;
    private boolean mothershipExists = false;
    private int currentLevel = 1;
    private final int maxLevel = 5; // Total number of levels
    private int score = 0;
    private int jetX, jetY;
    private final int jetSpeed = 20; // Jet movement speed
    private final int alienSpeed = 2; // Alien movement speed
    private int mothershipHits = 0;
    private int hearts = 5; // Player's lives

    private ArrayList<Shot> shots = new ArrayList<>();
    private ArrayList<Alien> aliens = new ArrayList<>();
    private ArrayList<Shot> mothershipShots = new ArrayList<>(); // Mothership's projectiles

    // Mothership properties
    private int mothershipX = 0;
    private int mothershipDirection = 1; // 1 for right, -1 for left
    private final int mothershipWidth = 370;
    private final int mothershipHeight = 270;
    private final int mothershipY = 50;

    // Load images
    private final Image jetImage = new ImageIcon("gifs/spaceship-ezgif.com-video-to-gif-converter.gif").getImage();
    private final Image alienImage = new ImageIcon("gifs/WhatsAppVideo2024-09-19at10.36.47-ezgif.com-video-to-gif-converter.gif").getImage();
    private final Image mothershipImage = new ImageIcon("gifs/mothership.gif").getImage();
    private final Image shotImage = new ImageIcon("gifs/shooter-ezgif.com-video-to-gif-converter.gif").getImage();
    private final Image backgroundImage = new ImageIcon("pictures/WhatsApp Image 2024-09-14 at 09.01.48.jpeg").getImage(); // Background photo
    private final Image heartImage = new ImageIcon("pictures/fp6koryn.png").getImage(); // Heart image

    // Sound clips
    private Clip explosionClip;
    private Clip shootClip;
    private Clip winClip;
    private Clip loseClip;
    private Clip backgroundMusicClip;

    // Constants for screen dimensions
    private int SCREEN_WIDTH;
    private int SCREEN_HEIGHT;

    // Constants for jet dimensions
    private final int JET_WIDTH = 120;
    private final int JET_HEIGHT = 120;

    // Constants for alien dimensions
    private final int ALIEN_WIDTH = 100;
    private final int ALIEN_HEIGHT = 100;

    public MothershipMayhemGame() {
        setFocusable(true);
        addKeyListener(this);
        initializeSounds();
        initializeScreenDimensions();
        initializePositions();
        spawnAliens(currentLevel);
        playBackgroundMusic();
        timer = new Timer(16, this); // Approximately 60 FPS
        timer.start();
    }

    // Initialize screen dimensions based on the display
    private void initializeScreenDimensions() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        SCREEN_WIDTH = screenSize.width;
        SCREEN_HEIGHT = screenSize.height;
    }

    private void initializeSounds() {
        try {
            // Load explosion sound
            explosionClip = AudioSystem.getClip();
            explosionClip.open(AudioSystem.getAudioInputStream(new File("sounds/Voicy_Explosion (online-audio-converter.com).wav")));

            // Load shoot sound
            shootClip = AudioSystem.getClip();
            shootClip.open(AudioSystem.getAudioInputStream(new File("sounds/ambience-launch-of-two-model-rockets-one-small-and-one-larger-243895.wav")));

            // Load win sound
            winClip = AudioSystem.getClip();
            winClip.open(AudioSystem.getAudioInputStream(new File("sounds/you-win-sequence-2-183949.wav")));

            // Load lose sound
            loseClip = AudioSystem.getClip();
            loseClip.open(AudioSystem.getAudioInputStream(new File("sounds/8-bit-video-game-lose-sound-version-1-145828.wav")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializePositions() {
        // Initialize jet position based on screen size
        jetX = (SCREEN_WIDTH - JET_WIDTH) / 2; // Centered horizontally
        jetY = SCREEN_HEIGHT - JET_HEIGHT - 30; // Positioned 30 pixels from the bottom
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background
        g.drawImage(backgroundImage, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, this);

        if (titleScreen) {
            // Title
            g.setFont(new Font("Arial", Font.BOLD, 100));
            FontMetrics fm = g.getFontMetrics();
            String title = "Mothership Mayhem";
            int titleX = (SCREEN_WIDTH - fm.stringWidth(title)) / 2;
            int titleY = SCREEN_HEIGHT / 2 - 100;
            g.setColor(Color.WHITE);
            g.drawString(title, titleX, titleY);

            // Press Enter to Start
            g.setFont(new Font("Arial", Font.PLAIN, 50));
            String prompt = "Press Enter to Start";
            int promptX = (SCREEN_WIDTH - fm.stringWidth(prompt)) / 2;
            int promptY = SCREEN_HEIGHT / 2;
            g.drawString(prompt, promptX, promptY);

            // Press 'I' for Instructions (Centered)
            String instructionsPrompt = "Press 'I' then 'Enter' for Instructions";
            int instructionsPromptX = (SCREEN_WIDTH - fm.stringWidth(instructionsPrompt)) / 2 + 380;
            int instructionsPromptY = SCREEN_HEIGHT / 2 + 100;
            g.drawString(instructionsPrompt, instructionsPromptX, instructionsPromptY);

        } else if (instructionScreen) {
            // Instructions Screen
            g.setFont(new Font("Arial", Font.BOLD, 80));
            g.setColor(Color.CYAN);
            String instructionsTitle = "Instructions";
            FontMetrics fmTitle = g.getFontMetrics();
            int titleX = (SCREEN_WIDTH - fmTitle.stringWidth(instructionsTitle)) / 2;
            int titleY = 100;
            g.drawString(instructionsTitle, titleX, titleY);

            g.setFont(new Font("Arial", Font.PLAIN, 40));
            g.setColor(Color.WHITE);
            int lineHeight = 50;
            int startY = 200;
            g.drawString("1. Move the jet with LEFT and RIGHT arrow keys.", 100, startY);
            g.drawString("2. Shoot with SPACE to destroy aliens.", 100, startY + lineHeight);
            g.drawString("3. Avoid the mothership projectiles.", 100, startY + 2 * lineHeight);
            g.drawString("4. Destroy the mothership with 30 hits.", 100, startY + 3 * lineHeight);
            g.drawString("5. Survive to win the game.", 100, startY + 4 * lineHeight);
            g.drawString("Press Backspace to Return and Esc to exit the game", 100, startY + 6 * lineHeight);

        } else if (gameWon || gameLost) {
            // Game Over or Win Screen
            g.setFont(new Font("Arial", Font.PLAIN, 80));
            FontMetrics fm = g.getFontMetrics();
            String message = gameWon ? "You Win!" : "You Lose!";
            int messageX = (SCREEN_WIDTH - fm.stringWidth(message)) / 2;
            int messageY = SCREEN_HEIGHT / 2 - 50;
            g.setColor(Color.YELLOW);
            g.drawString(message, messageX, messageY);

            // Press Backspace to Play Again
            String restartPrompt = "Press Backspace to Play Again or Esc to exit";
            g.setFont(new Font("Arial", Font.PLAIN, 50));
            fm = g.getFontMetrics();
            int promptX = (SCREEN_WIDTH - fm.stringWidth(restartPrompt)) / 2;
            int promptY = SCREEN_HEIGHT / 2 + 50;
            g.setColor(Color.WHITE);
            g.drawString(restartPrompt, promptX, promptY);
        } else {
            // Game Elements
            // Draw jet
            jetY = SCREEN_HEIGHT - JET_HEIGHT - 30; // Position jet 30 pixels from bottom
            g.drawImage(jetImage, jetX, jetY, JET_WIDTH, JET_HEIGHT, this); // Draw jet

            // Draw Aliens
            for (Alien alien : aliens) {
                g.drawImage(alienImage, alien.getX(), alien.getY(), ALIEN_WIDTH, ALIEN_HEIGHT, this);
            }

            // Draw Player's Shots
            for (Shot shot : shots) {
                if (!shot.isMothershipShot()) {
                    g.drawImage(shotImage, shot.getX(), shot.getY(), 20, 60, this);
                }
            }

            // Draw Mothership and its Shots
            if (mothershipExists) {
                g.drawImage(mothershipImage, mothershipX, mothershipY, mothershipWidth, mothershipHeight, this);
                for (Shot mothershipShot : mothershipShots) {
                    g.drawImage(shotImage, mothershipShot.getX(), mothershipShot.getY(), 20, 60, this);
                }
            }

            // Draw Score
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 40));
            g.drawString("Score: " + score, 20, 50);

            // Draw Hearts (Lives)
            for (int i = 0; i < hearts; i++) {
                g.drawImage(heartImage, 20 + i * 50, 60, 40, 40, this); // Draw heart images
            }
        }
    }

    // Game Loop and Logic
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!titleScreen && !instructionScreen && !gameWon && !gameLost) {
            updateAliens();
            updateShots();
            if (mothershipExists) {
                updateMothership();
                updateMothershipShots();
            }
            checkCollisions();
            checkLevelCompletion();
        }

        repaint(); // Redraw the screen
    }

    // Update Alien Positions
    public void updateAliens() {
        Iterator<Alien> alienIterator = aliens.iterator();
        while (alienIterator.hasNext()) {
            Alien alien = alienIterator.next();
            alien.moveDown(alienSpeed); // Move alien downwards
            if (alien.getY() > SCREEN_HEIGHT) {
                hearts--; // Lose a heart if alien passes the screen
                alienIterator.remove(); // Remove alien
                if (hearts <= 0) {
                    gameLost = true;
                    playSound(loseClip);
                }
            }
        }
    }

    // Update Shots Positions
    public void updateShots() {
        // Update Player's Shots
        Iterator<Shot> shotIterator = shots.iterator();
        while (shotIterator.hasNext()) {
            Shot shot = shotIterator.next();
            shot.moveUp(); // Move shot upwards
            if (shot.getY() < 0) {
                shotIterator.remove(); // Remove shot if it goes off-screen
            }
        }

        // Update Mothership's Shots
        Iterator<Shot> mothershipShotIterator = mothershipShots.iterator();
        while (mothershipShotIterator.hasNext()) {
            Shot mothershipShot = mothershipShotIterator.next();
            mothershipShot.moveDown(alienSpeed); // Move shot downwards
            if (mothershipShot.getY() > SCREEN_HEIGHT) {
                mothershipShotIterator.remove(); // Remove shot if it goes off-screen
            }
        }
    }

    // Update Mothership Position and Shooting
    public void updateMothership() {
        mothershipX += mothershipDirection * alienSpeed; // Move mothership

        // Change direction at screen edges
        if (mothershipX <= 0 || mothershipX + mothershipWidth >= SCREEN_WIDTH) {
            mothershipDirection *= -1;
        }

        // Random chance to shoot
        if (Math.random() < 0.05) {
            mothershipShots.add(new Shot(mothershipX + mothershipWidth / 2 - 10, mothershipY + mothershipHeight, true));
        }
    }

    // Update Mothership's Shots
    public void updateMothershipShots() {
        Iterator<Shot> shotIterator = mothershipShots.iterator();
        while (shotIterator.hasNext()) {
            Shot shot = shotIterator.next();
            shot.moveDown(alienSpeed);
            if (shot.getY() > SCREEN_HEIGHT) {
                shotIterator.remove();
            }
        }
    }

    // Collision Detection
    public void checkCollisions() {
        // Player's shots vs Aliens
        Iterator<Shot> shotIterator = shots.iterator();
        while (shotIterator.hasNext()) {
            Shot shot = shotIterator.next();
            Iterator<Alien> alienIterator = aliens.iterator();
            while (alienIterator.hasNext()) {
                Alien alien = alienIterator.next();
                if (shot.getBounds().intersects(alien.getBounds())) {
                    alienIterator.remove();
                    shotIterator.remove();
                    score += 10;
                    playSound(explosionClip);
                    break;
                }
            }
        }

        // Player's shots vs Mothership
        if (mothershipExists) {
            shotIterator = shots.iterator();
            while (shotIterator.hasNext()) {
                Shot shot = shotIterator.next();
                Rectangle mothershipRect = new Rectangle(mothershipX, mothershipY, mothershipWidth, mothershipHeight);
                if (shot.getBounds().intersects(mothershipRect)) {
                    mothershipHits++;
                    shotIterator.remove();
                    playSound(explosionClip);
                    if (mothershipHits >= 30) {
                        mothershipExists = false;
                        score += 100; // Bonus for defeating mothership
                    }
                }
            }

            // Mothership's shots vs Player
            Iterator<Shot> mothershipShotIterator = mothershipShots.iterator();
            Rectangle jetRect = new Rectangle(jetX, jetY, JET_WIDTH, JET_HEIGHT);
            while (mothershipShotIterator.hasNext()) {
                Shot shot = mothershipShotIterator.next();
                if (shot.getBounds().intersects(jetRect)) {
                    mothershipShotIterator.remove();
                    hearts--; // Lose a heart
                    playSound(explosionClip);
                    if (hearts <= 0) {
                        gameLost = true;
                        playSound(loseClip);
                    }
                }
            }
        }
    }

    // Check if Level is Completed
    public void checkLevelCompletion() {
        if (aliens.isEmpty() && !mothershipExists) {
            if (currentLevel < maxLevel) {
                currentLevel++;
                spawnAliens(currentLevel);
            } else {
                gameWon = true;
                playSound(winClip);
            }
        }
    }

    // Spawn Aliens Based on Level
    public void spawnAliens(int level) {
        aliens.clear(); // Clear existing aliens

        int alienSize = ALIEN_WIDTH; // Alien size

        switch (level) {
            case 1:
                // Level 1: 1 alien centered
                int x1 = (SCREEN_WIDTH - alienSize) / 2;
                int y1 = 50;
                aliens.add(new Alien(x1, y1));
                break;
            case 2:
                // Level 2: 2 aliens from different sides
                int x2a = clamp(100, 0, SCREEN_WIDTH - alienSize);
                int x2b = clamp(SCREEN_WIDTH - alienSize - 100, 0, SCREEN_WIDTH - alienSize);
                int y2 = 50;
                aliens.add(new Alien(x2a, y2));
                aliens.add(new Alien(x2b, y2));
                break;
            case 3:
                // Level 3: 3 aliens from different positions
                int x3a = clamp(100, 0, SCREEN_WIDTH - alienSize);
                int x3b = clamp((SCREEN_WIDTH - alienSize) / 2, 0, SCREEN_WIDTH - alienSize);
                int x3c = clamp(SCREEN_WIDTH - alienSize - 100, 0, SCREEN_WIDTH - alienSize);
                int y3 = 50;
                aliens.add(new Alien(x3a, y3));
                aliens.add(new Alien(x3b, y3));
                aliens.add(new Alien(x3c, y3));
                break;
            case 4:
                // Level 4: 4 aliens from various positions
                int x4a = clamp(50, 0, SCREEN_WIDTH - alienSize);
                int x4b = clamp((SCREEN_WIDTH - alienSize) / 3, 0, SCREEN_WIDTH - alienSize);
                int x4c = clamp(2 * (SCREEN_WIDTH - alienSize) / 3, 0, SCREEN_WIDTH - alienSize);
                int x4d = clamp(SCREEN_WIDTH - alienSize - 50, 0, SCREEN_WIDTH - alienSize);
                int y4 = 50;
                aliens.add(new Alien(x4a, y4));
                aliens.add(new Alien(x4b, y4));
                aliens.add(new Alien(x4c, y4));
                aliens.add(new Alien(x4d, y4));
                break;
            case 5:
                // Level 5: Spawn Mothership
                mothershipExists = true;
                // Initialize mothership position at center
                mothershipX = (SCREEN_WIDTH - mothershipWidth) / 2;
                break;
            default:
                break;
        }
    }

    // Helper method to clamp values within a range
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // Play Background Music
    private void playBackgroundMusic() {
        try {
            if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
                backgroundMusicClip.stop(); // Stop previous background music
                backgroundMusicClip.close(); // Release resources
            }
            backgroundMusicClip = AudioSystem.getClip();
            backgroundMusicClip.open(AudioSystem.getAudioInputStream(new File("sounds/space-adventure-29296 (online-audio-converter.com).wav")));
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY); // Loop continuously
            backgroundMusicClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Play Specific Sound
    private void playSound(Clip clip) {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0); // Rewind to start
            clip.start();
        }
    }

    // Key Events Handling
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (titleScreen) {
            if (key == KeyEvent.VK_ENTER) {
                titleScreen = false;
            } else if (key == KeyEvent.VK_I) { // Press 'I' for Instructions
                instructionScreen = true;
            }
        } else if (instructionScreen) {
            if (key == KeyEvent.VK_BACK_SPACE) { // Press Backspace to Return
                instructionScreen = false;
            }
        } else if (!instructionScreen && !gameWon && !gameLost) {
            // Gameplay Controls
            if (key == KeyEvent.VK_LEFT) {
                jetX -= jetSpeed;
                // Boundary check: Prevent jet from moving beyond the left edge
                if (jetX < 0) {
                    jetX = 0;
                }
            } else if (key == KeyEvent.VK_RIGHT) {
                jetX += jetSpeed;
                // Boundary check: Prevent jet from moving beyond the right edge
                if (jetX > SCREEN_WIDTH - JET_WIDTH) {
                    jetX = SCREEN_WIDTH - JET_WIDTH;
                }
            } else if (key == KeyEvent.VK_SPACE) {
                shots.add(new Shot(jetX + JET_WIDTH / 2 - 10, jetY, false)); // Fire a shot from center of the jet
                playSound(shootClip);
            }
        }

        if ((gameWon || gameLost) && key == KeyEvent.VK_BACK_SPACE) {
            // Restart the game
            resetGame();
        }

        // Exit Strategy: Press Esc to Exit the Game
        if (key == KeyEvent.VK_ESCAPE) {
            // Stop all sounds before exiting
            stopAllSounds();
            System.exit(0);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No action needed on key release
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No action needed on key typed
    }

    // Reset the Game to Initial State
    private void resetGame() {
        titleScreen = true;
        instructionScreen = false;
        gameWon = false;
        gameLost = false;
        score = 0;
        currentLevel = 1;
        hearts = 5;
        mothershipHits = 0;
        mothershipExists = false;
        initializePositions();
        spawnAliens(currentLevel);
        shots.clear();
        mothershipShots.clear();
        playBackgroundMusic();
    }

    // Stop all sounds when exiting
    private void stopAllSounds() {
        try {
            if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
                backgroundMusicClip.stop();
                backgroundMusicClip.close();
            }
            if (explosionClip != null && explosionClip.isRunning()) {
                explosionClip.stop();
                explosionClip.close();
            }
            if (shootClip != null && shootClip.isRunning()) {
                shootClip.stop();
                shootClip.close();
            }
            if (winClip != null && winClip.isRunning()) {
                winClip.stop();
                winClip.close();
            }
            if (loseClip != null && loseClip.isRunning()) {
                loseClip.stop();
                loseClip.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Main Method to Launch the Game
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Mothership Mayhem");
            MothershipMayhemGame gamePanel = new MothershipMayhemGame();
            frame.add(gamePanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false); // Prevent window resizing to maintain boundary checks

            // Set to full-screen mode
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setUndecorated(true);
            frame.setVisible(true);

            // Ensure the game panel takes the full screen
            gamePanel.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
            frame.pack();
        });
    }
}

// Alien Class
class Alien {
    private int x, y;
    private final int width = 100;
    private final int height = 100;

    public Alien(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void moveDown(int speed) {
        y += speed; // Move down vertically
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height); // Hitbox for collision detection
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

// Shot Class
class Shot {
    private int x, y;
    private final boolean mothershipShot;
    private final int width = 20;
    private final int height = 60;

    public Shot(int x, int y, boolean mothershipShot) {
        this.x = x;
        this.y = y;
        this.mothershipShot = mothershipShot;
    }

    public void moveUp() {
        y -= 10; // Player's shots move upwards
    }

    public void moveDown(int speed) {
        y += speed; // Mothership's shots move downwards
    }

    public boolean isMothershipShot() {
        return mothershipShot;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height); // Hitbox for collision detection
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
