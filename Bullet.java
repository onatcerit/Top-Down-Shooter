import java.awt.Rectangle;

public class Bullet {
    private double x, y;
    private double dirX, dirY;
    private int speed = 10;
    private boolean active = true;
    private int damage;


    public Bullet(int startX, int startY, int targetX, int targetY, int damage) {
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        double angle = Math.atan2(targetY - startY, targetX - startX);
        dirX = speed * Math.cos(angle);
        dirY = speed * Math.sin(angle);
    }

    public void move() {
        double newX = x + dirX;
        double newY = y + dirY;
        
        if (newX < 0 || newX > 800 || newY < 0 || newY > 600) {
            active = false;
            return;
        }

        Rectangle futureBullet = new Rectangle((int)newX, (int)newY, 5, 5);
        for (Obstacle o : GamePanel.obstacles) {    
            if (o.getBounds().intersects(futureBullet)) {
                active = false;
                return;
            }
        }

        x = newX;
        y = newY;
    }

    public void draw(java.awt.Graphics g) {
        g.setColor(java.awt.Color.YELLOW);
        g.fillOval((int)x, (int)y, 5, 5);
    }

    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle((int)x, (int)y, 5, 5);
    }

    public boolean isActive() {
        return active; 
    }

    public void deactivate() {
        active = false;
    }

    public int getDamage() {
        return damage;
    }

}
