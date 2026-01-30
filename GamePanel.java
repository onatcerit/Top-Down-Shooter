import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import zombies.*;


public class GamePanel extends JPanel implements ActionListener, MouseMotionListener {

    private boolean inMainMenu = true;
    private boolean inHelpMenu = false;
    private String playerName = "Oyuncu";

    public static ArrayList<Obstacle> obstacles = new ArrayList<>();
    private String mapTheme = "Grassland";
    private Random rand = new Random();
    private ArrayList<Shape> grassPatches = new ArrayList<>();
    private ArrayList<Shape> dirtPaths = new ArrayList<>();
    private ArrayList<Shape> darkForestBushes = new ArrayList<>();
    private ArrayList<Shape> desertCracks = new ArrayList<>();
    private ArrayList<Shape> desertRocks = new ArrayList<>();

    private Timer timer;
    private ArrayList<Zombie> zombies;
    private ArrayList<Bullet> bullets;
    private ArrayList<BloodStain> bloodStains;

    public static int playerX = 300, playerY = 300;
    private int playerSpeed = 5;

    private Weapon[] weapons;
    private int currentWeaponIndex = 0;

    private int playerHealth = 100;
    private boolean gameOver = false;
    private boolean paused = false;

    private int currentWave = 1;
    private boolean waveInProgress = false;
    private int zombiesPerWave = 5;

    private int score = 0;

    private float timeOfDay = 0.0f;
    private float daySpeed = 0.0005f;

    private String weather = "Clear";
    private int weatherTimer = 0;
    private int weatherDuration = 1000;

    private ArrayList<Particle> rainParticles = new ArrayList<>();
    private ArrayList<Particle> snowParticles = new ArrayList<>();

    private int flashTimer = 0;

    private BufferedImage playerImg;
    private Point mousePosition = new Point(400, 300);

    private Clip menuMusic;
    private boolean menuMusicPlaying = false;
    private float glowAlpha = 0.5f;
    private boolean glowIncreasing = true;
    private int haloOffset = 0;

    private static final int MAX_WAVE = 13;
    private boolean gameWon = false;

    private ArrayList<ExplosionEffect> explosions = new ArrayList<>();

