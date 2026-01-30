package zombies;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TankZombie extends Zombie {
    private BufferedImage sprite;

    public TankZombie(int x, int y) {
        super(x, y, 200, 0.5);
        try {
            BufferedImage img = ImageIO.read(new File("resources/zombies/tank_zombie.png"));
            setSprite(img);
        } catch (IOException e) {
            e.printStackTrace();
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
        g2d.drawImage(sprite, -25, -25, 50, 50, null);
        g2d.rotate(-angle);
        g2d.translate(-x, -y);
    }
    @Override
    public void moveTowards(int targetX, int targetY) {
        double angle = Math.atan2(targetY - y, targetX - x);
        x += speed * Math.cos(angle);
        y += speed * Math.sin(angle);
    }
    public void setSprite(BufferedImage img) {
        this.sprite = img;
    }
}