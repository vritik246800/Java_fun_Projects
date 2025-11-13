/* 
GraphNotesApp.java
Aplicação Swing com:
- Grafo animado (force-directed)
- Criar nós, ligar nós
- Arrastar nós
- Abrir/editar notas em janela
- Criar nó ligado
- Guardar/carregar (pseudo JSON)
*/

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class GraphNotesApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Graph Notes - Animated Graph");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);

            GraphPanel panel = new GraphPanel();
            frame.add(panel, BorderLayout.CENTER);

            // Toolbar
            JToolBar toolBar = new JToolBar();
            JButton addBtn = new JButton("Add Node");
            JButton linkBtn = new JButton("Link Nodes");
            JButton saveBtn = new JButton("Save");
            JButton loadBtn = new JButton("Load");
            JButton clearBtn = new JButton("Clear");
            JButton autoBtn = new JButton("Toggle Layout");

            toolBar.add(addBtn);
            toolBar.add(linkBtn);
            toolBar.addSeparator();
            toolBar.add(saveBtn);
            toolBar.add(loadBtn);
            toolBar.add(clearBtn);
            toolBar.addSeparator();
            toolBar.add(autoBtn);

            frame.add(toolBar, BorderLayout.NORTH);

            addBtn.addActionListener(e -> panel.startAddingNodeMode());
            linkBtn.addActionListener(e -> panel.startLinkingMode());
            saveBtn.addActionListener(e -> panel.saveToFile(frame));
            loadBtn.addActionListener(e -> panel.loadFromFile(frame));
            clearBtn.addActionListener(e -> panel.clearGraph());
            autoBtn.addActionListener(e -> panel.toggleLayoutRunning());

            frame.setVisible(true);
        });
    }
}

class GraphPanel extends JPanel {
    private final java.util.List<Node> nodes = new ArrayList<>();
    private final java.util.List<Edge> edges = new ArrayList<>();

    private final Timer animTimer;
    private boolean layoutRunning = true;

    // Interaction
    private Node draggedNode = null;
    private Point dragOffset = null;
    private Node selectedForLink = null;
    private Mode mode = Mode.NORMAL;

    private enum Mode { NORMAL, ADD_NODE, LINKING }

    public GraphPanel() {
        setBackground(Color.WHITE);
        setFocusable(true);
        setLayout(null);

        // nodes example
        nodes.add(new Node(200, 120, "Bem-vindo"));
        nodes.add(new Node(400, 200, "Nota 1"));
        edges.add(new Edge(nodes.get(0), nodes.get(1)));

        animTimer = new Timer(40, e -> {
            if (layoutRunning) stepLayout();
            repaint();
        });
        animTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {

                Node n = findNodeAt(e.getPoint());

                if (SwingUtilities.isLeftMouseButton(e)) {

                    if (mode == Mode.ADD_NODE) {
                        Node newNode = new Node(e.getX(), e.getY(), "Nova nota");
                        addNode(newNode);
                        openNodeEditor(newNode);
                        mode = Mode.NORMAL;
                        return;
                    }

                    if (mode == Mode.LINKING) {
                        if (n != null) {
                            if (selectedForLink == null) {
                                selectedForLink = n;
                            } else if (selectedForLink != n) {
                                addEdge(selectedForLink, n);
                                selectedForLink = null;
                                mode = Mode.NORMAL;
                            }
                        }
                        return;
                    }

                    if (n != null) {
                        draggedNode = n;
                        dragOffset = new Point(
                            e.getX() - (int)n.x,
                            e.getY() - (int)n.y
                        );
                    } else {
                        selectedForLink = null;
                    }
                }

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (n != null) {
                        JPopupMenu pm = new JPopupMenu();
                        JMenuItem open = new JMenuItem("Abrir/Editar nota");
                        JMenuItem del = new JMenuItem("Apagar nó");
                        JMenuItem newLink = new JMenuItem("Ligar a...");

                        open.addActionListener(a -> openNodeEditor(n));
                        del.addActionListener(a -> removeNode(n));
                        newLink.addActionListener(a -> { mode = Mode.LINKING; selectedForLink = n; });

                        pm.add(open); pm.add(newLink); pm.addSeparator(); pm.add(del);
                        pm.show(GraphPanel.this, e.getX(), e.getY());

                    } else {
                        JPopupMenu pm = new JPopupMenu();
                        JMenuItem addHere = new JMenuItem("Adicionar nó aqui");
                        addHere.addActionListener(a -> {
                            Node newNode = new Node(e.getX(), e.getY(), "Nova nota");
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
                if (draggedNode != null)
                    draggedNode.fixed = false;

                draggedNode = null;
                dragOffset = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    Node n = findNodeAt(e.getPoint());
                    if (n != null) openNodeEditor(n);
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedNode != null) {
                    draggedNode.x = e.getX() - dragOffset.x;
                    draggedNode.y = e.getY() - dragOffset.y;
                    draggedNode.vx = draggedNode.vy = 0;
                    draggedNode.fixed = true;
                }
            }
        });
    }

