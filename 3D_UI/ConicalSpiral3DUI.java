import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.geom.Line2D;

public class ConicalSpiral3DUI extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConicalSpiral3DUI ui = new ConicalSpiral3DUI();
            ui.setVisible(true);
        });
    }

    private final SpiralPanel canvas;
    private final JSlider turnsSlider, decaySlider, liftSlider, yawSlider, pitchSlider, endRatioSlider;
    private final JCheckBox archCheck;
    private final JSlider dThetaSlider, fovSlider, depthSlider;

    public ConicalSpiral3DUI() {
        super("3D Conical Spiral with Live Controls");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.canvas = new SpiralPanel();

        // 右側控制面板
        JPanel controls = new JPanel();
        controls.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        // 1. 螺旋類型
        archCheck = new JCheckBox("Archimedean (線性縮小半徑)");
        archCheck.setSelected(false);

        // 2. 圈數 1..12
        turnsSlider = labeledSlider("圈數 (turns)", 1, 12, canvas.getTurns());

        // 3. 每圈縮小比例 0.50..0.95 以百分比呈現
        // 用整數 50..95 對應 0.50..0.95
        decaySlider = labeledSlider("每圈縮小比例 decay/turn (%)", 50, 95, (int)Math.round(canvas.getDecayPerTurn()*100));

        // 4. 阿基米德模式的最終半徑比例 2..30%（避免收斂到 0）
        endRatioSlider = labeledSlider("Archimedean 終端半徑比例 (%)", 2, 30, 8);

        // 5. 每弧度抬升高度 0..20（以 0.1 為步進，即 0..2.0 對映 0..20）
        liftSlider = labeledSlider("每弧度上升高度 lift/rad (×0.1)", 0, 50, (int)Math.round(canvas.getLiftPerRad()*5)); // 預設 10 對應 2.0

        // 6. 視角
        yawSlider   = labeledSlider("Yaw (°)",   -80, 80, (int)Math.round(canvas.getYawDeg()));
        pitchSlider = labeledSlider("Pitch (°)", -10, 60, (int)Math.round(canvas.getPitchDeg()));

        // 7. 精細度與投影
        dThetaSlider = labeledSlider("細緻度 dTheta (×0.001)", 1, 30, 10); // 0.001 .. 0.030
        fovSlider    = labeledSlider("FOV 焦距", 300, 1400, (int)Math.round(canvas.getFov()));
        depthSlider  = labeledSlider("Depth 偏移", 100, 900, (int)Math.round(canvas.getDepth()));

        // 提示
        JLabel hint = new JLabel("<html><body style='width:240px'>提示：<br/>" +
                "・對數螺旋用「每圈縮小比例」<br/>" +
                "・阿基米德螺旋用「終端半徑比例」<br/>" +
                "・dTheta 越小越平滑，但越吃效能</body></html>");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        controls.add(archCheck);
        controls.add(Box.createVerticalStrut(8));
        controls.add(turnsSlider);
        controls.add(decaySlider);
        controls.add(endRatioSlider);
        controls.add(liftSlider);
        controls.add(Box.createVerticalStrut(8));
        controls.add(yawSlider);
        controls.add(pitchSlider);
        controls.add(Box.createVerticalStrut(8));
        controls.add(dThetaSlider);
        controls.add(fovSlider);
        controls.add(depthSlider);
        controls.add(Box.createVerticalStrut(8));
        controls.add(hint);

        // 綁定事件
        ChangeListener repaintOnChange = e -> {
            canvas.setArchimedean(archCheck.isSelected());
            canvas.setTurns(turnsSlider.getValue());

            double decay = decaySlider.getValue() / 100.0; // 0.50..0.95
            canvas.setDecayPerTurn(decay);

            double endRatio = endRatioSlider.getValue() / 100.0; // 0.02..0.30
            canvas.setEndRatio(endRatio);

            double lift = liftSlider.getValue() / 10.0; // 0..5.0
            canvas.setLiftPerRad(lift);

            canvas.setYawDeg(yawSlider.getValue());
            canvas.setPitchDeg(pitchSlider.getValue());

            double dTheta = dThetaSlider.getValue() / 1000.0; // 0.001..0.030
            canvas.setDTheta(dTheta);

            canvas.setFov(fovSlider.getValue());
            canvas.setDepth(depthSlider.getValue());

            canvas.repaint();
        };

        archCheck.addChangeListener(repaintOnChange);
        for (JSlider s : new JSlider[]{turnsSlider, decaySlider, endRatioSlider, liftSlider, yawSlider, pitchSlider, dThetaSlider, fovSlider, depthSlider}) {
            s.addChangeListener(repaintOnChange);
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, controls);
        split.setResizeWeight(1.0);
        setContentPane(split);
        setSize(1200, 820);
        setLocationRelativeTo(null);
    }

    private static JSlider labeledSlider(String title, int min, int max, int val) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(title);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        JSlider s = new JSlider(min, max, val);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setMajorTickSpacing((max - min) / 4);
        s.setPaintTicks(true);
        s.setPaintLabels(true);
        p.add(l);
        p.add(s);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel wrap = new JPanel();
        wrap.setLayout(new BorderLayout());
        wrap.add(p, BorderLayout.CENTER);
        // 小技巧：把 slider 放入 titled border 容器以取得一致的間距
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        holder.add(wrap, BorderLayout.CENTER);
        // 直接回傳 slider，但呼叫端會把這個 slider 加入容器，因此這裡改回傳 s
        return s;
    }

    // 畫布
    static class SpiralPanel extends JPanel {
        private int turns = 6;
        private boolean archimedean = false;
        private double decayPerTurn = 0.75; // 對數螺旋每圈縮小比例
        private double endRatio = 0.08;     // 阿基米德終端半徑比例 r_end = r0 * endRatio
        private double liftPerRad = 2.0;    // 每弧度上升高度
        private double yawDeg = 35.0;
        private double pitchDeg = 25.0;
        private double dTheta = 0.01;       // 精細度
        private double fov = 850.0;
        private double depth = 300.0;

        public SpiralPanel() {
            setBackground(Color.white);
        }

        // getters
        public int getTurns() { return turns; }
        public double getDecayPerTurn() { return decayPerTurn; }
        public double getLiftPerRad() { return liftPerRad; }
        public double getYawDeg() { return yawDeg; }
        public double getPitchDeg() { return pitchDeg; }
        public double getFov() { return fov; }
        public double getDepth() { return depth; }

        // setters
        public void setTurns(int t) { this.turns = Math.max(1, t); }
        public void setArchimedean(boolean b) { this.archimedean = b; }
        public void setDecayPerTurn(double d) { this.decayPerTurn = clamp(d, 0.5, 0.98); }
        public void setEndRatio(double r) { this.endRatio = clamp(r, 0.02, 0.5); }
        public void setLiftPerRad(double l) { this.liftPerRad = clamp(l, 0, 8.0); }
        public void setYawDeg(double d) { this.yawDeg = d; }
        public void setPitchDeg(double d) { this.pitchDeg = d; }
        public void setDTheta(double dt) { this.dTheta = clamp(dt, 0.001, 0.05); }
        public void setFov(double f) { this.fov = clamp(f, 200, 2000); }
        public void setDepth(double d) { this.depth = clamp(d, 50, 2000); }

        @Override public Dimension getPreferredSize() { return new Dimension(900, 800); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2 + 40;

            double r0 = Math.min(getWidth(), getHeight()) * 0.38;
            double twoPi = Math.PI * 2.0;
            double thetaMax = turns * twoPi;

            double yaw = Math.toRadians(yawDeg);
            double pitch = Math.toRadians(pitchDeg);
            double cyaw = Math.cos(yaw), syaw = Math.sin(yaw);
            double cpitch = Math.cos(pitch), spitch = Math.sin(pitch);

            // 地面格線
            drawGroundGrid(g2, cx, cy, cyaw, syaw, cpitch, spitch, fov, depth);

            // 初始點
            double theta = 0.0;
            double r = radiusAt(theta, r0, thetaMax);
            double x = r * Math.cos(theta);
            double y = r * Math.sin(theta);
            double z = liftPerRad * theta;
            PointExt pPrev = project3D(cx, cy, x, y, z, cyaw, syaw, cpitch, spitch, fov, depth);

            for (theta = dTheta; theta <= thetaMax; theta += dTheta) {
                r = radiusAt(theta, r0, thetaMax);
                if (r <= 0) break;
                x = r * Math.cos(theta);
                y = r * Math.sin(theta);
                z = liftPerRad * theta;

                PointExt p = project3D(cx, cy, x, y, z, cyaw, syaw, cpitch, spitch, fov, depth);

                // 依相機 Z 深度做簡單著色和粗細
                float t = (float) clamp((p.zCam - 0) / 800.0, 0, 1);
                float alpha = (float) (0.25 + 0.55 * (1 - t));
                float gray =  (float) (0.25 + 0.65 * (1 - t));
                float width = (float) (1.0 + 2.5 * (1 - t));

                g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(gray, gray, gray, alpha));
                g2.draw(new Line2D.Double(pPrev.x, pPrev.y, p.x, p.y));

                pPrev = p;
            }

            // 起點與終點
            g2.setColor(new Color(30, 80, 200, 200));
            fillCircle(g2, project3D(cx, cy, r0, 0, 0, cyaw, syaw, cpitch, spitch, fov, depth), 5);
            g2.setColor(new Color(200, 60, 30, 220));
            fillCircle(g2, pPrev, 6);

            g2.dispose();
        }

        private double radiusAt(double theta, double r0, double thetaMax) {
            if (!archimedean) {
                // 對數螺旋
                double twoPi = Math.PI * 2.0;
                return r0 * Math.pow(decayPerTurn, theta / twoPi);
            } else {
                // 阿基米德螺旋：線性縮小至 r_end
                double rEnd = r0 * endRatio;
                double k = (r0 - rEnd) / thetaMax; // 每弧度減少量
                return Math.max(rEnd, r0 - k * theta);
            }
        }

        private static class PointExt extends Point {
            final double zCam;
            PointExt(int x, int y, double zCam) { super(x, y); this.zCam = zCam; }
        }

        private static PointExt project3D(int cx, int cy, double x, double y, double z,
                                          double cyaw, double syaw, double cpitch, double spitch,
                                          double fov, double depth) {
            // Yaw
            double x1 =  cyaw * x + syaw * z;
            double y1 =  y;
            double z1 = -syaw * x + cyaw * z;
            // Pitch
            double x2 = x1;
            double y2 =  cpitch * y1 - spitch * z1;
            double z2 =  spitch * y1 + cpitch * z1;

            double denom = (depth + z2);
            if (denom < 1) denom = 1;

            double sx = cx + (fov * x2) / denom;
            double sy = cy - (fov * y2) / denom;

            return new PointExt((int)Math.round(sx), (int)Math.round(sy), z2);
        }

        private static void drawGroundGrid(Graphics2D g2, int cx, int cy,
                                           double cyaw, double syaw, double cpitch, double spitch,
                                           double fov, double depth) {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(0, 0, 0, 26));
            int half = 600;
            int step = 60;
            for (int i = -half; i <= half; i += step) {
                Point p1 = projHelper(cx, cy, -half, i, 0, cyaw, syaw, cpitch, spitch, fov, depth);
                Point p2 = projHelper(cx, cy,  half, i, 0, cyaw, syaw, cpitch, spitch, fov, depth);
                g2.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));

                Point p3 = projHelper(cx, cy, i, -half, 0, cyaw, syaw, cpitch, spitch, fov, depth);
                Point p4 = projHelper(cx, cy, i,  half, 0, cyaw, syaw, cpitch, spitch, fov, depth);
                g2.draw(new Line2D.Double(p3.x, p3.y, p4.x, p4.y));
            }
        }

        private static Point projHelper(int cx, int cy, double x, double y, double z,
                                        double cyaw, double syaw, double cpitch, double spitch,
                                        double fov, double depth) {
            double x1 =  cyaw * x + syaw * z;
            double y1 =  y;
            double z1 = -syaw * x + cyaw * z;

            double x2 = x1;
            double y2 =  cpitch * y1 - spitch * z1;
            double z2 =  spitch * y1 + cpitch * z1;

            double denom = (depth + z2);
            if (denom < 1) denom = 1;

            double sx = cx + (fov * x2) / denom;
            double sy = cy - (fov * y2) / denom;
            return new Point((int)Math.round(sx), (int)Math.round(sy));
        }

        private static void fillCircle(Graphics2D g2, Point p, int r) {
            g2.fillOval(p.x - r, p.y - r, r * 2, r * 2);
        }

        private static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}
