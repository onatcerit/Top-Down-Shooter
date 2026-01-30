
import java.awt.*;

public class BloodStain {
    private int x, y;
    private int size;

    public BloodStain(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = (int)(30 + Math.random() * 20);
    }

    public void draw(Graphics g) {
        g.setColor(new Color(139, 0, 0, 150));
        g.fillOval(x, y, size, size);
    }
}
