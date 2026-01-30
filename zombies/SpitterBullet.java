package zombies;

import java.awt.*;

public class SpitterBullet {
    private double x, y;
    private double dirX, dirY;
    private int speed = 5;
    private boolean active = true;

    public SpitterBullet(int x, int y, int targetX, int targetY) {
        this.x = x;
        this.y = y;
        double angle = Math.atan2(targetY - y, targetX - x);
        dirX = Math.cos(angle) * speed;
        dirY = Math.sin(angle) * speed;
    }

    public void move() {
        x += dirX;
        y += dirY;
        if (x < 0 || x > 800 || y < 0 || y > 600) active = false;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillOval((int)x,(int) y, 10, 10);
    }

    public boolean isActive() { 
        return active; 
    }
    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, 10, 10); 
    }
}