    // PUBLIC METHODS TO FIX ACCESS ERRORS
    public void addNode(Node n) {
        nodes.add(n);
    }

    public void addEdge(Node a, Node b) {
        edges.add(new Edge(a, b));
    }

    public void openNodeEditor(Node n) {
        openNoteDialog(n);
    }

    // MODES
    public void startAddingNodeMode() {
        mode = Mode.ADD_NODE;
        selectedForLink = null;
        JOptionPane.showMessageDialog(this, "Modo Adicionar Nó.");
    }

    public void startLinkingMode() {
        mode = Mode.LINKING;
        selectedForLink = null;
        JOptionPane.showMessageDialog(this, "Modo Ligar Nós.");
    }

    public void toggleLayoutRunning() {
        layoutRunning = !layoutRunning;
    }

    private Node findNodeAt(Point p) {
        for (int i = nodes.size()-1; i>=0; i--)
            if (nodes.get(i).contains(p.x, p.y))
                return nodes.get(i);
        return null;
    }

    private void removeNode(Node n) {
        edges.removeIf(e -> e.a == n || e.b == n);
        nodes.remove(n);
        repaint();
    }

    private void openNoteDialog(Node node) {
        NoteDialog dlg = new NoteDialog(
            SwingUtilities.getWindowAncestor(this),
            node,
            this
        );
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    public void clearGraph() {
        nodes.clear();
        edges.clear();
        repaint();
    }

    // SIMPLE FORCE LAYOUT
    private void stepLayout() {
        double area = getWidth() * getHeight();
        if (area <= 0) return;

        double k = Math.sqrt(area / (1 + nodes.size()));
        double repulsion = 5000;
        double attraction = 0.1;

        for (Node v : nodes) {
            v.fx = v.fy = 0;
        }

        for (int i=0; i<nodes.size(); i++) {
            Node v = nodes.get(i);

            for (int j=i+1; j<nodes.size(); j++) {
                Node u = nodes.get(j);
                double dx = v.x - u.x;
                double dy = v.y - u.y;
                double dist = Math.sqrt(dx*dx + dy*dy) + 0.01;

                double force = repulsion / (dist*dist);
                double fx = force * dx / dist;
                double fy = force * dy / dist;

                v.fx += fx; v.fy += fy;
                u.fx -= fx; u.fy -= fy;
            }
        }

        for (Edge e : edges) {
            Node a = e.a, b = e.b;
            double dx = b.x - a.x;
            double dy = b.y - a.y;
            double dist = Math.sqrt(dx*dx + dy*dy) + 0.01;

            double force = attraction * (dist - k);
            double fx = force * dx / dist;
            double fy = force * dy / dist;

            a.fx += fx; a.fy += fy;
            b.fx -= fx; b.fy -= fy;
        }

        for (Node v : nodes) {
            if (v.fixed) continue;

            double damping = 0.85;

            v.vx = (v.vx + v.fx * 0.05) * damping;
            v.vy = (v.vy + v.fy * 0.05) * damping;

            v.x += v.vx;
            v.y += v.vy;

            v.x = Math.max(20, Math.min(getWidth()-20, v.x));
            v.y = Math.max(20, Math.min(getHeight()-20, v.y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.GRAY);

        for (Edge e : edges) {
            g2.drawLine((int)e.a.x, (int)e.a.y, (int)e.b.x, (int)e.b.y);
        }

        for (Node n : nodes) {
            n.draw(g2, n==selectedForLink);
        }
    }

    // Save / Load
    public void saveToFile(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Graph Notes (*.gn)", "gn"));

        if (fc.showSaveDialog(parent==null?this:parent) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".gn"))
                f = new File(f.getAbsolutePath() + ".gn");

            try (PrintWriter out = new PrintWriter(f)) {
                out.println("{");
                out.println("\"nodes\": [");

                for (int i=0; i<nodes.size(); i++) {
                    Node n = nodes.get(i);
                    out.printf(
                        "{\"x\":%f,\"y\":%f,\"label\":\"%s\",\"note\":\"%s\"}%s\n",
                        n.x, n.y, escape(n.label), escape(n.note),
                        (i<nodes.size()-1?",":"")
                    );
                }

                out.println("],");
                out.println("\"edges\": [");

                for (int i=0; i<edges.size(); i++) {
                    Edge e = edges.get(i);
                    int a = nodes.indexOf(e.a);
                    int b = nodes.indexOf(e.b);
                    out.printf("{\"a\":%d,\"b\":%d}%s\n",
                        a,b, (i<edges.size()-1?",":"")
                    );
                }

                out.println("]");
                out.println("}");
            }
            catch(Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        }
    }

    public void loadFromFile(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Graph Notes (*.gn)", "gn"));

        if (fc.showOpenDialog(parent==null?this:parent) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                String txt = new String(Files.readAllBytes(Paths.get(f.toURI())));

                nodes.clear();
                edges.clear();

                // Parse nodes
                String nodesPart = txt.split("\"nodes\"\\s*:\\s*\\[")[1].split("]",2)[0];
                for (String line : nodesPart.split("},")) {
                    String s = line.replaceAll("[\\n\\r\\t\"]"," ");
                    s = s.replace(",", " "); // <--- REMOVE VÍRGULAS PROBLEMÁTICAS

                    if (!s.contains("x")) continue;

                    double x = Double.parseDouble(s.replaceAll(".*x\\s*:\\s*([0-9.+-eE]+).*","$1"));
                    double y = Double.parseDouble(s.replaceAll(".*y\\s*:\\s*([0-9.+-eE]+).*", "$1"));

                    String label = extractBetween(s, "label:", "note:");
                    String note = extractBetween(s, "note:", "");

                    Node n = new Node(x, y, unescape(label.trim()));
                    n.note = unescape(note.trim());

                    nodes.add(n);
                }

                // Parse edges
                String edgesPart = txt.split("\"edges\"\\s*:\\s*\\[")[1].split("]",2)[0];
                for (String line : edgesPart.split("},")) {
                    if (!line.contains("a")) continue;

                    line = line.replaceAll("[^0-9 ,]"," ");
                    String[] parts = line.trim().split(",");

                    if (parts.length>=2) {
                        int a = Integer.parseInt(parts[0].trim());
                        int b = Integer.parseInt(parts[1].trim());

                        if (a>=0 && a<nodes.size() && b>=0 && b<nodes.size()) {
                            addEdge(nodes.get(a), nodes.get(b));
                        }
                    }
                }

                repaint();
            }
            catch(Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    // helpers
    private static String escape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }

    private static String unescape(String s) {
        return s.replace("\\n","\n").replace("\\\"","\"").replace("\\\\","\\");
    }

    private static String extractBetween(String s, String a, String b) {
        int ia = s.indexOf(a);
        if (ia < 0) return "";
        int ib = b.isEmpty()? s.length() : s.indexOf(b, ia+1);
        if (ib < 0) ib = s.length();
        return s.substring(ia + a.length(), ib).replaceAll("[:,]"," ");
    }
}


// Node class
class Node {
    double x, y;
    double vx=0, vy=0;
    double fx=0, fy=0;
    boolean fixed=false;

    String label;
    String note = "";

    final int size = 48;

    public Node(double x, double y, String label) {
        this.x = x; this.y = y;
        this.label = label;
    }

    public boolean contains(double px, double py) {
        double r = size/2.0;
        return (px-x)*(px-x) + (py-y)*(py-y) <= r*r;
    }

    public void draw(Graphics2D g2, boolean highlight) {
        Ellipse2D.Double circle =
            new Ellipse2D.Double(x-size/2.0, y-size/2.0, size, size);

        if (highlight)
            g2.setColor(new Color(200,220,255));
        else
            g2.setColor(new Color(240,240,240));

        g2.fill(circle);

        g2.setColor(highlight ? Color.BLUE : Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(2));
        g2.draw(circle);

        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(label);
        g2.drawString(label, (int)(x - tw/2.0), (int)(y + fm.getAscent()/2.0));
    }
}

// Edge class
class Edge {
    Node a, b;
    Edge(Node a, Node b){ this.a=a; this.b=b; }
}


// Note Dialog
class NoteDialog extends JDialog {

    public NoteDialog(Window owner, Node node, GraphPanel panel) {
        super(owner, "Nota: " + node.label, ModalityType.APPLICATION_MODAL);
        setSize(420, 320);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        JTextField title = new JTextField(node.label);
        top.add(new JLabel("Título:"), BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JTextArea area = new JTextArea(node.note);
        add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton save = new JButton("Salvar");
        JButton cancel = new JButton("Cancelar");
        JButton newNode = new JButton("Criar Nó Ligado");
        bottom.add(newNode);
        bottom.add(save);
        bottom.add(cancel);
        add(bottom, BorderLayout.SOUTH);

        save.addActionListener(e -> {
            node.label = title.getText();
            node.note = area.getText();
            dispose();
        });

        cancel.addActionListener(e -> dispose());

        newNode.addActionListener(e -> {
            Node other = new Node(node.x + 80, node.y + 20, "Nova nota");
            panel.addNode(other);
            panel.addEdge(node, other);
            dispose();
            panel.openNodeEditor(other);
        });
    }
}
