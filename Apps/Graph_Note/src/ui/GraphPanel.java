package ui;

import model.Edge;
import model.GraphData;
import model.Node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * GraphPanel: interactive animated graph.
 * Uses simple runtime NodeUI objects (inner class) while persisting using model.Node/model.Edge.
 */
public class GraphPanel extends JPanel {
    // runtime representation with visual fields
    private final List<NodeUI> nodes = new ArrayList<>();
    private final List<EdgeUI> edges = new ArrayList<>();

    private final Timer animTimer;
    private boolean layoutRunning = true;

    private NodeUI draggedNode = null;
    private Point dragOffset = null;
    private NodeUI selectedForLink = null;
    private Mode mode = Mode.NORMAL;

    public enum Mode { NORMAL, ADD_NODE, LINKING }

    public GraphPanel() {
        setBackground(Color.WHITE);
        setFocusable(true);
        setLayout(null);

        // example nodes
        nodes.add(new NodeUI(200,120,"Bem-vindo",""));
        nodes.add(new NodeUI(400,200,"Nota 1",""));
        edges.add(new EdgeUI(nodes.get(0), nodes.get(1)));

        animTimer = new Timer(40, e -> {
            if (layoutRunning) stepLayout();
            repaint();
        });
        animTimer.start();

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseMotionAdapter);

        addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e){
                if (e.getKeyCode() == KeyEvent.VK_A) startAddingNodeMode();
                if (e.getKeyCode() == KeyEvent.VK_L) startLinkingMode();
            }
        });
    }

    // -------------------------
    // Public API used by NoteDialog and main
    // -------------------------
    public void addNode(NodeUI n) { nodes.add(n); }
    public void addEdge(NodeUI a, NodeUI b) { edges.add(new EdgeUI(a,b)); }
    public void openNodeEditor(NodeUI n) { openNoteDialog(n); }
    public void startAddingNodeMode() { mode = Mode.ADD_NODE; selectedForLink=null; JOptionPane.showMessageDialog(this,"Modo Adicionar Nó: clique para colocar."); }
    public void startLinkingMode() { mode = Mode.LINKING; selectedForLink=null; JOptionPane.showMessageDialog(this,"Modo Ligar: clique em dois nós."); }
    public void toggleLayoutRunning() { layoutRunning = !layoutRunning; }
    public void clearGraph() { nodes.clear(); edges.clear(); repaint(); }

    // Save using Gson
    public void saveToFile(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Graph Notes (*.gn)", "gn"));
        if (fc.showSaveDialog(parent==null?this:parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".gn")) f = new File(f.getAbsolutePath()+".gn");

        try {
            // build model.GraphData
            GraphData gd = new GraphData();
            for (NodeUI nu : nodes) {
                gd.nodes.add(new Node(nu.x, nu.y, nu.label, nu.note));
            }
            for (EdgeUI e : edges) {
                int a = nodes.indexOf(e.a);
                int b = nodes.indexOf(e.b);
                if (a >= 0 && b >= 0) gd.edges.add(new Edge(a,b));
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter fw = new FileWriter(f)) {
                gson.toJson(gd, fw);
            }
            JOptionPane.showMessageDialog(this, "Guardado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao guardar: " + ex.getMessage());
        }
    }

    // Load using Gson
    public void loadFromFile(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Graph Notes (*.gn)", "gn"));
        if (fc.showOpenDialog(parent==null?this:parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();

        try {
            String txt = new String(Files.readAllBytes(Paths.get(f.toURI())));
            Gson gson = new Gson();
            GraphData gd = gson.fromJson(txt, GraphData.class);

            // convert to runtime NodeUI/EdgeUI
            nodes.clear();
            edges.clear();
            if (gd != null && gd.nodes != null) {
                for (Node n : gd.nodes) {
                    nodes.add(new NodeUI(n.x, n.y, n.label, n.note));
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
            JOptionPane.showMessageDialog(this, "Carregado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar: " + ex.getMessage());
        }
    }

    // -------------------------
    // Internal runtime classes
    // -------------------------
    public static class NodeUI {
        public double x,y;
        public double vx=0, vy=0;
        public double fx=0, fy=0;
        public boolean fixed=false;
        public String label;
        public String note = "";
        public final int size = 48;

        public NodeUI(double x, double y, String label, String note) {
            this.x = x; this.y = y; this.label = label; this.note = note;
        }

        public boolean contains(double px, double py) {
            double r = size/2.0;
            return (px-x)*(px-x) + (py-y)*(py-y) <= r*r;
        }

        public void draw(Graphics2D g2, boolean highlight) {
            Ellipse2D.Double circle = new Ellipse2D.Double(x-size/2.0, y-size/2.0, size, size);
            if (highlight) g2.setColor(new Color(200,220,255));
            else g2.setColor(new Color(240,240,240));
            g2.fill(circle);
            g2.setColor(highlight?Color.BLUE:Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.draw(circle);
            FontMetrics fm = g2.getFontMetrics();
            String text = label==null?"":label;
            int tw = fm.stringWidth(text);
            g2.drawString(text, (int)(x - tw/2.0), (int)(y + fm.getAscent()/2.0));
        }
    }

    private static class EdgeUI {
        public NodeUI a,b;
        public EdgeUI(NodeUI a, NodeUI b) { this.a = a; this.b = b; }
    }

    // -------------------------
    // Layout and painting
    // -------------------------
    private void stepLayout() {
        double area = Math.max(1, getWidth() * getHeight());
        double k = Math.sqrt(area / (1 + Math.max(1, nodes.size())));
        double repulsion = 5000;
        double attraction = 0.1;

        for (NodeUI v : nodes) { v.fx = v.fy = 0; }

        for (int i=0;i<nodes.size();i++){
            NodeUI v = nodes.get(i);
            for (int j=i+1;j<nodes.size();j++){
                NodeUI u = nodes.get(j);
                double dx = v.x - u.x;
                double dy = v.y - u.y;
                double dist = Math.sqrt(dx*dx + dy*dy) + 0.01;
                double force = repulsion / (dist*dist);
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
            double damping = 0.85;
            v.vx = (v.vx + v.fx * 0.05) * damping;
            v.vy = (v.vy + v.fy * 0.05) * damping;
            v.x += v.vx; v.y += v.vy;
            v.x = Math.max(20, Math.min(getWidth()-20, v.x));
            v.y = Math.max(20, Math.min(getHeight()-20, v.y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.GRAY);
        g2.setStroke(new BasicStroke(2));
        for (EdgeUI e : edges) {
            g2.drawLine((int)e.a.x, (int)e.a.y, (int)e.b.x, (int)e.b.y);
        }
        for (NodeUI n : nodes) {
            n.draw(g2, n==selectedForLink);
        }
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Modo: " + mode + (selectedForLink!=null ? (" | Selecionado: " + selectedForLink.label) : ""), 10, getHeight()-8);
    }

    // -------------------------
    // Mouse handlers
    // -------------------------
    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            NodeUI n = findNodeAt(e.getPoint());
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (mode == Mode.ADD_NODE) {
                    NodeUI newNode = new NodeUI(e.getX(), e.getY(), "Nova nota", "");
                    addNode(newNode);
                    openNodeEditor(newNode);
                    mode = Mode.NORMAL;
                    return;
                }
                if (mode == Mode.LINKING) {
                    if (n != null) {
                        if (selectedForLink == null) selectedForLink = n;
                        else if (selectedForLink != n) {
                            addEdge(selectedForLink, n);
                            selectedForLink = null;
                            mode = Mode.NORMAL;
                        }
                    }
                    return;
                }
                if (n != null) {
                    draggedNode = n;
                    dragOffset = new Point(e.getX() - (int)n.x, e.getY() - (int)n.y);
                } else selectedForLink = null;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                if (n != null) {
                    JPopupMenu pm = new JPopupMenu();
                    JMenuItem open = new JMenuItem("Abrir/Editar nota");
                    JMenuItem del = new JMenuItem("Apagar nó");
                    JMenuItem newLink = new JMenuItem("Ligar a...");

                    open.addActionListener(a -> openNodeEditor(n));
                    del.addActionListener(a -> { edges.removeIf(ed -> ed.a==n || ed.b==n); nodes.remove(n); repaint(); });
                    newLink.addActionListener(a -> { mode = Mode.LINKING; selectedForLink = n; });

                    pm.add(open); pm.add(newLink); pm.addSeparator(); pm.add(del);
                    pm.show(GraphPanel.this, e.getX(), e.getY());
                } else {
                    JPopupMenu pm = new JPopupMenu();
                    JMenuItem addHere = new JMenuItem("Adicionar nó aqui");
                    addHere.addActionListener(a -> {
                        NodeUI newNode = new NodeUI(e.getX(), e.getY(), "Nova nota", "");
                        addNode(newNode);
                        openNodeEditor(newNode);
                    });
                    pm.add(addHere);
                    pm.show(GraphPanel.this, e.getX(), e.getY());
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (draggedNode != null) draggedNode.fixed = false;
            draggedNode = null; dragOffset = null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                NodeUI n = findNodeAt(e.getPoint());
                if (n != null) openNodeEditor(n);
            }
        }
    };

    private final MouseMotionAdapter mouseMotionAdapter = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (draggedNode != null) {
                draggedNode.x = e.getX() - dragOffset.x;
                draggedNode.y = e.getY() - dragOffset.y;
                draggedNode.vx = draggedNode.vy = 0;
                draggedNode.fixed = true;
                repaint();
            }
        }
    };

    private NodeUI findNodeAt(Point p) {
        for (int i=nodes.size()-1;i>=0;i--) {
            if (nodes.get(i).contains(p.x,p.y)) return nodes.get(i);
        }
        return null;
    }

    private void openNoteDialog(NodeUI node) {
        NoteDialog dlg = new NoteDialog(SwingUtilities.getWindowAncestor(this), node, this);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
}