    public GamePanel() {
        setFocusable(true);
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        loadPlayerImage();
        addMouseMotionListener(this);

        zombies = new ArrayList<>();
        bullets = new ArrayList<>();
        bloodStains = new ArrayList<>();

        obstacles.add(new Obstacle(200, 150, 400, 20));
        obstacles.add(new Obstacle(200, 250, 20, 300));
        obstacles.add(new Obstacle(580, 250, 20, 300));

        mapTheme = "Grassland";
        generateMapDetails();

        weapons = new Weapon[] {
            new Weapon("Pistol", 120, 12, Integer.MAX_VALUE, 30),
            new Weapon("Rifle", 600, 30, 120, 20),
            new Weapon("Shotgun", 60, 5, 25, 50),
            new Weapon("Sniper", 30, 5, 15, 100),
            new Weapon("Rocket Launcher", 10, 1, 5, 200)
        };

        timer = new Timer(30, this);
        timer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (gameOver && key == KeyEvent.VK_ENTER) {
                    restartGame();
                    return;
                }
                if (gameOver && key == KeyEvent.VK_Q) {
                    System.exit(0);
                    return;
                }
                if (gameOver && key == KeyEvent.VK_M) {
                    inMainMenu = true;
                    gameOver = false;
                    return;
                }
                if (inMainMenu) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        inMainMenu = false;
                        restartGame();
                    }
                    if (key == KeyEvent.VK_H) {
                        inMainMenu = false;
                        inHelpMenu = true;
                        repaint();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_Q) {
                        System.exit(0);
                    }
                    repaint();
                    return;
                }
                if (inHelpMenu) {
                    if (key == KeyEvent.VK_B) { 
                        inHelpMenu = false;
                        inMainMenu = true;
                    }
                    repaint();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    paused = !paused;
                    repaint();
                }
                if (paused) {
                    if (e.getKeyCode() == KeyEvent.VK_K) {
                        saveGame();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_L) {
                        loadGame();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_Q) {
                        System.exit(0);
                    }
                }
        
                if (gameOver) return;

                if (key == KeyEvent.VK_W) {
                    playerY -= playerSpeed;
                }    
                if (key == KeyEvent.VK_S) {
                    playerY += playerSpeed;
                }
                if (key == KeyEvent.VK_A){ 
                    playerX -= playerSpeed;
                }    
                if (key == KeyEvent.VK_D){ 
                    playerX += playerSpeed;
                }    
                if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_5) {
                    checkWeaponAvailability(key);
                }
                if (key == KeyEvent.VK_R){ 
                    weapons[currentWeaponIndex].reload();
                }    
                if (key == KeyEvent.VK_M){
                    changeMap();
                }

                if (!collidesWithObstacle(playerX, playerY - playerSpeed)) { 
                    playerY -= playerSpeed;
                }
                if (!collidesWithObstacle(playerX, playerY + playerSpeed)) { 
                    playerY += playerSpeed;
                }
                if (!collidesWithObstacle(playerX - playerSpeed, playerY)) {
                    playerX -= playerSpeed;
                }
                if (!collidesWithObstacle(playerX + playerSpeed, playerY)){ 
                    playerX += playerSpeed;
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (paused || gameOver) {
                    return;
                }
                Weapon currentWeapon = weapons[currentWeaponIndex];
                
                if (currentWeapon.getName().equals("Rocket Launcher")) {
                    int targetX = e.getX();
                    int targetY = e.getY();
                
                    explosions.add(new ExplosionEffect(targetX, targetY));
                    playSound("sounds/explosion.wav");

                    for (Zombie z : new ArrayList<>(zombies)) {
                        if (new ExplosionEffect(targetX, targetY).getBounds().intersects(z.getBounds())) {
                            z.takeDamage(currentWeapon.getDamagePerShot());
                            if (z.isDead()) {
                                zombies.remove(z);
                                score += getZombieScore(z);
                                bloodStains.add(new BloodStain(z.getBounds().x, z.getBounds().y));
                                playSound("sounds/zombie_growl.wav");
                            }
                        }
                    }
                
                    return;
                }

                if (currentWeapon.canShoot()) {
                    int targetX = e.getX();
                    int targetY = e.getY();
                    bullets.add(new Bullet(playerX + 20, playerY + 20, targetX, targetY, currentWeapon.getDamagePerShot()));
                    currentWeapon.shoot();
                    createMuzzleFlash();
                    playSound("sounds/gunshot.wav");
                }
            }
        });
    }

    private void changeMap() {
        if (mapTheme.equals("Grassland")) {
            mapTheme = "DarkForest";
        }
        else if (mapTheme.equals("DarkForest")){ 
            mapTheme = "Desert";
        }
        else{ 
            mapTheme = "Grassland";
        }
        generateMapDetails();
        repaint();
    }

    private void drawMap(Graphics2D g2d) {
        switch (mapTheme) {
            case "Grassland":
                
                g2d.setColor(new Color(180, 255, 180));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                for (Shape s : grassPatches) {
                    int greenVariation = rand.nextInt(30);
                    g2d.setColor(new Color(34 + greenVariation, 139 + greenVariation, 34));
                    g2d.fill(s);
                    
                    g2d.setColor(new Color(0, 100, 0, 100));
                    g2d.draw(s);
                }
                for (Shape s : dirtPaths) {
                    g2d.setColor(new Color(194, 178, 128));
                    g2d.fill(s);
                    
                    g2d.setColor(new Color(160, 140, 90));
                    g2d.draw(s);
                }
                break;

            case "DarkForest":
                g2d.setColor(new Color(20, 40, 20));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                for (Shape s : darkForestBushes) {
                    g2d.setColor(new Color(0, 30, 0));
                    g2d.fill(s);
                    
                    g2d.setColor(new Color(0, 50, 0));
                    g2d.draw(s);
                    
                    Ellipse2D.Double bush = (Ellipse2D.Double) s;
                    int centerX = (int)(bush.x + bush.width/2);
                    int centerY = (int)(bush.y + bush.height/2);
                    int radius = (int)(bush.width/2);

                    for (int i = 0; i < 3; i++) {
                        int angle = rand.nextInt(360);
                        int length = radius - rand.nextInt(radius/2);
                        int endX = centerX + (int)(length * Math.cos(Math.toRadians(angle)));
                        int endY = centerY + (int)(length * Math.sin(Math.toRadians(angle)));
                        g2d.setColor(new Color(0, 40 + rand.nextInt(20), 0));
                        g2d.drawLine(centerX, centerY, endX, endY);
                    }
                }
                break;

            case "Desert":
                g2d.setColor(new Color(255, 240, 180));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                for (Shape s : desertCracks) {
                    g2d.setColor(new Color(220, 200, 160));
                    g2d.fill(s);
                    
                    g2d.setColor(new Color(200, 180, 140));
                    g2d.draw(s);
                }
                for (Shape s : desertRocks) {
                    g2d.setColor(new Color(180, 160, 120));
                    g2d.fill(s);

                    g2d.setColor(new Color(210, 190, 140));
                    g2d.draw(s);
                    
                    Rectangle2D.Double rock = (Rectangle2D.Double) s;
                    g2d.setColor(new Color(200, 180, 130));
                    g2d.drawLine((int)rock.x, (int)rock.y, 
                               (int)(rock.x + rock.width), (int)(rock.y + rock.height));
                }
                break;
        }
    }

    private void generateMapDetails() {
        grassPatches.clear();
        dirtPaths.clear();
        darkForestBushes.clear();
        desertCracks.clear();
        desertRocks.clear();

        switch (mapTheme) {
            case "Grassland":
                for (int i = 0; i < 200; i++) {
                    int x = rand.nextInt(800);
                    int y = rand.nextInt(600);
                    int size = rand.nextInt(8) + 4;
                    grassPatches.add(new Ellipse2D.Double(x, y, size, size));
                }
                for (int i = 0; i < 12; i++) {
                    int x = rand.nextInt(800);
                    int y = rand.nextInt(600);
                    int w = 80 + rand.nextInt(60);
                    int h = 10 + rand.nextInt(6);
                    dirtPaths.add(new Rectangle2D.Double(x, y, w, h));
                }
                break;

            case "DarkForest":
                for (int i = 0; i < 200; i++) {
                    int x = rand.nextInt(800);
                    int y = rand.nextInt(600);

                    int size = 8 + rand.nextInt(15);
                    darkForestBushes.add(new Ellipse2D.Double(x, y, size, size));
                }
                break;

            case "Desert":
                for (int i = 0; i < 150; i++) {
                    int x = rand.nextInt(800);
                    int y = rand.nextInt(600);
                    int w = 6 + rand.nextInt(6);
                    int h = 3 + rand.nextInt(3);
                    desertCracks.add(new Ellipse2D.Double(x, y, w, h));
                }
                for (int i = 0; i < 30; i++) {
                    int x = rand.nextInt(800);
                    int y = rand.nextInt(600);
                    int w = 15 + rand.nextInt(15);
                    int h = 15 + rand.nextInt(15);
                    desertRocks.add(new Rectangle2D.Double(x, y, w, h));
                }
                break;
        }
    }

    private void startWave() {
        waveInProgress = true;
        spawnZombies(zombiesPerWave);
    }
    private void drawPauseMenu(Graphics g) {
        
        g.setColor(new Color(40, 40, 40, 220));
        g.fillRoundRect(0, 0, 800, 600, 20, 20);
        
        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("OYUN DURAKLATILDI", 200, 200);
        
        g.setColor(new Color(255, 215, 0, 150));
        g.fillRect(200, 210, 420, 2);
        
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        int y = 260;
        
        g.setColor(new Color(200, 200, 200));
        g.fillRoundRect(250, y, 300, 40, 10, 10);
        g.setColor(new Color(50, 50, 50));
        g.drawRoundRect(250, y, 300, 40, 10, 10);
        g.setColor(new Color(0, 0, 0));
        g.drawString("Devam Et (P)", 320, y + 28);
        
        y += 60;
        g.setColor(new Color(200, 200, 200));
        g.fillRoundRect(250, y, 300, 40, 10, 10);
        g.setColor(new Color(50, 50, 50));
        g.drawRoundRect(250, y, 300, 40, 10, 10);
        g.setColor(new Color(0, 0, 0));
        g.drawString("Oyunu Kaydet (K)", 320, y + 28);
        
        y += 60;
        g.setColor(new Color(200, 200, 200));
        g.fillRoundRect(250, y, 300, 40, 10, 10);
        g.setColor(new Color(50, 50, 50));
        g.drawRoundRect(250, y, 300, 40, 10, 10);
        g.setColor(new Color(0, 0, 0));
        g.drawString("Kaydı Yükle (L)", 320, y + 28);
        
        y += 60;
        g.setColor(new Color(200, 100, 100));
        g.fillRoundRect(250, y, 300, 40, 10, 10);
        g.setColor(new Color(150, 50, 50));
        g.drawRoundRect(250, y, 300, 40, 10, 10);
        g.setColor(new Color(0, 0, 0));
        g.drawString("Oyundan Çık (Q)", 320, y + 28);
    }
    

    private void spawnZombies(int count) {
        Random rand = new Random();

        int normalCount = 5 + (currentWave - 1);
        int crawlerCount = currentWave >= 3 ? 3 + ((currentWave - 3) * 2) : 0;
        int tankCount = currentWave >= 5 ? 5 + (currentWave - 5) : 0;
        int spitterCount = currentWave >= 7 ? 3 + ((currentWave - 7) * 2) : 0;

        for (int i = 0; i < normalCount; i++) {
            int x = rand.nextInt(800);
            int y = rand.nextInt(600);
            zombies.add(generateZombieAtSafeDistance(new NormalZombie(x, y) , 150));
        }
        if (currentWave >= 3) {
            for (int i = 0; i < crawlerCount; i++) {
                int x = rand.nextInt(800);
                int y = rand.nextInt(600);
                zombies.add(generateZombieAtSafeDistance(new CrawlerZombie(x, y), 150));
            }
        }
        if (currentWave >= 5) {
            for (int i = 0; i < tankCount; i++) {
                int x = rand.nextInt(800);
                int y = rand.nextInt(600);
                zombies.add(generateZombieAtSafeDistance(new TankZombie(x, y), 150));
            }
        }
        if (currentWave >= 7) {
            for (int i = 0; i < spitterCount; i++) {
                int x = rand.nextInt(800);
                int y = rand.nextInt(600);
                zombies.add(generateZombieAtSafeDistance(new SpitterZombie(x, y) , 150));
            }
        }

        zombiesPerWave = normalCount + crawlerCount + tankCount + spitterCount;
    }
    private Zombie generateZombieAtSafeDistance(Zombie zombie, int minDistance) {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(800);
            y = rand.nextInt(600);
        } while (Math.hypot(playerX - x, playerY - y) < minDistance);
    
        zombie.setX(x);
        zombie.setY(y);
        return zombie;
    }
    
    private void nextWave() {
        if (currentWave >= MAX_WAVE) {
            gameWon = true;
            timer.stop();
            return;
        }
        currentWave++;
        waveInProgress = true;

        if (playerHealth < 60) {
            playerHealth += 30;
            if (playerHealth > 100) {
                playerHealth = 100;
            }
        }
        startWave();
    }

    private void restartGame() {
        playerX = 300;
        playerY = 300;
        playerHealth = 100;
        currentWave = 1;
        zombiesPerWave = 3;
        zombies.clear();
        bullets.clear();
        bloodStains.clear();
        score = 0;
        gameOver = false;
        
        weapons = new Weapon[] {
            new Weapon("Pistol", 120, 12, Integer.MAX_VALUE, 30),
            new Weapon("Rifle", 600, 30, 120, 20),
            new Weapon("Shotgun", 60, 5, 25, 50),
            new Weapon("Sniper", 30, 5, 15, 100),
            new Weapon("Rocket Launcher", 10, 1, 5, 200)
        };
        currentWeaponIndex = 0;
        
        startWave();
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (paused || gameOver) {
            return;
        }    
        if (inMainMenu || inHelpMenu) {
            playMenuMusic();
        } else {
            stopMenuMusic();
        }
        
        timeOfDay += daySpeed;
        if (timeOfDay > 1.0f) { 
            timeOfDay = 0.0f;
        }

        weatherTimer++;
        if (weatherTimer >= weatherDuration) {
            changeWeather();
            weatherTimer = 0;
        }

        updateWeatherEffects();

        for (Zombie z : zombies) {
            z.moveTowards(playerX, playerY);
            if (z.getBounds().intersects(new Rectangle(playerX, playerY, 40, 40))) {
                playerHealth -= 1;
                if (playerHealth <= 0) {
                    gameOver = true;
                    timer.stop();
                }
            }

            if (z instanceof SpitterZombie) {
                SpitterZombie spitter = (SpitterZombie) z;
                for (SpitterBullet sb : spitter.getBullets()) {
                    if (sb.getBounds().intersects(new Rectangle(playerX, playerY, 40, 40))) {
                        playerHealth -= 1;
                    }
                }
            }
        }

        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet b = bulletIterator.next();
            b.move();
            if (!b.isActive()) {
                bulletIterator.remove();
                continue;
            }

            handleBulletCollision(b, bulletIterator);
        }

        if (zombies.isEmpty() && waveInProgress) {
            waveInProgress = false;
            nextWave();
        }

        repaint();
        
        if (glowIncreasing) {
            glowAlpha += 0.01f;
        } else {
            glowAlpha -= 0.01f;
        }
        if (glowAlpha > 1.0f) {
            glowAlpha = 1.0f;
            glowIncreasing = false;
        }
        if (glowAlpha < 0.4f) {
            glowAlpha = 0.4f;
            glowIncreasing = true;
        }

        haloOffset = (haloOffset + 1) % 360;

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        drawMap(g2d);
        for (ExplosionEffect explosion : explosions) {
            explosion.draw(g2d);
        }
        
        drawPlayer(g2d);
        for (BloodStain blood : bloodStains) {
            blood.draw(g);
        }    
        for (Zombie z : zombies ) { 
            z.draw(g, playerX, playerY);
        }
        for (Bullet b : bullets) {
            b.draw(g);
        }    
        for (Obstacle o : obstacles) {
            o.draw(g);
        }
        
        explosions.removeIf(explosion -> !explosion.isActive());

        drawMuzzleFlash(g);
        drawWeatherEffects(g);
        drawDayNightOverlay(g);
        drawStatusBars(g);

        g.setColor(Color.WHITE);
        g.drawString("Wave: " + currentWave, 700, 35);
        g.drawString("Score: " + score, 700, 55);
        g.drawString("Weapon: " + weapons[currentWeaponIndex].getName(), 700, 75);
        g.drawString("Map: " + mapTheme, 700, 95);

        
        if (inMainMenu) {
            drawMainMenu(g);
            return;
        }

        if (inHelpMenu) {
            drawHelpMenu(g);
            return;
        }
        
        if (paused) {
            drawPauseMenu(g); 
        }

        if (gameOver) {
            g.setColor(new Color(40, 40, 40, 220));
            g.fillRoundRect(0, 0, 800, 600, 20, 20);
            
            g.setColor(new Color(255, 0, 0));
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("OYUN BİTTİ", 250, 200);
            
            g.setColor(new Color(255, 0, 0, 150));
            g.fillRect(250, 210, 300, 2);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("Final Skoru: " + score, 140, 285);
            g.drawString("Ulaştığın Dalga: " + currentWave, 440, 285);

            
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            int y = 320;
            
            g.setColor(new Color(200, 200, 200));
            g.fillRoundRect(250, y, 300, 40, 10, 10);
            g.setColor(new Color(50, 50, 50));
            g.drawRoundRect(250, y, 300, 40, 10, 10);
            g.setColor(new Color(0, 0, 0));
            g.drawString("Yeniden Başla (ENTER)", 280, y + 28);
            
            y += 60;
            g.setColor(new Color(200, 200, 200));
            g.fillRoundRect(250, y, 300, 40, 10, 10);
            g.setColor(new Color(50, 50, 50));
            g.drawRoundRect(250, y, 300, 40, 10, 10);
            g.setColor(new Color(0, 0, 0));
            g.drawString("Ana Menü (M)", 330, y + 28);
            
            y += 60;
            g.setColor(new Color(200, 100, 100));
            g.fillRoundRect(250, y, 300, 40, 10, 10);
            g.setColor(new Color(150, 50, 50));
            g.drawRoundRect(250, y, 300, 40, 10, 10);
            g.setColor(new Color(0, 0, 0));
            g.drawString("Çıkış (Q)", 350, y + 28);
        }
        if (gameWon) {
            drawVictoryScreen((Graphics2D) g);
            return;
        }
}
    private void drawMainMenu(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
    
        GradientPaint gradient = new GradientPaint(0, 0, new Color(10, 10, 10), 0, getHeight(), new Color(30, 30, 30));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    
        RadialGradientPaint vignette = new RadialGradientPaint(
            new Point(getWidth() / 2, getHeight() / 2),
            getWidth(),
            new float[]{0.7f, 1.0f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 180)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    
        int radius = 240;
        int cx = getWidth() / 2;
        int cy = 180;
        g2d.setColor(new Color(255, 255, 255, 10));
        g2d.fillOval(cx - radius + (int)(5 * Math.sin(Math.toRadians(haloOffset))), 
                     cy - radius + (int)(5 * Math.cos(Math.toRadians(haloOffset))),
                     radius * 2, radius * 2);

        g2d.setColor(new Color(40, 40, 40, 240));
        g2d.fillRoundRect(240, 320, 320, 320, 30, 30);
    
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(new Color(255, 255, 100, (int)(glowAlpha * 255)));
        g2d.drawString("TOP-DOWN SHOOTER", 180, 200);
    
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        Rectangle startBtn = new Rectangle(280, 330, 240, 35);
        Rectangle helpBtn = new Rectangle(280, 375, 240, 35);
        drawButton(g2d, startBtn, "ENTER - Oyuna Başla");
        drawButton(g2d, helpBtn, "H - Nasıl Oynanır");
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        Rectangle quitBtn = new Rectangle(280, 420, 240, 35);
        drawButton(g2d, quitBtn, "Q - Oyundan Çık");
    }

    private void drawButton(Graphics2D g2d, Rectangle rect, String text) {
        if (rect.contains(mousePosition)) {
            g2d.setColor(new Color(80, 120, 255));
            g2d.fillRoundRect(rect.x - 5, rect.y - 3, rect.width + 10, rect.height + 6, 15, 15);
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(100, 100, 100, 200));
            g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g2d.setColor(Color.LIGHT_GRAY);
        }
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int textY = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(text, textX, textY);
    }

    private void drawHelpMenu(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        GradientPaint gradient = new GradientPaint(0, 0, new Color(10, 10, 10), 0, getHeight(), new Color(30, 30, 30));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(new Color(30, 30, 30, 230));
        g2d.fillRoundRect(80, 50, 640, 500, 30, 30);

        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("NASIL OYNANIR?", 250, 100);
        g2d.fillRect(240, 110, 320, 2);

        g2d.setColor(new Color(255, 51, 51));
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Amaç: ", 100, 160);

        g2d.setColor(new Color(204, 204, 204));
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Dalga dalga gelen çeşitli zombileri farklı silahlar kullanıp öldürmektir.", 160, 160);
        g2d.drawString("Oyun 13.Dalga sonunda bitmektedir.", 160, 180);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.setColor(Color.LIGHT_GRAY);
        int y = 240;
        g2d.drawString(" W-A-S-D → Hareket", 100, y); y += 30;
        g2d.drawString(" Mouse Sol Tık → Ateş", 100, y); y += 30;
        g2d.drawString(" R → Mermi Doldur", 100, y); y += 30;
        g2d.drawString(" M → Harita Değiştir", 100, y); y += 30;
        g2d.drawString(" P → Oyunu Duraklat", 100, y); y += 30;
        g2d.drawString(" K → Kaydet (Duraklatmada)", 100, y); y += 30;
        g2d.drawString(" L → Yükle (Duraklatmada)", 100, y); y += 40;
    
        g2d.setColor(new Color(50, 50, 70));
        g2d.fillRoundRect(450, 220, 240, 160, 20, 20);
        g2d.setColor(new Color(255, 255, 255));
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.drawString(" Silahlar", 520, 250);
    
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        int sy = 275;
        g2d.drawString("1 - Pistol", 480, sy); sy += 24;
        g2d.drawString("2 - Rifle", 480, sy); sy += 24;
        g2d.drawString("3 - Shotgun", 480, sy); sy += 24;
        g2d.drawString("4 - Sniper", 480, sy); sy += 24;
        g2d.drawString("5 - Rocket Launcher", 480, sy);

        g2d.setColor(new Color(100, 255, 100));
        g2d.setFont(new Font("Arial", Font.ITALIC, 18));
        g2d.drawString("⬅ Geri dönmek için 'B' tuşuna basın", 250, 480);
    }
    
    private void drawStatusBars(Graphics g) {
        int barWidth = 200;
        int barHeight = 20;

        g.setColor(Color.GRAY);
        g.fillRect(20, 20, barWidth, barHeight);
        g.setColor(Color.RED);
        int healthWidth = (int) ((playerHealth / 100.0) * barWidth);
        g.fillRect(20, 20, healthWidth, barHeight);
        g.setColor(Color.WHITE);
        g.drawRect(20, 20, barWidth, barHeight);
        g.drawString("HEALTH", 230, 35);

        int ammoBarMax = weapons[currentWeaponIndex].getMagazineSize();
        int ammoInMag = weapons[currentWeaponIndex].getAmmoInMagazine();

        g.setColor(Color.GRAY);
        g.fillRect(20, 50, barWidth, barHeight);
        g.setColor(Color.BLUE);
        int ammoWidth = (int) ((ammoInMag / (double) ammoBarMax) * barWidth);
        g.fillRect(20, 50, ammoWidth, barHeight);
        g.setColor(Color.WHITE);
        g.drawRect(20, 50, barWidth, barHeight);
        g.drawString("AMMO", 230, 65);
    }

    private void drawDayNightOverlay(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        float alpha = 0f;

        if (timeOfDay <= 0.5f) {
            alpha = timeOfDay * 2;
        } else {
            alpha = (1.0f - timeOfDay) * 2;
        }

        Color overlay = new Color(0, 0, 0, (int)(alpha * 150));
        g2d.setColor(overlay);
        g2d.fillRect(0, 0, 800, 600);
    }

    private void drawWeatherEffects(Graphics g) {
        if (!mapTheme.equals("Desert")) {
            if (weather.equals("Rain") || weather.equals("Storm")) {
                for (Particle p : rainParticles) {
                    p.drawRain(g);
                }
            }

            if (weather.equals("Snow")) {
                for (Particle p : snowParticles) {
                    p.drawSnow(g);
                }
            }
        }
        if (weather.equals("Fog")) {
            g.setColor(new Color(200, 200, 200, 80));
            g.fillRect(0, 0, 800, 600);
        }
        if (weather.equals("Storm")) {
            Random rand = new Random();
            if (rand.nextInt(100) < 2) {
                g.setColor(new Color(255, 255, 255, 150));
                g.fillRect(0, 0, 800, 600);
                playSound("sounds/thunder.wav");
            }
        }
    }

    private void createMuzzleFlash() {
        flashTimer = 5;
    }

    private void drawMuzzleFlash(Graphics g) {
        if (flashTimer > 0) {
            g.setColor(Color.YELLOW);
            g.fillOval(playerX + 30, playerY + 10, 10, 10);
            flashTimer--;
        }
    }
    private void playMenuMusic() {
        try {
            if (menuMusicPlaying) {
                return;
            }    
            File file = new File("sounds/menu_music.wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            menuMusic = AudioSystem.getClip();
            menuMusic.open(audioStream);
            menuMusic.loop(Clip.LOOP_CONTINUOUSLY);
            menuMusic.start();
            menuMusicPlaying = true;
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopMenuMusic() {
        if (menuMusic != null && menuMusic.isRunning()) {
            menuMusic.stop();
            menuMusic.close();
        }
        menuMusicPlaying = false;
    }

    private void changeWeather() {
        String[] types;
        if (mapTheme.equals("Desert")) {
            types = new String[]{"Clear", "Fog", "Storm"};
        } else {
            types = new String[]{"Clear", "Rain", "Snow", "Fog", "Storm"};
        }
        
        Random rand = new Random();
        weather = types[rand.nextInt(types.length)];

        rainParticles.clear();
        snowParticles.clear();

        if ((weather.equals("Rain") || weather.equals("Storm")) && !mapTheme.equals("Desert")) {
            for (int i = 0; i < 200; i++) {
                rainParticles.add(new Particle(rand.nextInt(800), rand.nextInt(600), 2 + rand.nextInt(3), 10 + rand.nextInt(10)));
            }
        }

        if (weather.equals("Snow") && !mapTheme.equals("Desert")) {
            for (int i = 0; i < 150; i++) {
                snowParticles.add(new Particle(rand.nextInt(800), rand.nextInt(600), 1, 2 + rand.nextInt(2)));
            }
        }
    }

    private void updateWeatherEffects() {
        for (Particle p : rainParticles) {
            p.fall();
        }
        for (Particle p : snowParticles) {
            p.fall();
        }    
    }

    private void playSound(String path) {
        try {
            File file = new File(path);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getZombieScore(Zombie z) {
        if (z instanceof NormalZombie) return 10;
        if (z instanceof CrawlerZombie) return 15;
        if (z instanceof TankZombie) return 25;
        if (z instanceof SpitterZombie) return 20;
        return 0;
    }

    private void saveGame() {
        String inputName = JOptionPane.showInputDialog(this, 
            "Kayıt için isminizi giriniz:\nMevcut Skor: " + score,
            "Oyunu Kaydet",
            JOptionPane.QUESTION_MESSAGE);

        if (inputName != null && !inputName.trim().isEmpty()) {
            try {
                ArrayList<String> saves = new ArrayList<>();
                File saveFile = new File("saves.txt");
                if (saveFile.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(saveFile))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            saves.add(line);
                        }
                    }
                }
                String newSave = inputName + "," + score + "," + currentWave + "," + 
                               playerHealth + "," + playerX + "," + playerY;
                saves.add(newSave);
                saves.sort((a, b) -> {
                    int scoreA = Integer.parseInt(a.split(",")[1]);
                    int scoreB = Integer.parseInt(b.split(",")[1]);
                    return scoreB - scoreA;
                });

                if (saves.size() > 10) {
                    saves = new ArrayList<>(saves.subList(0, 10));
                }

                try (FileWriter fw = new FileWriter(saveFile)) {
                    for (String save : saves) {
                        fw.write(save + "\n");
                    }
                }

                JOptionPane.showMessageDialog(this,
                    "Oyun başarıyla kaydedildi!\nSkor: " + score,
                    "Kayıt Başarılı",
                    JOptionPane.INFORMATION_MESSAGE);
            } 
            catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Kaydetme hatası: " + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadGame() {
        try {
            File saveFile = new File("saves.txt");
            if (!saveFile.exists()) {
                JOptionPane.showMessageDialog(this,
                    "Henüz kayıtlı oyun bulunmuyor!",
                    "Kayıt Bulunamadı",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            ArrayList<String> saves = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(saveFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    saves.add(line);
                }
            }

            StringBuilder message = new StringBuilder("Kayıtlı Oyunlar:\n\n");
            for (int i = 0; i < saves.size(); i++) {
                String[] data = saves.get(i).split(",");
                message.append((i + 1)).append(". ")
                      .append(data[0]).append(" - Skor: ").append(data[1])
                      .append(" - Dalga: ").append(data[2]).append("\n");
            }
            message.append("\nYüklemek istediğiniz kaydın numarasını girin:");

            String input = JOptionPane.showInputDialog(this,
                message.toString(),
                "Kayıt Yükle",
                JOptionPane.QUESTION_MESSAGE);

            if (input != null && !input.trim().isEmpty()) {
                try {
                    int selection = Integer.parseInt(input) - 1;
                    if (selection >= 0 && selection < saves.size()) {
                        String[] data = saves.get(selection).split(",");
                        playerName = data[0];
                        score = Integer.parseInt(data[1]);
                        currentWave = Integer.parseInt(data[2]);
                        playerHealth = Integer.parseInt(data[3]);
                        playerX = Integer.parseInt(data[4]);
                        playerY = Integer.parseInt(data[5]);

                        zombies.clear();
                        bullets.clear();
                        bloodStains.clear();
                        waveInProgress = false;
                        startWave();

                        JOptionPane.showMessageDialog(this,
                            "Oyun başarıyla yüklendi!\nOyuncu: " + playerName + "\nSkor: " + score,
                            "Yükleme Başarılı",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "Geçersiz seçim!",
                            "Hata",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } 
                catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this,
                        "Geçersiz seçim! Lütfen bir sayı girin.",
                        "Hata",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        } 
        catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Yükleme hatası: " + e.getMessage(),
                "Hata",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private class Particle {
        int x, y, speed, size;

        public Particle(int x, int y, int speed, int size) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
        }

        public void fall() {
            y += speed;
            if (y > 600) {
                y = 0;
                x = new Random().nextInt(800);
            }
        }

        public void drawRain(Graphics g) {
            g.setColor(Color.CYAN);
            g.fillRect(x, y, 2, size);
        }

        public void drawSnow(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillOval(x, y, size, size);
        }
    }
    private void loadPlayerImage() {
        try {
            playerImg = ImageIO.read(new File("resources/player/player_sprite.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void drawPlayer(Graphics2D g2d) {
        double angle = Math.atan2(mousePosition.y - playerY, mousePosition.x - playerX);
        int width = playerImg.getWidth();
        int height = playerImg.getHeight();

        g2d.translate(playerX, playerY);
        g2d.rotate(angle);
        g2d.drawImage(playerImg, -width / 2, -height / 2, null);
        g2d.rotate(-angle);
        g2d.translate(-playerX, -playerY);
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        mousePosition = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mousePosition = e.getPoint();
    }
    private boolean collidesWithObstacle(int newX, int newY) {
        Rectangle futurePlayer = new Rectangle(newX, newY, 40, 40);
        for (Obstacle o : obstacles) {
            if (o.getBounds().intersects(futurePlayer)) {
                return true;
            }
        }
        return false;
    }

    private void checkWeaponAvailability(int key) {
        int weaponIndex = key - KeyEvent.VK_1;
        boolean canSwitch = false;
        
        switch (weaponIndex) {
            case 0:
                canSwitch = true;
                break;
            case 1:
                canSwitch = currentWave > 1;
                break;
            case 2:
                canSwitch = currentWave > 3;
                break;
            case 3:
                canSwitch = currentWave > 5;
                break;
            case 4:
                canSwitch = currentWave > 7;
                break;
        }
        
        if (canSwitch) {
            currentWeaponIndex = weaponIndex;
        }
    }

    private class ExplosionEffect {
        private int x, y;
        private int radius;
        private int duration;
        private final int maxDuration = 10;

        public ExplosionEffect(int x, int y) {
            this.x = x;
            this.y = y;
            this.radius = 80;
            this.duration = maxDuration;
        }

        public void draw(Graphics2D g2d) {
            if (duration > 0) {
                int alpha = (int)((duration / (float)maxDuration) * 255);

                g2d.setColor(new Color(255, 140, 0, alpha));
                g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);

                g2d.setColor(new Color(255, 200, 0, alpha));
                g2d.fillOval(x - radius/2, y - radius/2, radius, radius);
                duration--;
            }
        }

        public boolean isActive() {
            return duration > 0;
        }

        public Rectangle getBounds() {
            return new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
        }
    }

    private void handleBulletCollision(Bullet b, Iterator<Bullet> bulletIterator) {
        Rectangle bulletBounds = b.getBounds();
        int bulletX = bulletBounds.x + bulletBounds.width/2;
        int bulletY = bulletBounds.y + bulletBounds.height/2;

        if (currentWeaponIndex == 4) {
            ExplosionEffect explosion = new ExplosionEffect(bulletX, bulletY);
            explosions.add(explosion);
            playSound("sounds/explosion.wav");
            
            for (Zombie z : new ArrayList<>(zombies)) {
                if (explosion.getBounds().intersects(z.getBounds())) {
                    z.takeDamage(b.getDamage());
                    if (z.isDead()) {
                        zombies.remove(z);
                        score += getZombieScore(z);
                        bloodStains.add(new BloodStain(z.getBounds().x, z.getBounds().y));
                        playSound("sounds/zombie_growl.wav");
                    }
                }
            }
            bulletIterator.remove();
        } 
        else {
            for (Zombie z : new ArrayList<>(zombies)) {
                if (bulletBounds.intersects(z.getBounds())) {
                    z.takeDamage(b.getDamage());
                    bulletIterator.remove();
                    if (z.isDead()) {
                        zombies.remove(z);
                        score += getZombieScore(z);
                        bloodStains.add(new BloodStain(z.getBounds().x, z.getBounds().y));
                        playSound("sounds/zombie_growl.wav");
                    }
                    break;
                }
            }
        }
    }
    private void drawVictoryScreen(Graphics2D g2d) {
        int btnWidth = 320;
        int btnHeight = 40;

        GradientPaint gp = new GradientPaint(0, 0, new Color(0, 50, 0), 0, getHeight(), new Color(0, 20, 0));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.setColor(new Color(144, 238, 144, (int)(glowAlpha * 255)));
        g2d.drawString("TEBRİKLER!", 220, 150);
    
        g2d.setColor(new Color(173, 255, 47, 100));
        g2d.fillOval(220 - (int)(Math.sin(Math.toRadians(haloOffset)) * 10), 100, 360, 80);
    
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Tüm dalgaları başarıyla tamamladınız.", 160, 230);
        g2d.drawString("Skorunuz: " + score, 290, 280);
    
        int x1 = (getWidth() - btnWidth) / 2;
        int y1 = 360;
        g2d.setColor(new Color(200, 255, 200));
        g2d.fillRoundRect(x1, y1, btnWidth, btnHeight, 15, 15);
        g2d.setColor(new Color(100, 200, 100));
        g2d.drawRoundRect(x1, y1, btnWidth, btnHeight, 15, 15);
        g2d.setColor(Color.BLACK); 
        g2d.setFont(new Font("Arial", Font.PLAIN, 22));
        g2d.drawString("Ana menüye dönmek için (M)", x1 + 40, y1 + 27);

        int y2 = y1 + 60;
        g2d.setColor(new Color(255, 200, 200));
        g2d.fillRoundRect(x1, y2, btnWidth, btnHeight, 15, 15);
        g2d.setColor(new Color(200, 100, 100));
        g2d.drawRoundRect(x1, y2, btnWidth, btnHeight, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Oyundan çıkmak için (Q)", x1 + 55, y2 + 27);
    }    
}