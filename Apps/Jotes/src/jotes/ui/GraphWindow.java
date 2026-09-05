package jotes.ui;

import jotes.db.FolderRepository;
import jotes.db.LinkRepository;
import jotes.db.NoteRepository;
import jotes.db.TagRepository;
import jotes.model.Folder;
import jotes.model.Note;
import jotes.ui.anim.Animations;
import jotes.ui.anim.Colors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Vista de grafo das ligações {@code [[wikilink]]} entre notas, desenhada com G2 e
 * acomodada por uma simulação de forças (repulsão entre nós, molas nas arestas e
 * gravidade para o centro) que corre a 60 FPS até estabilizar.
 * <p>Filtros por pasta, tag e notas órfãs; clicar num nó realça a sua vizinhança
 * e duplo-clique abre a nota.</p>
 */
public class GraphWindow extends JFrame {

    private static final String ALL_FOLDERS = "Todas as pastas";
    private static final String ALL_TAGS = "Todas as tags";

    private final NoteRepository noteRepo;
    private final LinkRepository linkRepo;
    private final FolderRepository folderRepo;
    private final TagRepository tagRepo;

    private final GraphCanvas canvas = new GraphCanvas();
    private final JComboBox<String> folderCombo = new JComboBox<>();
    private final JComboBox<String> tagCombo = new JComboBox<>();
    private final JCheckBox orphansOnly = new JCheckBox("Só órfãs");
    private final JLabel stats = new JLabel(" ");

    private final Map<Long, Folder> foldersById = new HashMap<>();
    private Consumer<Long> onOpenNote = id -> {};

    public GraphWindow(NoteRepository noteRepo, LinkRepository linkRepo,
                       FolderRepository folderRepo, TagRepository tagRepo) {
        super("Grafo de notas");
        this.noteRepo = noteRepo;
        this.linkRepo = linkRepo;
        this.folderRepo = folderRepo;
        this.tagRepo = tagRepo;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildToolbar(), BorderLayout.NORTH);
        root.add(canvas, BorderLayout.CENTER);
        setContentPane(root);

