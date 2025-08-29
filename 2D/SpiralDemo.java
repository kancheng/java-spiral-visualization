import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

public class SpiralDemo extends JPanel {
    // 切換螺旋型態：LOGARITHMIC（對數螺旋：r = r0 * a^(θ/2π)）
    // 或 ARCHIMEDEAN（阿基米德螺旋：r = r0 - k*θ）
    enum SpiralType { LOGARITHMIC, ARCHIMEDEAN }
    private static final SpiralType TYPE = SpiralType.LOGARITHMIC;

    private static final int W = 900, H = 900;

    @Override public Dimension getPreferredSize() { return new Dimension(W, H); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        double r0 = Math.min(getWidth(), getHeight()) * 0.42; // 初始半徑
        int turns = 7;                   // 旋轉圈數
        double dTheta = 0.01;            // 每步進的角度 (弧度)
        double twoPi = Math.PI * 2.0;
        double thetaMax = turns * twoPi;

        // 參數：對數螺旋每轉一圈的縮小比例（例如 0.75 代表每圈半徑變為 75%）
        double decayPerTurn = 0.75;

        // 參數：阿基米德螺旋的線性縮小，設定最後半徑佔 r0 的比例（例如 0.1）
        double rEnd = r0 * 0.10;
        double shrinkPerRadian = (r0 - rEnd) / thetaMax;

        // 初始點
        double theta = 0.0;
        double r = r0;
        double xPrev = cx + r * Math.cos(theta);
        double yPrev = cy + r * Math.sin(theta);

        for (theta = dTheta; theta <= thetaMax; theta += dTheta) {
            if (TYPE == SpiralType.LOGARITHMIC) {
                // r(θ) = r0 * (decayPerTurn)^(θ / 2π)
                r = r0 * Math.pow(decayPerTurn, theta / twoPi);
            } else {
                // r(θ) = r0 - k*θ
                r = r0 - shrinkPerRadian * theta;
                if (r <= 0) break;
            }
            double x = cx + r * Math.cos(theta);
            double y = cy + r * Math.sin(theta);
            g2.draw(new Line2D.Double(xPrev, yPrev, x, y));
            xPrev = x; yPrev = y;
        }
        g2.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Spiral 2D (sin/cos)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new SpiralDemo());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
