import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;

public class ConicalSpiral3DInteractive extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConicalSpiral3DInteractive ui = new ConicalSpiral3DInteractive();
            ui.setVisible(true);
        });
    }

    private final SpiralPanel canvas;
    private final JSlider turnsSlider, decaySlider, liftSlider, yawSlider, pitchSlider, endRatioSlider;
    private final JCheckBox archCheck, autoRotateCheck;
    private final JSlider dThetaSlider, fovSlider, depthSlider, speedSlider;
    private Timer rotTimer;

    public ConicalSpiral3DInteractive() {
        super("3D Conical Spiral · Animation + Mouse Drag");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.canvas = new SpiralPanel();

        // 右側控制面板
        JPanel controls = new JPanel();
        controls.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        archCheck = new JCheckBox("Archimedean (線性縮小半徑)");
        archCheck.setSelected(false);

        turnsSlider = labeledSlider("圈數 (turns)", 1, 12, canvas.getTurns());
        decaySlider = labeledSlider("每圈縮小比例 decay/turn (%)", 50, 95, (int)Math.round(canvas.getDecayPerTurn()*100));
        endRatioSlider = labeledSlider("Archimedean 終端半徑比例 (%)", 2, 30, 8);
        liftSlider = labeledSlider("每弧度上升高度 lift/rad (×0.1)", 0, 50, (int)Math.round(canvas.getLiftPerRad()*5));
        yawSlider   = labeledSlider("Yaw (°)",   -120, 120, (int)Math.round(canvas.getYawDeg()));
        pitchSlider = labeledSlider("Pitch (°)",  -20,  80, (int)Math.round(canvas.getPitchDeg()));
        dThetaSlider = labeledSlider("細緻度 dTheta (×0.001)", 1, 30, 10);
        fovSlider    = labeledSlider("FOV 焦距", 300, 1400, (int)Math.round(canvas.getFov()));
        depthSlider  = labeledSlider("Depth 偏移", 100, 900, (int)Math.round(canvas.getDepth()));

        // 動畫控制
        autoRotateCheck = new JCheckBox("自動旋轉");
        speedSlider = labeledSlider("旋轉速度 (度/秒)", -90, 90, 20); // 負值 = 反向

        JLabel hint = new JLabel("<html><body style='width:240px'>滑鼠操作：<br/>" +
                "・拖曳畫布：改變 Yaw/Pitch<br/>" +
                "・滾輪：縮放 FOV（遠近感）<br/>" +
                "提示：dTheta 越小越平滑但較耗效能</body></html>");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 逐項加入
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
        controls.add(autoRotateCheck);
        controls.add(speedSlider);
        controls.add(Box.createVerticalStrut(8));
        controls.add(hint);

        // 綁定變更事件
        ChangeListener repaintOnChange = e -> {
            canvas.setArchimedean(archCheck.isSelected());
            canvas.setTurns(turnsSlider.getValue());
            canvas.setDecayPerTurn(decaySlider.getValue() / 100.0);
            canvas.setEndRatio(endRatioSlider.getValue() / 100.0);
            canvas.setLiftPerRad(liftSlider.getValue() / 10.0);
            canvas.setYawDeg(yawSlider.getValue());
            canvas.setPitchDeg(pitchSlider.getValue());
            canvas.setDTheta(dThetaSlider.getValue() / 1000.0);
            canvas.setFov(fovSlider.getValue());
            canvas.setDepth(depthSlider.getValue());
            canvas.repaint();
        };

        archCheck.addChangeListener(repaintOnChange);
        for (JSlider s : new JSlider[]{turnsSlider, decaySlider, endRatioSlider, liftSlider, yawSlider, pitchSlider, dThetaSlider, fovSlider, depthSlider}) {
            s.addChangeListener(repaintOnChange);
        }

        // 自動旋轉 Timer（約 60 FPS）
        rotTimer = new Timer(16, evt -> {
            double degPerSec = speedSlider.getValue();
            double deltaDeg = degPerSec * (16.0 / 1000.0);
            canvas.setYawDeg(canvas.getYawDeg() + deltaDeg);
            yawSlider.setValue((int)Math.round(canvas.getYawDeg()));
            canvas.repaint();
        });

        autoRotateCheck.addActionListener(e -> {
            if (autoRotateCheck.isSelected()) {
                rotTimer.start();
            } else {
                rotTimer.stop();
            }
        });

        // 畫布滑鼠互動
        canvas.enableMouseControl(yawSlider, pitchSlider, fovSlider);

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
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(p, BorderLayout.CENTER);
        return s;
    }

    // ===================== 畫布與數學 =====================
    static class SpiralPanel extends JPanel {
        private int turns = 6;
        private boolean archimedean = false;
        private double decayPerTurn = 0.75;
        private double endRatio = 0.08;   // r_end = r0*endRatio
        private double liftPerRad = 2.0;
        private double yawDeg = 35.0;
        private double pitchDeg = 25.0;
        private double dTheta = 0.01;
        private double fov = 850.0;
        private double depth = 300.0;

        SpiralPanel() {
            setBackground(Color.white);
        }

        // 對外 getters
        public int getTurns() { return turns; }
        public double getDecayPerTurn() { return decayPerTurn; }
        public double getLiftPerRad() { return liftPerRad; }
        public double getYawDeg() { return yawDeg; }
        public double getPitchDeg() { return pitchDeg; }
        public double getFov() { return fov; }
        public double getDepth() { return depth; }

        // 對外 setters
        public void setTurns(int t) { this.turns = Math.max(1, t); }
        public void setArchimedean(boolean b) { this.archimedean = b; }
        public void setDecayPerTurn(double d) { this.decayPerTurn = clamp(d, 0.5, 0.98); }
        public void setEndRatio(double r) { this.endRatio = clamp(r, 0.02, 0.5); }
        public void setLiftPerRad(double l) { this.liftPerRad = clamp(l, 0, 8.0); }
        public void setYawDeg(double d) { this.yawDeg = d; }
        public void setPitchDeg(double d) { this.pitchDeg = clamp(d, -20, 80); }
        public void setDTheta(double dt) { this.dTheta = clamp(dt, 0.001, 0.05); }
        public void setFov(double f) { this.fov = clamp(f, 200, 2000); }
        public void setDepth(double d) { this.depth = clamp(d, 50, 2000); }

        // 啟用滑鼠控制（拖曳調整視角、滾輪縮放 FOV）
        public void enableMouseControl(JSlider yawSlider, JSlider pitchSlider, JSlider fovSlider) {
            MouseAdapter ma = new MouseAdapter() {
                Point last;
                @Override public void mousePressed(MouseEvent e) { last = e.getPoint(); requestFocusInWindow(); }
                @Override public void mouseDragged(MouseEvent e) {
                    if (last == null) { last = e.getPoint(); return; }
                    int dx = e.getX() - last.x;
                    int dy = e.getY() - last.y;
                    setYawDeg(getYawDeg() + dx * 0.4);
                    setPitchDeg(getPitchDeg() - dy * 0.4);
                    if (yawSlider != null) yawSlider.setValue((int)Math.round(getYawDeg()));
                    if (pitchSlider != null) pitchSlider.setValue((int)Math.round(getPitchDeg()));
                    last = e.getPoint();
                    repaint();
                }
                @Override public void mouseReleased(MouseEvent e) { last = null; }
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    double step = 30.0;
                    if (e.getPreciseWheelRotation() < 0) setFov(getFov() + step);
                    else setFov(getFov() - step);
                    if (fovSlider != null) fovSlider.setValue((int)Math.round(getFov()));
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
            setFocusable(true);
        }

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

                float t = (float) clamp((p.zCam - 0) / 800.0, 0, 1);
                float alpha = (float) (0.25 + 0.55 * (1 - t));
                float gray  = (float) (0.25 + 0.65 * (1 - t));
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
                // 阿基米德：線性縮小至 r_end
                double rEnd = r0 * endRatio;
                double k = (r0 - rEnd) / thetaMax;
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
