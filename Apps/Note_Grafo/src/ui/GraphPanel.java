package ui;

import model.Edge;
import model.GraphData;
import model.Node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * GraphPanel modernizado com design profissional
 */
public class GraphPanel extends JPanel {
    private final List<NodeUI> nodes = new ArrayList<>();
    private final List<EdgeUI> edges = new ArrayList<>();

    private final Timer animTimer;
    private boolean layoutRunning = true;

    // interaction
    private NodeUI draggedNode = null;
    private Point dragOffset = null;
    private NodeUI selectedForLink = null;
    private Mode mode = Mode.NORMAL;
    private NodeUI hoveredNode = null;

    // transform
    public double scale = 1.0;
    public double translateX = 0;
    public double translateY = 0;
    private Point lastPanPoint = null;

    private boolean darkMode = false;
    private double edgePhase = 0;

    // persistence
    private File currentFile = null;

    // snapshots for undo/redo
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public enum Mode { NORMAL, ADD_NODE, LINKING }

    public GraphPanel() {
        setBackground(new Color(250, 251, 252));
        setFocusable(true);
        setLayout(null);

        // sample nodes com cores modernas
        nodes.add(new NodeUI(300, 180, "Bem-vindo", "", new Color(99, 110, 250), "#636EFA"));
        nodes.add(new NodeUI(550, 250, "Ideias", "", new Color(239, 85, 59), "#EF553B"));
        nodes.add(new NodeUI(450, 400, "Projetos", "", new Color(0, 204, 150), "#00CC96"));
        edges.add(new EdgeUI(nodes.get(0), nodes.get(1)));
        edges.add(new EdgeUI(nodes.get(0), nodes.get(2)));

        animTimer = new Timer(30, e -> {
            edgePhase += 0.015;
            if (edgePhase > 1e9) edgePhase = 0;
            if (layoutRunning) stepLayout();
            repaint();
        });
        animTimer.start();

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseMotionAdapter);
        addMouseWheelListener(mouseWheelListener);

