import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

public class ConicalSpiral3D extends JPanel {

    private static final int W = 1000, H = 800;

    // 參數區：可依需求微調
    private static final int TURNS = 6;         // 螺旋圈數
    private static final double DECAY_PER_TURN = 0.75; // 每轉一圈半徑縮小比例（0.75 = 75%）
    private static final double D_THETA = 0.01; // 角度步距（弧度），越小越平滑
    private static final double LIFT_PER_RAD = 6.0; // 每弧度上升的高度（控制“往下位置就抬高一點”）

    // 視角/投影參數
    private static final double YAW_DEG = 35;   // 左右旋轉（度）
    private static final double PITCH_DEG = 25; // 上下俯仰（度）
    private static final double FOV = 850;      // 透視焦距（越大越不誇張）
    private static final double DEPTH = 300;    // 透視深度偏移，避免除零

    @Override public Dimension getPreferredSize() { return new Dimension(W, H); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2 + 40;

        // 初始半徑用畫面邊長決定
        double r0 = Math.min(getWidth(), getHeight()) * 0.38;

        // 將對數衰減轉為每弧度的縮放：r(θ) = r0 * (DECAY_PER_TURN)^(θ / 2π)
        double twoPi = Math.PI * 2.0;
        double thetaMax = TURNS * twoPi;

        // 把視角轉成弧度，預算旋轉矩陣用的 cos/sin
        double yaw = Math.toRadians(YAW_DEG);
        double pitch = Math.toRadians(PITCH_DEG);
        double cyaw = Math.cos(yaw), syaw = Math.sin(yaw);
        double cpitch = Math.cos(pitch), spitch = Math.sin(pitch);

        // 畫一點簡單的地面網格，增加 3D 感
        drawGroundGrid(g2, cx, cy, cyaw, syaw, cpitch, spitch);

        // 用由遠到近的「畫線」順序（θ 由小到大，z 會越來越近），讓近處覆蓋遠處
        double theta = 0.0;

        // 第一個點
        double r = r0 * Math.pow(DECAY_PER_TURN, theta / twoPi);
        double x = r * Math.cos(theta);
        double y = r * Math.sin(theta);
        double z = LIFT_PER_RAD * theta;

        Point pPrev = project3D(cx, cy, x, y, z, cyaw, syaw, cpitch, spitch);

        // 線條從遠到近，近的稍微加粗/深一點
        for (theta = D_THETA; theta <= thetaMax; theta += D_THETA) {
            r = r0 * Math.pow(DECAY_PER_TURN, theta / twoPi); // 半徑隨角度遞減
            x = r * Math.cos(theta);
            y = r * Math.sin(theta);
            z = LIFT_PER_RAD * theta; // 高度隨角度遞增（每一步“抬高一點”）

            Point p = project3D(cx, cy, x, y, z, cyaw, syaw, cpitch, spitch);

            // 依 z'（投影前的相機座標 Z）決定顏色與粗細
            double zCam = lastZCam;
            // 其實在 project3D 已更新 lastZCam（見下方變數/流程）
            float t = (float) clamp((lastZCam - 0) / 800.0, 0, 1); // 調整 0~800 的深度範圍
            float alpha = (float) (0.30 + 0.50 * (1 - t)); // 近處更不透明
            float gray = (float) (0.25 + 0.65 * (1 - t));  // 近處更亮
            float width = (float) (1.0 + 2.5 * (1 - t));   // 近處更粗

            g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(gray, gray, gray, alpha));
            g2.draw(new Line2D.Double(pPrev.x, pPrev.y, p.x, p.y));

            pPrev = p;
        }

        // 畫出起始/終點的小圓點
        g2.setColor(new Color(30, 80, 200, 200));
        fillCircle(g2, project3D(cx, cy, r0, 0, 0, cyaw, syaw, cpitch, spitch), 5);
        g2.setColor(new Color(200, 60, 30, 220));
        fillCircle(g2, pPrev, 6);

        g2.dispose();
    }

    // 儲存上一次的相機座標 Z（用於深度調色）
    private double lastZCam = 0;

    private Point project3D(int cx, int cy, double x, double y, double z,
                            double cyaw, double syaw, double cpitch, double spitch) {
        // 先繞 Y 軸（左右）旋轉（Yaw）
        double x1 =  cyaw * x + syaw * z;
        double y1 =  y;
        double z1 = -syaw * x + cyaw * z;

        // 再繞 X 軸（上下）旋轉（Pitch）
        double x2 = x1;
        double y2 =  cpitch * y1 - spitch * z1;
        double z2 =  spitch * y1 + cpitch * z1;

        // 簡單透視投影
        double denom = (DEPTH + z2);
        if (denom < 1) denom = 1;

        double sx = cx + (FOV * x2) / denom;
        double sy = cy - (FOV * y2) / denom;

        lastZCam = z2; // 給上層用來做顏色/粗細插值
        return new Point((int) Math.round(sx), (int) Math.round(sy));
    }

    private static void drawGroundGrid(Graphics2D g2, int cx, int cy,
                                       double cyaw, double syaw, double cpitch, double spitch) {
        // 在 x-y 平面上畫一個網格（z=0），透過投影製造 3D 感
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(0, 0, 0, 30));

        int half = 600;
        int step = 60;
        for (int i = -half; i <= half; i += step) {
            // 線 1：平行 x 軸（y = i）
            Point p1 = projHelper(cx, cy, -half, i, 0, cyaw, syaw, cpitch, spitch);
            Point p2 = projHelper(cx, cy,  half, i, 0, cyaw, syaw, cpitch, spitch);
            g2.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));

            // 線 2：平行 y 軸（x = i）
            Point p3 = projHelper(cx, cy, i, -half, 0, cyaw, syaw, cpitch, spitch);
            Point p4 = projHelper(cx, cy, i,  half, 0, cyaw, syaw, cpitch, spitch);
            g2.draw(new Line2D.Double(p3.x, p3.y, p4.x, p4.y));
        }
    }

    private static Point projHelper(int cx, int cy, double x, double y, double z,
                                    double cyaw, double syaw, double cpitch, double spitch) {
        // 只用於地面格線，不需要深度著色，複製簡化的投影（與 project3D 保持一致）
        double x1 =  cyaw * x + syaw * z;
        double y1 =  y;
        double z1 = -syaw * x + cyaw * z;

        double x2 = x1;
        double y2 =  cpitch * y1 - spitch * z1;
        double z2 =  spitch * y1 + cpitch * z1;

        double denom = (DEPTH + z2);
        if (denom < 1) denom = 1;

        double sx = cx + (FOV * x2) / denom;
        double sy = cy - (FOV * y2) / denom;
        return new Point((int) Math.round(sx), (int) Math.round(sy));
    }

    private static void fillCircle(Graphics2D g2, Point p, int r) {
        g2.fillOval(p.x - r, p.y - r, r * 2, r * 2);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Conical 3D Spiral (sin/cos + shrinking radius + lifted z)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new ConicalSpiral3D());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
