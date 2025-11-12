import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

class ParticulaExplosao {
    double x, y, vx, vy, vz;   // posição e profundidade
    double life;
    double angle, rotationSpeed; // rotação para efeito direcional
    double tamanhoBase;
    Color color;
    float brilho;

    public ParticulaExplosao(double x, double y) {
        this.x = x;
        this.y = y;
        this.life = 1.0;
        Random r = new Random();

        // Direção e velocidade base
        double direction = r.nextDouble() * 2 * Math.PI;
        double speed = 0.8 + r.nextDouble() * 3.2;
        this.vx = Math.cos(direction) * speed;
        this.vy = Math.sin(direction) * speed;
        this.vz = (r.nextDouble() - 0.5) * 2; // profundidade 3D

        // 🎨 Cores quentes (fogo)
        float c = r.nextFloat();
        if (c < 0.33f)
            this.color = new Color(255, 240, 80);   // amarelo intenso
        else if (c < 0.66f)
            this.color = new Color(255, 140, 0);    // laranja
        else
            this.color = new Color(255, 50, 0);     // vermelho vivo

        this.tamanhoBase = 1.5 + r.nextDouble() * 2.5;
        this.rotationSpeed = (r.nextDouble() - 0.5) * 0.25; // rotação suave
        this.angle = r.nextDouble() * 2 * Math.PI;
        this.brilho = 0.6f + r.nextFloat() * 0.4f;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.04;  // gravidade
        vz += 0.02 * (vz > 0 ? -1 : 1);
        life -= 0.015;
        angle += rotationSpeed;
        if (life < 0) life = 0;
    }

    public boolean isAlive() {
        return life > 0;
    }

    public double getZ() {
        return vz;
    }

    public double getSize() {
        // profundidade afeta tamanho
        double depthScale = 1.0 + vz * 0.4;
        return Math.max(1, tamanhoBase * depthScale);
    }

    public float getBrightness() {
        // brilho varia com ângulo e profundidade
        return (float) Math.min(1.0, brilho + 0.3 * Math.sin(angle) + vz * 0.2);
    }
}

public class EfeitoExplosao {
    private final List<ParticulaExplosao> particulas;

    public EfeitoExplosao(int x, int y, int count) {
        particulas = new ArrayList<>();
        int total = Math.max(count, 200); // densidade maior
        for (int i = 0; i < total; i++) {
            particulas.add(new ParticulaExplosao(x, y));
        }
    }

    public void update() {
        particulas.forEach(ParticulaExplosao::update);
        particulas.removeIf(p -> !p.isAlive());
    }

    public void draw(Graphics2D g2d) {
        // Ordena por profundidade (distantes primeiro)
        particulas.sort(Comparator.comparingDouble(ParticulaExplosao::getZ));

        for (ParticulaExplosao p : particulas) {
            int alpha = (int) (255 * p.life);
            int size = (int) p.getSize();
            float bright = p.getBrightness();

            // Transição de cor conforme esfria (fogo realista)
            float fade = (float) (1 - p.life);
            Color base = p.color;
            Color atual = new Color(
                (int) (base.getRed() * (1 - fade) + 100 * fade),
                (int) (base.getGreen() * (1 - fade) + 80 * fade),
                (int) (base.getBlue() * (1 - fade) + 80 * fade),
                alpha
            );

            // 💡 Halo radial com brilho variável
            Paint oldPaint = g2d.getPaint();
            float radius = size * (float) (3.0 + p.getZ());
            float cx = (float) p.x + size / 2f;
            float cy = (float) p.y + size / 2f;

            // brilho direcional (rotaciona luz)
            float brilhoDirecional = (float) (0.5 + 0.5 * Math.sin(p.angle * 2));
            Color center = new Color(
                (int) Math.min(255, atual.getRed() + 80 * brilhoDirecional),
                (int) Math.min(255, atual.getGreen() + 60 * brilhoDirecional),
                (int) Math.min(255, atual.getBlue() + 30 * brilhoDirecional),
                (int) (200 * bright * p.life)
            );
            Color edge = new Color(atual.getRed(), atual.getGreen(), atual.getBlue(), 0);

            RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float(cx, cy),
                radius,
                new float[]{0f, 1f},
                new Color[]{center, edge}
            );

            g2d.setPaint(paint);
            g2d.fillOval((int) (p.x - size), (int) (p.y - size), size * 3, size * 3);

            // ✨ Faísca central com reflexo direcional
            g2d.setPaint(oldPaint);
            g2d.setColor(new Color(
                Math.min(255, center.getRed() + 30),
                Math.min(255, center.getGreen() + 30),
                Math.min(255, center.getBlue() + 30),
                alpha
            ));

            // Leve rotação para simular faíscas "girando"
            int cxSpark = (int) (p.x + Math.cos(p.angle) * size / 3);
            int cySpark = (int) (p.y + Math.sin(p.angle) * size / 3);
            g2d.fillOval(cxSpark, cySpark, size, size);
        }
    }

    public boolean isDone() {
        return particulas.isEmpty();
    }
}
