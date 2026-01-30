package zombies;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

public class SpitterZombie extends Zombie {
    private List<SpitterBullet> bullets = new ArrayList<>();
    private int attackCooldown = 120;
    private BufferedImage sprite;
    private int shootCooldown = 0;
    private Random rand;

    public SpitterZombie(int x, int y) {
        super(x, y, 150, 0.8);
        
        bullets = new ArrayList<>();
        rand = new Random();

        try {
            BufferedImage img = ImageIO.read(new File("resources/zombies/spitter_zombie.png"));
            setSprite(img);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void moveTowards(int playerX, int playerY) {
        int distance = (int) Math.hypot(playerX - x, playerY - y);
        if (distance > 200) super.moveTowards(playerX, playerY);

        if (attackCooldown <= 0) {
            bullets.add(new SpitterBullet((int)x + 20, (int)y + 20, playerX, playerY));
            attackCooldown = 120;
        } else {
            attackCooldown--;
        }

        Iterator<SpitterBullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            SpitterBullet bullet = iterator.next();
            bullet.move();
            if (!bullet.isActive()) iterator.remove();
        }
    }

    @Override
    public void draw(Graphics g, int playerX, int playerY) {
        if (sprite == null) return;

        Graphics2D g2d = (Graphics2D) g;
        double angle = Math.atan2(playerY - y, playerX - x);
        int w = sprite.getWidth();
        int h = sprite.getHeight();

        g2d.translate(x, y);
        g2d.rotate(angle);
        g2d.drawImage(sprite, -20, -20, 40, 40, null);
        g2d.rotate(-angle);
        g2d.translate(-x, -y);

        for (SpitterBullet b : bullets) b.draw(g);
    }
    
    public void setSprite(BufferedImage img) {
        this.sprite = img;
    }
    
    public List<SpitterBullet> getBullets() { 
        return bullets; 
    }
}