        setupKeyBindings();
        pushSnapshot();
    }

    // -------------------------
    // Snapshot management (mantido igual)
    // -------------------------
    public void pushSnapshot() {
        try {
            GraphData gd = toGraphData();
            String snap = gson.toJson(gd);
            undoStack.push(snap);
            redoStack.clear();
            if (undoStack.size() > 100) {
                ArrayDeque<String> tmp = new ArrayDeque<>(undoStack);
                while (tmp.size() > 100) tmp.removeLast();
                undoStack.clear();
                undoStack.addAll(tmp);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void undo() {
        if (undoStack.size() <= 1) return;
        try {
            String current = undoStack.pop();
            redoStack.push(current);
            String prev = undoStack.peek();
            if (prev != null) {
                restoreFromSnapshot(prev);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        try {
            String snap = redoStack.pop();
            undoStack.push(snap);
            restoreFromSnapshot(snap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void restoreFromSnapshot(String snap) {
        try {
            GraphData gd = gson.fromJson(snap, GraphData.class);
            nodes.clear();
            edges.clear();
            if (gd != null && gd.nodes != null) {
                for (Node n : gd.nodes) {
                    Color c = parseHex(n.colorHex, new Color(99, 110, 250));
                    nodes.add(new NodeUI(n.x, n.y, n.label, n.note, c, n.colorHex));
                }
                if (gd.edges != null) {
                    for (Edge ed : gd.edges) {
                        if (ed.a >=0 && ed.a < nodes.size() && ed.b >=0 && ed.b < nodes.size()) {
                            edges.add(new EdgeUI(nodes.get(ed.a), nodes.get(ed.b)));
                        }
                    }
                }
            }
            repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private GraphData toGraphData() {
        GraphData gd = new GraphData();
        for (NodeUI nu : nodes) gd.nodes.add(new Node(nu.x, nu.y, nu.label, nu.note, nu.colorHex));
        for (EdgeUI e : edges) {
            int a = nodes.indexOf(e.a), b = nodes.indexOf(e.b);
            if (a >=0 && b >=0) gd.edges.add(new Edge(a,b));
        }
        return gd;
    }

    // -------------------------
    // Public API
    // -------------------------
    public void addNode(NodeUI n) { nodes.add(n); repaint(); }
    public void addEdge(NodeUI a, NodeUI b) { edges.add(new EdgeUI(a,b)); repaint(); }
    public void openNodeEditor(NodeUI n) { openNoteDialog(n); }
    public void startAddingNodeMode() { 
        mode = Mode.ADD_NODE; 
        selectedForLink=null; 
        showModernNotification("Clique para adicionar um novo nó", new Color(52, 152, 219));
    }
    public void startLinkingMode() { 
        mode = Mode.LINKING; 
        selectedForLink=null; 
        showModernNotification("Selecione dois nós para criar ligação", new Color(46, 204, 113));
    }
    public void toggleLayoutRunning() { layoutRunning = !layoutRunning; }
    public void clearGraph() { 
        int result = JOptionPane.showConfirmDialog(this, 
            "Tem certeza que deseja limpar todo o grafo?", 
            "Confirmar", 
            JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            pushSnapshot(); 
            nodes.clear(); 
            edges.clear(); 
            repaint(); 
        }
    }
    public void setDarkMode(boolean d) { 
        darkMode = d; 
        setBackground(darkMode ? new Color(30, 30, 35) : new Color(250, 251, 252));
        repaint(); 
    }
    public boolean isDarkMode() { return darkMode; }
    public void resetZoomAndPan() { scale = 1.0; translateX = translateY = 0; repaint(); }

    private void showModernNotification(String message, Color color) {
        JLabel notif = new JLabel(message);
        notif.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        notif.setForeground(Color.WHITE);
        notif.setBackground(color);
        notif.setOpaque(true);
        notif.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        JOptionPane pane = new JOptionPane(notif, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        JDialog dialog = pane.createDialog(this, "");
        dialog.setModal(false);
        dialog.setVisible(true);
        
        Timer timer = new Timer(2000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    // -------------------------
    // Save / Load / Merge (mantido similar)
    // -------------------------
    public void saveAs(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Graph Notes (*.gn)", "gn"));
        if (fc.showSaveDialog(parent==null?this:parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".gn")) f = new File(f.getAbsolutePath()+".gn");
        currentFile = f;
        saveToFile(parent);
    }

    public void saveToFile(Component parent) {
        if (currentFile == null) { saveAs(parent); return; }
        try (FileWriter fw = new FileWriter(currentFile)) {
            GraphData gd = toGraphData();
            gson.toJson(gd, fw);
            showModernNotification("Guardado: " + currentFile.getName(), new Color(46, 204, 113));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao guardar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadFromFile(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Graph Notes (*.gn)", "gn"));
        if (fc.showOpenDialog(parent==null?this:parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            String txt = new String(Files.readAllBytes(Paths.get(f.toURI())));
            GraphData gd = gson.fromJson(txt, GraphData.class);
            pushSnapshot();
            nodes.clear(); edges.clear();
            if (gd != null && gd.nodes != null) {
                for (Node n : gd.nodes) nodes.add(new NodeUI(n.x, n.y, n.label, n.note, parseHex(n.colorHex, new Color(99, 110, 250)), n.colorHex));
                if (gd.edges != null) {
                    for (Edge ed : gd.edges) {
                        if (ed.a >=0 && ed.a < nodes.size() && ed.b >=0 && ed.b < nodes.size()) {
                            edges.add(new EdgeUI(nodes.get(ed.a), nodes.get(ed.b)));
                        }
                    }
                }
            }
            currentFile = f;
            pushSnapshot();
            repaint();
            showModernNotification("Carregado: " + f.getName(), new Color(52, 152, 219));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void mergeFromFile(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Graph Notes (*.gn)", "gn"));
        if (fc.showOpenDialog(parent==null?this:parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            String txt = new String(Files.readAllBytes(Paths.get(f.toURI())));
            GraphData gd = gson.fromJson(txt, GraphData.class);
            if (gd == null) return;
            pushSnapshot();
            int base = nodes.size();
            for (Node n : gd.nodes) {
                Color c = parseHex(n.colorHex, new Color(99, 110, 250));
                nodes.add(new NodeUI(n.x + 40, n.y + 40, n.label, n.note, c, n.colorHex));
            }
            if (gd.edges != null) {
                for (Edge ed : gd.edges) {
                    int a = ed.a + base;
                    int b = ed.b + base;
                    if (a >=0 && a < nodes.size() && b >=0 && b < nodes.size()) {
                        edges.add(new EdgeUI(nodes.get(a), nodes.get(b)));
                    }
                }
            }
            pushSnapshot();
            repaint();
            showModernNotification("Mesclado: " + f.getName(), new Color(155, 89, 182));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao mesclar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportImage(Component parent, String format) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(format.toUpperCase() + " image", format.toLowerCase()));
        if (fc.showSaveDialog(parent==null?this:parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + format.toLowerCase())) 
            f = new File(f.getAbsolutePath() + "." + format.toLowerCase());

        try {
            BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            if (darkMode) g.setColor(new Color(30, 30, 35)); 
            else g.setColor(new Color(250, 251, 252));
            g.fillRect(0,0,getWidth(), getHeight());
            
            AffineTransform at = new AffineTransform();
            at.translate(translateX, translateY);
            at.scale(scale, scale);
            g.transform(at);
            g.setStroke(new BasicStroke((float)(2.0/scale)));
            
            for (EdgeUI e : edges) drawModernEdge(g, e, edgePhase);
            for (NodeUI n : nodes) n.draw(g, false, scale, darkMode);
            
            ImageIO.write(img, format, f);
            g.dispose();
            showModernNotification("Imagem exportada: " + f.getName(), new Color(52, 152, 219));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------
    // Runtime classes - NodeUI Modernizado
    // -------------------------
    public static class NodeUI {
        public double x,y;
        public double vx=0, vy=0;
        public double fx=0, fy=0;
        public boolean fixed=false;
        public String label;
        public String note = "";
        public Color color = new Color(99, 110, 250);
        public String colorHex = "#636EFA";
        public final int baseSize = 70;

        public NodeUI(double x, double y, String label, String note, Color color, String colorHex) {
            this.x = x; this.y = y; this.label = label; this.note = note; 
            this.color = color; this.colorHex = colorHex;
        }

        public boolean contains(double px, double py) {
            double r = (baseSize/2.0);
            return (px-x)*(px-x) + (py-y)*(py-y) <= r*r;
        }

        public void draw(Graphics2D g2, boolean highlight, double scale, boolean darkMode) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int size = Math.max(50, (int)(baseSize*scale));
            double rx = x - size/2.0, ry = y - size/2.0;
            
            // Sombra moderna suave
            Composite oldComp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
            g2.setColor(new Color(0, 0, 0));
            g2.fill(new Ellipse2D.Double(rx+6, ry+6, size, size));
            g2.setComposite(oldComp);

            // Círculo com gradiente moderno
            GradientPaint gp = new GradientPaint(
                (float)rx, (float)ry, color,
                (float)(rx+size), (float)(ry+size), color.darker()
            );
            g2.setPaint(gp);
            g2.fill(new Ellipse2D.Double(rx, ry, size, size));

            // Borda elegante
            if (highlight) {
                g2.setColor(new Color(255, 215, 0));
                g2.setStroke(new BasicStroke(Math.max(3f,(float)(4.0/scale))));
            } else {
                g2.setColor(darkMode ? color.brighter() : color.darker());
                g2.setStroke(new BasicStroke(Math.max(2f,(float)(2.5/scale))));
            }
            g2.draw(new Ellipse2D.Double(rx, ry, size, size));

            // Label moderno
            Font orig = g2.getFont();
            Font f = new Font("Segoe UI", Font.BOLD, Math.max(11,(int)(13*scale)));
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String text = label==null?"":label;
            int tw = fm.stringWidth(text);
            int th = fm.getHeight();
            
            double bx = x - tw/2.0 - 10;
            double by = y + size/2.0 + 12;
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
            g2.setColor(darkMode ? new Color(45, 45, 48, 240) : new Color(255, 255, 255, 245));
            RoundRectangle2D rr = new RoundRectangle2D.Double(bx, by - th + 6, tw + 20, th + 4, 12, 12);
            g2.fill(rr);
            
            g2.setColor(darkMode ? new Color(80, 80, 85) : new Color(220, 220, 225));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(rr);
            
            g2.setComposite(oldComp);
            g2.setColor(darkMode ? Color.WHITE : new Color(30, 30, 35));
            g2.drawString(text, (float)(x - tw/2.0), (float)(by + 2));
            g2.setFont(orig);
        }
    }

    private static class EdgeUI {
        public NodeUI a,b;
        public EdgeUI(NodeUI a, NodeUI b) { this.a=a; this.b=b; }
    }

    // -------------------------
    // Layout (mantido similar)
    // -------------------------
    private void stepLayout() {
        double area = Math.max(1, getWidth() * getHeight());
        double k = Math.sqrt(area / (1 + Math.max(1, nodes.size())));
        double repulsion = 6000;
        double attraction = 0.10;

        for (NodeUI v : nodes) { v.fx = v.fy = 0; }

        for (int i=0;i<nodes.size();i++){
            NodeUI v = nodes.get(i);
            for (int j=i+1;j<nodes.size();j++){
                NodeUI u = nodes.get(j);
                double dx = v.x - u.x;
                double dy = v.y - u.y;
                double dist = Math.sqrt(dx*dx + dy*dy) + 0.01;
                double force = repulsion/(dist*dist);
                double fx = force * dx / dist; double fy = force * dy / dist;
                v.fx += fx; v.fy += fy; u.fx -= fx; u.fy -= fy;
            }
        }

        for (EdgeUI e : edges) {
            NodeUI a = e.a, b = e.b;
            double dx = b.x - a.x, dy = b.y - a.y;
            double dist = Math.sqrt(dx*dx + dy*dy) + 0.01;
            double force = attraction * (dist - k);
            double fx = force * dx / dist, fy = force * dy / dist;
            a.fx += fx; a.fy += fy; b.fx -= fx; b.fy -= fy;
        }

        for (NodeUI v : nodes) {
            if (v.fixed) continue;
            double damping = 0.88;
            v.vx = (v.vx + v.fx * 0.04) * damping;
            v.vy = (v.vy + v.fy * 0.04) * damping;
            v.x += v.vx; v.y += v.vy;
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Grid sutil
        drawGrid(g);

        AffineTransform at = new AffineTransform();
        at.translate(translateX, translateY);
        at.scale(scale, scale);
        g.transform(at);

        g.setStroke(new BasicStroke((float)(2.5/scale)));
        for (EdgeUI e : edges) drawModernEdge(g, e, edgePhase);
        for (NodeUI n : nodes) n.draw(g, n==selectedForLink || n==hoveredNode, scale, darkMode);

        g.dispose();

        // Status bar info
        Graphics2D g2 = (Graphics2D) g0;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(darkMode ? new Color(180, 180, 185) : new Color(100, 100, 105));
        String status = String.format("Modo: %s | Nós: %d | Ligações: %d", 
            mode, nodes.size(), edges.size());
        if (selectedForLink != null) status += " | Selecionado: " + selectedForLink.label;
        g2.drawString(status, 15, getHeight()-12);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(darkMode ? new Color(40, 40, 45, 50) : new Color(230, 230, 235, 80));
        int spacing = 40;
        for (int x = 0; x < getWidth(); x += spacing) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += spacing) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawModernEdge(Graphics2D g, EdgeUI e, double phase) {
        double x1 = e.a.x, y1 = e.a.y, x2 = e.b.x, y2 = e.b.y;
        
        // Linha principal com gradiente
        GradientPaint gp = new GradientPaint(
            (float)x1, (float)y1, new Color(e.a.color.getRed(), e.a.color.getGreen(), e.a.color.getBlue(), 120),
            (float)x2, (float)y2, new Color(e.b.color.getRed(), e.b.color.getGreen(), e.b.color.getBlue(), 120)
        );
        g.setPaint(gp);
        g.setStroke(new BasicStroke((float)(3.0/scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1,y1,x2,y2));
        
        // Partículas animadas
        double len = Math.hypot(x2-x1, y2-y1);
        int dots = Math.max(2, (int)(len/60));
        for (int i=0;i<dots;i++){
            double t = ((phase + (double)i/dots) % 1.0);
            double px = x1 + (x2-x1)*t;
            double py = y1 + (y2-y1)*t;
            double r = Math.max(4.0, 5.0/scale);
            Color c = blend(e.a.color, e.b.color, t);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 220));
            g.fill(new Ellipse2D.Double(px - r/2.0, py - r/2.0, r, r));
        }
    }

    private Color blend(Color a, Color b, double t) {
        int r = (int)(a.getRed()*(1-t) + b.getRed()*t);
        int g = (int)(a.getGreen()*(1-t) + b.getGreen()*t);
        int bl = (int)(a.getBlue()*(1-t) + b.getBlue()*t);
        return new Color(Math.max(0,Math.min(255,r)), Math.max(0,Math.min(255,g)), Math.max(0,Math.min(255,bl)));
    }

    // -------------------------
    // Mouse handlers (modernizado com hover)
    // -------------------------
    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            Point2D p = toLogical(e.getPoint());
            NodeUI n = findNodeAt((int)p.getX(), (int)p.getY());

            if (SwingUtilities.isLeftMouseButton(e)) {
                if (mode == Mode.ADD_NODE) {
                    pushSnapshot();
                    NodeUI newNode = new NodeUI(p.getX(), p.getY(), "Nova Nota", "", 
                        new Color(99, 110, 250), "#636EFA");
                    addNode(newNode);
                    openNodeEditor(newNode);
                    mode = Mode.NORMAL;
                    pushSnapshot();
                    return;
                }

                if (mode == Mode.LINKING) {
                    if (n != null) {
                        if (selectedForLink == null) { 
                            selectedForLink = n; 
                            showModernNotification("Selecione o segundo nó", new Color(46, 204, 113));
                        }
                        else if (selectedForLink != n) {
                            pushSnapshot();
                            addEdge(selectedForLink, n);
                            pushSnapshot();
                            selectedForLink = null;
                            mode = Mode.NORMAL;
                            showModernNotification("Ligação criada!", new Color(46, 204, 113));
                        }
                    }
                    return;
                }

                if (n != null) {
                    pushSnapshot();
                    draggedNode = n;
                    dragOffset = new Point((int)(p.getX() - n.x), (int)(p.getY() - n.y));
                } else selectedForLink = null;
            }

            if (SwingUtilities.isMiddleMouseButton(e) || (e.getButton()==MouseEvent.BUTTON1 && e.isShiftDown())) {
                lastPanPoint = e.getPoint();
                setCursor(new Cursor(Cursor.MOVE_CURSOR));
            }

            if (SwingUtilities.isRightMouseButton(e)) {
                if (n != null) {
                    showNodeContextMenu(n, e.getX(), e.getY());
                } else {
                    showCanvasContextMenu(e);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (draggedNode != null) {
                pushSnapshot();
            }
            draggedNode = null;
            dragOffset = null;
            lastPanPoint = null;
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount()==2 && SwingUtilities.isLeftMouseButton(e)) {
                Point2D p = toLogical(e.getPoint());
                NodeUI n = findNodeAt((int)p.getX(), (int)p.getY());
                if (n != null) openNodeEditor(n);
            }
        }
    };

    private void showNodeContextMenu(NodeUI n, int x, int y) {
        JPopupMenu pm = new JPopupMenu();
        pm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JMenuItem open = new JMenuItem("✏️ Editar Nota");
        JMenuItem newLink = new JMenuItem("🔗 Criar Ligação");
        JMenuItem pickColor = new JMenuItem("🎨 Escolher Cor");
        JMenuItem del = new JMenuItem("🗑️ Apagar Nó");

        open.addActionListener(a -> openNodeEditor(n));
        newLink.addActionListener(a -> { mode = Mode.LINKING; selectedForLink = n; });
        pickColor.addActionListener(a -> {
            Color chosen = JColorChooser.showDialog(GraphPanel.this, "Escolher cor do nó", n.color);
            if (chosen != null) {
                pushSnapshot();
                n.color = chosen;
                n.colorHex = String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue());
                pushSnapshot();
                repaint();
            }
        });
        del.addActionListener(a -> {
            pushSnapshot();
            edges.removeIf(ed -> ed.a==n || ed.b==n);
            nodes.remove(n);
            pushSnapshot();
            repaint();
        });

        pm.add(open); 
        pm.add(newLink); 
        pm.add(pickColor); 
        pm.addSeparator(); 
        pm.add(del);
        pm.show(GraphPanel.this, x, y);
    }

    private void showCanvasContextMenu(MouseEvent e) {
        JPopupMenu pm = new JPopupMenu();
        pm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JMenuItem addHere = new JMenuItem("➕ Adicionar Nó Aqui");
        addHere.addActionListener(a -> {
            try {
                pushSnapshot();
                Point2D p2 = toLogical(e.getPoint());
                NodeUI newNode = new NodeUI(p2.getX(), p2.getY(), "Nova Nota", "", 
                    new Color(99, 110, 250), "#636EFA");
                addNode(newNode);
                openNodeEditor(newNode);
                pushSnapshot();
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        pm.add(addHere);
        pm.show(GraphPanel.this, e.getX(), e.getY());
    }

    private final MouseMotionAdapter mouseMotionAdapter = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (draggedNode != null && dragOffset != null) {
                Point2D p = toLogical(e.getPoint());
                draggedNode.x = p.getX() - dragOffset.x;
                draggedNode.y = p.getY() - dragOffset.y;
                draggedNode.vx = draggedNode.vy = 0;
                draggedNode.fixed = true;
                repaint();
                return;
            }
            if (lastPanPoint != null) {
                Point p = e.getPoint();
                double dx = p.getX() - lastPanPoint.getX();
                double dy = p.getY() - lastPanPoint.getY();
                translateX += dx; translateY += dy;
                lastPanPoint = p; repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point2D p = toLogical(e.getPoint());
            NodeUI newHovered = findNodeAt((int)p.getX(), (int)p.getY());
            if (newHovered != hoveredNode) {
                hoveredNode = newHovered;
                setCursor(hoveredNode != null ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
                repaint();
            }
        }
    };

    private final MouseWheelListener mouseWheelListener = e -> {
        int rotation = e.getWheelRotation();
        double factor = Math.pow(1.12, -rotation);
        Point p = e.getPoint();
        Point2D before = toLogical(p);
        scale *= factor;
        scale = Math.max(0.1, Math.min(5.0, scale));
        Point2D after = toLogical(p);
        translateX += (after.getX() - before.getX()) * scale;
        translateY += (after.getY() - before.getY()) * scale;
        repaint();
    };

    // -------------------------
    // Utilities
    // -------------------------
    private Point2D toLogical(Point p) {
        try {
            AffineTransform at = new AffineTransform();
            at.translate(translateX, translateY);
            at.scale(scale, scale);
            AffineTransform inv = at.createInverse();
            Point2D src = new Point2D.Double(p.getX(), p.getY());
            Point2D dst = new Point2D.Double();
            inv.transform(src, dst);
            return dst;
        } catch (Exception ex) {
            return new Point2D.Double(p.getX(), p.getY());
        }
    }

    private NodeUI findNodeAt(int lx, int ly) {
        for (int i = nodes.size()-1; i>=0; i--) {
            if (nodes.get(i).contains(lx, ly)) return nodes.get(i);
        }
        return null;
    }

    private void openNoteDialog(NodeUI node) {
        NoteDialog dlg = new NoteDialog(SwingUtilities.getWindowAncestor(this), node, this);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        pushSnapshot();
    }

    private static Color parseHex(String s, Color fallback) {
        try {
            if (s == null) return fallback;
            return Color.decode(s);
        } catch (Exception ex) { return fallback; }
    }

    // -------------------------
    // Key bindings
    // -------------------------
    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newNote");

        am.put("undo", new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e) { undo(); }
        });
        am.put("redo", new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e) { redo(); }
        });
        am.put("newNote", new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e) {
                Point center = new Point(getWidth()/2, getHeight()/2);
                Point2D logical = toLogical(center);
                pushSnapshot();
                NodeUI newNode = new NodeUI(logical.getX(), logical.getY(), "Nova Nota", "", 
                    new Color(99, 110, 250), "#636EFA");
                addNode(newNode);
                openNoteDialog(newNode);
                pushSnapshot();
            }
        });
    }
}