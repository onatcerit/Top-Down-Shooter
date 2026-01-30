package zombies;

import java.awt.*;
import java.awt.image.BufferedImage;


public abstract class Zombie {
    protected double x, y;
    protected int health;
    protected double speed;
    protected int width = 40; 
    protected int height = 40;
    protected BufferedImage sprite;

    public Zombie(int x, int y, int health, double speed) {
        this.x = x;
        this.y = y;
        this.health = health;
        this.speed = speed;
    }
    
    public abstract void draw(Graphics g, int playerX, int playerY);

    public void moveTowards(int playerX, int playerY) {
        double angle = Math.atan2(playerY - y, playerX - x);
        double dx = speed * Math.cos(angle);
        double dy = speed * Math.sin(angle);
    
        if (Math.hypot(playerX - x, playerY - y) < 2.0) {
            return;
        }
    
        x += dx;
        y += dy;
    }

    public void takeDamage(int dmg) {
        health -= dmg;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x - 20, (int)y - 20, 40, 40);
    }
    
    public void setSprite(BufferedImage img) {
        this.sprite = img;
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }
}