package gui.component.base;

import java.awt.*;
import javax.swing.JPanel;

public class RoundPanel extends JPanel {
    private int radius = 20;
    public RoundPanel() {
        super();
        setOpaque(false);
    }
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(getBackground());
        graphics.fillRoundRect(0, 0, width - 1, height - 1, radius, radius);
        super.paintComponent(g);
    }
}

