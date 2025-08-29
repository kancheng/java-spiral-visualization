# Java Spiral Collection (2D & 3D)

> 該專案為龍華科技大學資訊管理系 Java 課程 李銘城教授 的作業

用 **Sin/Cos** 與圓的特性繪製螺旋：

* 2D 螺旋（半徑逐圈縮小）
* 3D 錐形螺旋（半徑縮小 + 高度上升），含：

  * 基本版
  * 即時滑桿調整參數的 UI 版
  * 互動加強版（自動旋轉 + 滑鼠拖曳視角 + 滾輪縮放）

## 環境需求

* JDK 8 以上
* 桌面環境（Swing GUI）

## 快速開始

### 單檔編譯與執行

```bash
# 2D 螺旋
javac SpiralDemo.java
java SpiralDemo

# 3D 基本版
javac ConicalSpiral3D.java
java ConicalSpiral3D

# 3D UI 版（滑桿調整）
javac ConicalSpiral3DUI.java
java ConicalSpiral3DUI

# 3D 互動版（動畫 + 拖曳 + 滾輪）
javac ConicalSpiral3DInteractive.java
java ConicalSpiral3DInteractive
```

### 全部一起編譯

```bash
javac SpiralDemo.java ConicalSpiral3D.java ConicalSpiral3DUI.java ConicalSpiral3DInteractive.java
```

---

## 目錄

* [SpiralDemo.java（2D 螺旋）](#spiraldemojava2d-螺旋)
* [ConicalSpiral3D.java（3D 錐形螺旋・基本版）](#conicalspiral3djava3d-錐形螺旋基本版)
* [ConicalSpiral3DUI.java（3D 錐形螺旋・滑桿 UI）](#conicalspiral3duijava3d-錐形螺旋滑桿-ui)
* [ConicalSpiral3DInteractive.java（3D 錐形螺旋・互動加強版）](#conicalspiral3dinteractivejava3d-錐形螺旋互動加強版)

---

## SpiralDemo.java（2D 螺旋）

<details><summary>點此展開完整程式碼</summary>

```java
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
```

</details>

---

## ConicalSpiral3D.java（3D 錐形螺旋・基本版）

<details><summary>點此展開完整程式碼</summary>

```java
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
```

</details>

---

## ConicalSpiral3DUI.java（3D 錐形螺旋・滑桿 UI）

<details><summary>點此展開完整程式碼</summary>

```java
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
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setLayout(new BorderLayout());
        wrap.add(p, BorderLayout.CENTER);
        // 直接回傳 slider；上層以 controls.add(slider) 方式加入
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
```

</details>

---

## ConicalSpiral3DInteractive.java（3D 錐形螺旋・互動加強版）

<details><summary>點此展開完整程式碼</summary>

```java
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
```

</details>


