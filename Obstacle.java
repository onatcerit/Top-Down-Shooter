import java.awt.*;

public class Obstacle {
    private Rectangle bounds;

    public Obstacle(int x, int y, int width, int height) {
        bounds = new Rectangle(x, y, width, height);
    }

    public void draw(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