        reload();
    }

    /** Chamado com o id da nota ao fazer duplo-clique num nó. */
    public void setOnOpenNote(Consumer<Long> listener) {
        this.onOpenNote = listener == null ? id -> {} : listener;
    }

    // ---------------------------------------------------------------- toolbar

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(Theme.SIDEBAR_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR));

        folderCombo.setToolTipText("Filtrar por pasta");
        folderCombo.addActionListener(e -> reload());
        tagCombo.setToolTipText("Filtrar por tag");
        tagCombo.addActionListener(e -> reload());

        orphansOnly.setToolTipText("Mostrar só notas sem ligações");
        orphansOnly.setOpaque(false);
        orphansOnly.setForeground(Theme.TEXT);
        orphansOnly.setFont(Theme.font(Font.PLAIN, 13));
        orphansOnly.addActionListener(e -> reload());

        JButton relayout = new JButton("Reorganizar");
        Theme.styleButton(relayout);
        relayout.setToolTipText("Espalhar os nós outra vez e deixar assentar");
        relayout.addActionListener(e -> canvas.scatterAndSettle());

        JButton fit = new JButton("Enquadrar");
        Theme.styleButton(fit);
        fit.addActionListener(e -> canvas.fitToView());

        stats.setForeground(Theme.TEXT_DIM);
        stats.setFont(Theme.font(Font.PLAIN, 12));

        bar.add(new JLabel(Icons.themed(Icons.FOLDER, 14)));
        bar.add(folderCombo);
        bar.add(new JLabel(Icons.themed(Icons.TAG, 14)));
        bar.add(tagCombo);
        bar.add(orphansOnly);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(relayout);
        bar.add(fit);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(stats);
        return bar;
    }

    /** Recarrega notas e ligações aplicando os filtros ativos. */
    private void reload() {
        try {
            refreshFilterCombos();

            Long folderId = null;
            Object f = folderCombo.getSelectedItem();
            if (f != null && !ALL_FOLDERS.equals(f)) {
                for (Folder folder : foldersById.values()) {
                    if (folder.getName().equals(f)) { folderId = folder.getId(); break; }
                }
            }
            Object t = tagCombo.getSelectedItem();
            String tag = (t == null || ALL_TAGS.equals(t)) ? null : t.toString();

            List<Note> notes = noteRepo.list(NoteRepository.Query.all().withFolder(folderId).withTag(tag));
            Set<Long> visible = new HashSet<>();
            for (Note n : notes) visible.add(n.getId());

            List<long[]> links = new ArrayList<>();
            Set<Long> connected = new HashSet<>();
            for (long[] link : linkRepo.allLinks()) {
                if (visible.contains(link[0]) && visible.contains(link[1]) && link[0] != link[1]) {
                    links.add(link);
                    connected.add(link[0]);
                    connected.add(link[1]);
                }
            }

            if (orphansOnly.isSelected()) {
                notes.removeIf(n -> connected.contains(n.getId()));
                links.clear();
            }

            canvas.setGraph(notes, links);
            stats.setText(notes.size() + " notas · " + links.size() + " ligações");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Enche os combos de pasta e tag preservando a seleção (sem disparar reload). */
    private void refreshFilterCombos() throws Exception {
        Object keepFolder = folderCombo.getSelectedItem();
        Object keepTag = tagCombo.getSelectedItem();

        List<String> folderNames = new ArrayList<>();
        foldersById.clear();
        for (Folder folder : folderRepo.list()) {
            foldersById.put(folder.getId(), folder);
            folderNames.add(folder.getName());
        }
        fill(folderCombo, ALL_FOLDERS, folderNames, keepFolder);
        fill(tagCombo, ALL_TAGS, tagRepo.listAll(), keepTag);
    }

    private void fill(JComboBox<String> combo, String allLabel, List<String> values, Object keep) {
        List<String> current = new ArrayList<>();
        for (int i = 1; i < combo.getItemCount(); i++) current.add(combo.getItemAt(i));
        if (combo.getItemCount() > 0 && current.equals(values)) return;

        var listeners = combo.getActionListeners();
        for (var l : listeners) combo.removeActionListener(l);
        combo.removeAllItems();
        combo.addItem(allLabel);
        for (String v : values) combo.addItem(v);
        if (keep != null && values.contains(keep.toString())) combo.setSelectedItem(keep);
        else combo.setSelectedIndex(0);
        for (var l : listeners) combo.addActionListener(l);
    }

    // ---------------------------------------------------------------- canvas

    /** Nó da simulação: posição, velocidade e grau (usado no raio e na cor). */
    private static final class Node {
        final Note note;
        double x, y, vx, vy;
        int degree;
        float highlight;

        Node(Note note, double x, double y) {
            this.note = note;
            this.x = x;
            this.y = y;
        }

        double radius() {
            return 7 + Math.min(12, degree * 1.8);
        }
    }

    /**
     * Painel do grafo: simulação de forças com {@link Timer} a 60 FPS, pan por
     * arrasto do fundo, zoom com a roda e nós arrastáveis.
     */
    private final class GraphCanvas extends JComponent {
        private static final int FPS_MS = 1000 / 60;
        /** Abaixo desta energia total a simulação pára (evita gastar CPU parada). */
        private static final double SETTLE_ENERGY = 0.05;

        private final List<Node> nodes = new ArrayList<>();
        private final List<long[]> edges = new ArrayList<>();
        private final Map<Long, Node> byId = new HashMap<>();
        private final Timer sim = new Timer(FPS_MS, e -> step());

        private double zoom = 1;
        private double panX;
        private double panY;
        private Node dragging;
        private Point lastMouse;
        private Node selectedNode;
        private Set<Long> neighbourhood = Set.of();

        GraphCanvas() {
            setBackground(Theme.BG);
            setOpaque(true);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMouse = e.getPoint();
                    dragging = nodeAt(e.getPoint());
                    if (dragging != null) sim.start();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = null;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (lastMouse == null) return;
                    double dx = (e.getX() - lastMouse.x) / zoom;
                    double dy = (e.getY() - lastMouse.y) / zoom;
                    if (dragging != null) {
                        dragging.x += dx;
                        dragging.y += dy;
                        dragging.vx = 0;
                        dragging.vy = 0;
                    } else {
                        panX += dx * zoom;
                        panY += dy * zoom;
                    }
                    lastMouse = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    Node hit = nodeAt(e.getPoint());
                    if (hit == null) {
                        select(null);
                        return;
                    }
                    if (e.getClickCount() >= 2) onOpenNote.accept(hit.note.getId());
                    else select(hit);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(
                            nodeAt(e.getPoint()) != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    // zoom centrado no cursor
                    double factor = Math.pow(1.1, -e.getPreciseWheelRotation());
                    double newZoom = Math.max(0.15, Math.min(4, zoom * factor));
                    double wx = (e.getX() - panX) / zoom;
                    double wy = (e.getY() - panY) / zoom;
                    zoom = newZoom;
                    panX = e.getX() - wx * zoom;
                    panY = e.getY() - wy * zoom;
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
        }

        void setGraph(List<Note> notes, List<long[]> links) {
            sim.stop();
            nodes.clear();
            byId.clear();
            edges.clear();
            edges.addAll(links);

            // arranque em espiral: distribuição inicial estável, sem sobreposições
            double golden = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < notes.size(); i++) {
                double r = 14 * Math.sqrt(i + 1);
                Node n = new Node(notes.get(i), Math.cos(golden * i) * r, Math.sin(golden * i) * r);
                nodes.add(n);
                byId.put(n.note.getId(), n);
            }
            for (long[] e : edges) {
                Node a = byId.get(e[0]);
                Node b = byId.get(e[1]);
                if (a != null) a.degree++;
                if (b != null) b.degree++;
            }
            select(null);
            fitToView();
            if (Animations.isEnabled()) sim.start();
            else settleInstantly();
            repaint();
        }

        void select(Node n) {
            selectedNode = n;
            if (n == null) {
                neighbourhood = Set.of();
            } else {
                Set<Long> near = new HashSet<>();
                near.add(n.note.getId());
                for (long[] e : edges) {
                    if (e[0] == n.note.getId()) near.add(e[1]);
                    if (e[1] == n.note.getId()) near.add(e[0]);
                }
                neighbourhood = near;
            }
            repaint();
        }

        /** Volta a espalhar os nós e reinicia a simulação. */
        void scatterAndSettle() {
            for (Node n : nodes) {
                n.x += (Math.random() - 0.5) * 200;
                n.y += (Math.random() - 0.5) * 200;
                n.vx = n.vy = 0;
            }
            if (Animations.isEnabled()) sim.start();
            else settleInstantly();
            repaint();
        }

        /** Sem animações: corre a simulação sem pintar até assentar. */
        private void settleInstantly() {
            for (int i = 0; i < 400 && stepOnce() > SETTLE_ENERGY; i++) {
                // corre em silêncio; o repaint acontece no fim
            }
            fitToView();
        }

        /** Ajusta zoom e pan para todo o grafo caber na janela. */
        void fitToView() {
            if (nodes.isEmpty()) {
                zoom = 1;
                panX = getWidth() / 2.0;
                panY = getHeight() / 2.0;
                repaint();
                return;
            }
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (Node n : nodes) {
                minX = Math.min(minX, n.x);
                maxX = Math.max(maxX, n.x);
                minY = Math.min(minY, n.y);
                maxY = Math.max(maxY, n.y);
            }
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            double spanX = Math.max(1, maxX - minX) + 120;
            double spanY = Math.max(1, maxY - minY) + 120;
            zoom = Math.max(0.15, Math.min(2, Math.min(w / spanX, h / spanY)));
            panX = w / 2.0 - (minX + maxX) / 2 * zoom;
            panY = h / 2.0 - (minY + maxY) / 2 * zoom;
            repaint();
        }

        private void step() {
            double energy = stepOnce();
            repaint();
            if (energy < SETTLE_ENERGY && dragging == null) sim.stop();
        }

        /**
         * Um passo da simulação. Devolve a energia cinética total, usada para
         * decidir quando o grafo está acomodado.
         */
        private double stepOnce() {
            final double repulsion = 9000;
            final double springLength = 90;
            final double springK = 0.02;
            final double gravity = 0.012;
            final double damping = 0.85;

            for (int i = 0; i < nodes.size(); i++) {
                Node a = nodes.get(i);
                for (int j = i + 1; j < nodes.size(); j++) {
                    Node b = nodes.get(j);
                    double dx = b.x - a.x;
                    double dy = b.y - a.y;
                    double d2 = Math.max(25, dx * dx + dy * dy);
                    double d = Math.sqrt(d2);
                    double force = repulsion / d2;
                    double fx = force * dx / d;
                    double fy = force * dy / d;
                    a.vx -= fx;
                    a.vy -= fy;
                    b.vx += fx;
                    b.vy += fy;
                }
            }

            for (long[] e : edges) {
                Node a = byId.get(e[0]);
                Node b = byId.get(e[1]);
                if (a == null || b == null) continue;
                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double d = Math.max(1, Math.hypot(dx, dy));
                double force = (d - springLength) * springK;
                double fx = force * dx / d;
                double fy = force * dy / d;
                a.vx += fx;
                a.vy += fy;
                b.vx -= fx;
                b.vy -= fy;
            }

            double energy = 0;
            for (Node n : nodes) {
                if (n == dragging) {
                    n.vx = n.vy = 0;
                    continue;
                }
                n.vx = (n.vx - n.x * gravity) * damping;
                n.vy = (n.vy - n.y * gravity) * damping;
                // limite de velocidade: evita nós a "explodir" em grafos densos
                double speed = Math.hypot(n.vx, n.vy);
                if (speed > 20) {
                    n.vx = n.vx / speed * 20;
                    n.vy = n.vy / speed * 20;
                }
                n.x += n.vx;
                n.y += n.vy;
                energy += n.vx * n.vx + n.vy * n.vy;
            }
            return nodes.isEmpty() ? 0 : energy / nodes.size();
        }

        private Node nodeAt(Point p) {
            for (int i = nodes.size() - 1; i >= 0; i--) {
                Node n = nodes.get(i);
                Point2D screen = toScreen(n);
                if (screen.distance(p.x, p.y) <= n.radius() * zoom + 4) return n;
            }
            return null;
        }

        private Point2D toScreen(Node n) {
            return new Point2D.Double(panX + n.x * zoom, panY + n.y * zoom);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(900, 640);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (nodes.isEmpty()) {
                g2.setColor(Theme.TEXT_DIM);
                g2.setFont(Theme.font(Font.PLAIN, 14));
                String msg = "Sem notas para mostrar com estes filtros";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                g2.dispose();
                return;
            }

            boolean focusing = selectedNode != null;

            // arestas
            for (long[] e : edges) {
                Node a = byId.get(e[0]);
                Node b = byId.get(e[1]);
                if (a == null || b == null) continue;
                boolean near = !focusing
                        || (neighbourhood.contains(a.note.getId()) && neighbourhood.contains(b.note.getId()));
                g2.setColor(near ? Theme.SEPARATOR : fade(Theme.SEPARATOR, 0.25f));
                Point2D pa = toScreen(a);
                Point2D pb = toScreen(b);
                g2.draw(new Line2D.Double(pa.getX(), pa.getY(), pb.getX(), pb.getY()));
            }

            // nós
            g2.setFont(Theme.font(Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            for (Node n : nodes) {
                boolean near = !focusing || neighbourhood.contains(n.note.getId());
                Point2D p = toScreen(n);
                double r = n.radius() * zoom;

                Color fill = n == selectedNode ? Theme.ACCENT
                        : Colors.lerp(Theme.CARD_HOVER, Theme.ACCENT, Math.min(1f, n.degree / 6f));
                g2.setColor(near ? fill : fade(fill, 0.2f));
                g2.fill(new Ellipse2D.Double(p.getX() - r, p.getY() - r, r * 2, r * 2));

                if (zoom > 0.4) {
                    String label = n.note.displayTitle();
                    if (label.length() > 28) label = label.substring(0, 27) + "…";
                    g2.setColor(near ? Theme.TEXT : fade(Theme.TEXT, 0.2f));
                    g2.drawString(label, (float) (p.getX() - fm.stringWidth(label) / 2.0),
                            (float) (p.getY() + r + fm.getAscent() + 3));
                }
            }
            g2.dispose();
        }

        private Color fade(Color c, float alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(255 * alpha));
        }
    }
}
