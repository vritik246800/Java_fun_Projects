package jotes.ui.canvas;

import jotes.db.CanvasRepository;
import jotes.db.NoteRepository;
import jotes.model.CanvasEdge;
import jotes.model.CanvasNode;
import jotes.model.Note;
import jotes.ui.Theme;
import jotes.ui.anim.Animator;
import jotes.ui.anim.Easing;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Canvas infinito ao estilo do Obsidian: cartões de texto, notas, imagens e grupos,
 * ligados por arestas curvas com etiqueta, cor e direção.
 * <p>Atalhos: arrastar o fundo desloca a vista; roda desloca (Shift+roda na horizontal);
 * Ctrl+roda faz zoom; duplo clique no fundo cria cartão; Shift+arrastar seleciona em área;
 * Ctrl+clique alterna seleção; Ctrl+D duplica; Delete remove a seleção;
 * arrastar o ponto à direita de um cartão cria uma ligação; o canto inferior direito redimensiona.</p>
 */
public class CanvasPanel extends JPanel {

    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 3.0;
    private static final int PORT_R = 5;
    private static final int HANDLE = 10;
    private static final int PAD = 12;
    private static final double MIN_W = 140;
    private static final double MIN_H = 70;

    /** Cores dos cartões: tons escuros suaves (cor viva esbatida sobre {@link Theme#CARD_BG}). */
    private static final Map<String, Color> NODE_COLORS = Map.of(
            "", Theme.CARD_BG,
            "red", tint(Theme.DANGER),
            "orange", tint(new Color(0xFF, 0x9F, 0x0A)),
            "yellow", tint(Theme.ACCENT),
            "green", tint(new Color(0x30, 0xD1, 0x58)),
            "blue", tint(Theme.TEXT_SELECTION),
            "purple", tint(new Color(0xBF, 0x5A, 0xF2)));

    /** Cores das ligações (tons vivos da paleta dark do macOS). */
    private static final Map<String, Color> EDGE_COLORS = Map.of(
            "", Theme.TEXT_DIM,
            "red", Theme.DANGER,
            "orange", new Color(0xFF, 0x9F, 0x0A),
            "yellow", Theme.ACCENT,
            "green", new Color(0x30, 0xD1, 0x58),
            "blue", Theme.TEXT_SELECTION,
            "purple", new Color(0xBF, 0x5A, 0xF2));

    /** Esbate uma cor viva sobre {@link Theme#CARD_BG}, para tons de cartão legíveis no escuro. */
    private static Color tint(Color vivid) {
        float a = 0.25f;
        return new Color(
                Math.round(vivid.getRed() * a + Theme.CARD_BG.getRed() * (1 - a)),
                Math.round(vivid.getGreen() * a + Theme.CARD_BG.getGreen() * (1 - a)),
                Math.round(vivid.getBlue() * a + Theme.CARD_BG.getBlue() * (1 - a)));
    }

    private static final Map<String, String> COLOR_NAMES = Map.of(
            "", "Predefinida",
            "red", "Vermelho",
            "orange", "Laranja",
            "yellow", "Amarelo",
            "green", "Verde",
            "blue", "Azul",
            "purple", "Roxo");

    private final CanvasRepository canvasRepo;
    private final NoteRepository noteRepo;

    private long boardId = -1;
    private final List<CanvasNode> nodes = new ArrayList<>();
    private final List<CanvasEdge> edges = new ArrayList<>();
    private final Map<Long, Note> notesById = new HashMap<>();
    private final Map<String, Image> imageCache = new HashMap<>();

    private final Set<CanvasNode> selection = new LinkedHashSet<>();
    private CanvasNode hovered;

    private double zoom = 1.0;
    private double zoomTarget = 1.0; // alvo do zoom animado (acumula com a roda a meio da animação)
    private Animator viewAnim;       // animação de vista em curso (cancelada ao re-apontar)
    private double originX; // coordenada do mundo no canto superior esquerdo do ecrã
    private double originY;

    private enum Drag { NONE, PAN, MOVE, EDGE, SELECT, RESIZE }
    private Drag drag = Drag.NONE;
    private Point pressScreen;
    private final Map<CanvasNode, double[]> moveStarts = new HashMap<>();
    private double pressOriginX, pressOriginY;
    private double pressNodeW, pressNodeH;
    private CanvasNode resizeNode;
    private CanvasNode edgeSource;
    private Point2D.Double edgeMouse;
    private Point2D.Double rubberStart, rubberCur;
    private boolean rubberAdditive;
    private boolean moved;

    private Consumer<Long> onOpenNote = id -> {};
    private Runnable onZoomChanged = () -> {};

    public CanvasPanel(CanvasRepository canvasRepo, NoteRepository noteRepo) {
        this.canvasRepo = canvasRepo;
        this.noteRepo = noteRepo;
        setBackground(Theme.BG);
        setForeground(Theme.TEXT);
        setFont(Theme.font(Font.PLAIN, 13));

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onPress(e); }
            @Override public void mouseReleased(MouseEvent e) { onRelease(e); }
            @Override public void mouseDragged(MouseEvent e) { onDrag(e); }
            @Override public void mouseClicked(MouseEvent e) { onClick(e); }
            @Override public void mouseMoved(MouseEvent e) { onHover(e); }
            @Override public void mouseExited(MouseEvent e) {
                hovered = null;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                zoomAt(e.getPoint(), zoomTarget * Math.pow(1.1, -e.getPreciseWheelRotation()));
            } else if (e.isShiftDown()) {
                originX += e.getPreciseWheelRotation() * 60 / zoom;
                repaint();
            } else {
                originY += e.getPreciseWheelRotation() * 60 / zoom;
                repaint();
            }
        });

        bindKey("DELETE", this::deleteSelection);
        bindKey("BACK_SPACE", this::deleteSelection);
        bindKey("ctrl D", this::duplicateSelection);
    }

    private void bindKey(String key, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
        });
    }

    /** Regista o callback chamado quando o utilizador abre uma nota (duplo clique). */
    public void setOnOpenNote(Consumer<Long> listener) {
        this.onOpenNote = listener;
    }

    /** Regista o callback chamado quando o zoom muda (para a etiqueta da toolbar). */
    public void setOnZoomChanged(Runnable listener) {
        this.onZoomChanged = listener;
    }

    /** Carrega um quadro da base de dados e centra a vista no conteúdo. */
    public void loadBoard(long boardId) {
        this.boardId = boardId;
        selection.clear();
        hovered = null;
        nodes.clear();
        edges.clear();
        try {
            nodes.addAll(canvasRepo.nodes(boardId));
            edges.addAll(canvasRepo.edges(boardId));
            refreshNotes();
        } catch (SQLException ex) {
            showError(ex);
        }
        // adiado: no construtor a janela ainda tem 0×0 e resetView() não faria nada
        SwingUtilities.invokeLater(this::resetView);
        repaint();
    }

    /** Recarrega os títulos/excertos das notas referenciadas; remove cartões de notas apagadas. */
    public void refreshNotes() {
        notesById.clear();
        try {
            for (Note n : noteRepo.listAll()) notesById.put(n.getId(), n);
        } catch (SQLException ex) {
            showError(ex);
        }
        // remove também na BD (as arestas caem em cascata), senão acumulavam-se órfãos
        List<CanvasNode> orphan = nodes.stream()
                .filter(n -> n.isNote() && !notesById.containsKey(n.getNoteId()))
                .toList();
        for (CanvasNode n : orphan) {
            try {
                canvasRepo.deleteNode(n.getId());
            } catch (SQLException ex) {
                showError(ex);
            }
            nodes.remove(n);
            edges.removeIf(e -> e.getFromNode() == n.getId() || e.getToNode() == n.getId());
            selection.remove(n);
        }
        repaint();
    }

    public double getZoom() { return zoom; }

    public void zoomIn() { zoomAt(centerScreen(), zoomTarget * 1.25); }
    public void zoomOut() { zoomAt(centerScreen(), zoomTarget / 1.25); }

    /** Centra a vista no conteúdo (ou na origem, se o quadro estiver vazio), com animação suave. */
    public void resetView() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        double z1, x1, y1;
        if (nodes.isEmpty()) {
            z1 = 1.0;
            x1 = -getWidth() / 2.0;
            y1 = -getHeight() / 2.0;
        } else {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (CanvasNode n : nodes) {
                minX = Math.min(minX, n.getX());
                minY = Math.min(minY, n.getY());
                maxX = Math.max(maxX, n.getX() + n.getW());
                maxY = Math.max(maxY, n.getY() + n.getH());
            }
            double m = 80;
            z1 = clamp(Math.min(getWidth() / (maxX - minX + 2 * m), getHeight() / (maxY - minY + 2 * m)));
            x1 = (minX + maxX) / 2 - getWidth() / (2 * z1);
            y1 = (minY + maxY) / 2 - getHeight() / (2 * z1);
        }
        zoomTarget = z1;
        double z0 = zoom, x0 = originX, y0 = originY;
        animateView(t -> {
            zoom = z0 + (z1 - z0) * t;
            originX = x0 + (x1 - x0) * t;
            originY = y0 + (y1 - y0) * t;
        });
    }

    /** Cria um cartão de texto no centro da vista e abre o editor de texto. */
    public void addCardAtCenter() {
        addCard(viewCenter());
    }

    /** Abre o seletor de notas e adiciona a nota escolhida ao centro da vista. */
    public void addNoteAtCenter(Component parent) {
        Note choice = chooseNote(parent);
        if (choice != null) addNoteNode(choice, viewCenter());
    }

    /** Abre o seletor de ficheiros e adiciona a imagem escolhida ao centro da vista. */
    public void addImageAtCenter(Component parent) {
        addImage(parent, viewCenter());
    }

    /** Cria um grupo à volta dos cartões selecionados. */
    public void groupSelection() {
        List<CanvasNode> targets = new ArrayList<>();
        for (CanvasNode n : selection) if (!n.isGroup()) targets.add(n);
        if (targets.isEmpty()) return;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (CanvasNode n : targets) {
            minX = Math.min(minX, n.getX());
            minY = Math.min(minY, n.getY());
            maxX = Math.max(maxX, n.getX() + n.getW());
            maxY = Math.max(maxY, n.getY() + n.getH());
        }
        double m = 40;
        CanvasNode g = new CanvasNode();
        g.setCanvasId(boardId);
        g.setKind(CanvasNode.KIND_GROUP);
        g.setText("Grupo");
        g.setX(minX - m);
        g.setY(minY - m);
        g.setW(maxX - minX + 2 * m);
        g.setH(maxY - minY + 2 * m);
        try {
            canvasRepo.addNode(g);
            nodes.add(0, g);
            repaint();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // ---------- interação ----------

    private void onPress(MouseEvent e) {
        requestFocusInWindow();
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        moved = false;
        pressScreen = e.getPoint();
        Point2D.Double w = toWorld(e.getPoint());

        // pega de redimensionamento do cartão selecionado
        if (selection.size() == 1) {
            CanvasNode sel = selection.iterator().next();
            if (overHandle(sel, w)) {
                drag = Drag.RESIZE;
                resizeNode = sel;
                pressNodeW = sel.getW();
                pressNodeH = sel.getH();
                return;
            }
        }
        CanvasNode port = pickPort(w);
        if (port != null) {
            drag = Drag.EDGE;
            edgeSource = port;
            edgeMouse = w;
            return;
        }
        CanvasNode node = pickNode(w);
        if (node != null) {
            if (e.isControlDown()) {
                if (!selection.remove(node)) selection.add(node);
                drag = Drag.NONE;
                repaint();
                return;
            }
            if (!selection.contains(node)) {
                selection.clear();
                selection.add(node);
            }
            drag = Drag.MOVE;
            moveStarts.clear();
            for (CanvasNode n : selection) {
                moveStarts.put(n, new double[]{n.getX(), n.getY()});
                if (n.isGroup()) {
                    for (CanvasNode inside : nodes) {
                        if (inside != n && !moveStarts.containsKey(inside) && containsFully(n, inside)) {
                            moveStarts.put(inside, new double[]{inside.getX(), inside.getY()});
                        }
                    }
                }
            }
            bringToFront(node);
        } else if (e.isShiftDown()) {
            drag = Drag.SELECT;
            rubberAdditive = e.isControlDown();
            rubberStart = w;
            rubberCur = w;
        } else {
            selection.clear();
            drag = Drag.PAN;
            // o arrasto manual assume o controlo da vista: pára qualquer animação em curso
            if (viewAnim != null) viewAnim.cancel();
            pressOriginX = originX;
            pressOriginY = originY;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        repaint();
    }

    private void onDrag(MouseEvent e) {
        if (pressScreen == null) return;
        moved = true;
        double dx = (e.getX() - pressScreen.x) / zoom;
        double dy = (e.getY() - pressScreen.y) / zoom;
        switch (drag) {
            case PAN -> {
                originX = pressOriginX - dx;
                originY = pressOriginY - dy;
            }
            case MOVE -> {
                for (Map.Entry<CanvasNode, double[]> en : moveStarts.entrySet()) {
                    en.getKey().setX(en.getValue()[0] + dx);
                    en.getKey().setY(en.getValue()[1] + dy);
                }
            }
            case RESIZE -> {
                if (resizeNode != null) {
                    resizeNode.setW(Math.max(MIN_W, pressNodeW + dx));
                    resizeNode.setH(Math.max(MIN_H, pressNodeH + dy));
                }
            }
            case EDGE -> edgeMouse = toWorld(e.getPoint());
            case SELECT -> rubberCur = toWorld(e.getPoint());
            default -> {}
        }
        repaint();
    }

    private void onRelease(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
        switch (drag) {
            case MOVE -> {
                if (moved) {
                    for (CanvasNode n : moveStarts.keySet()) {
                        try {
                            canvasRepo.updateNode(n);
                        } catch (SQLException ex) {
                            showError(ex);
                        }
                    }
                }
                moveStarts.clear();
            }
            case RESIZE -> {
                if (resizeNode != null) {
                    try {
                        canvasRepo.updateNode(resizeNode);
                    } catch (SQLException ex) {
                        showError(ex);
                    }
                }
                resizeNode = null;
            }
            case EDGE -> {
                if (edgeSource != null) {
                    CanvasNode target = pickNode(toWorld(e.getPoint()));
                    if (target != null && target != edgeSource) {
                        try {
                            edges.add(canvasRepo.addEdge(boardId, edgeSource.getId(), target.getId()));
                        } catch (SQLException ex) {
                            showError(ex);
                        }
                    }
                }
            }
            case SELECT -> {
                if (rubberStart != null && rubberCur != null) {
                    Rectangle2D.Double r = rect(rubberStart, rubberCur);
                    if (!rubberAdditive) selection.clear();
                    for (CanvasNode n : nodes) {
                        if (r.intersects(n.getX(), n.getY(), n.getW(), n.getH())) selection.add(n);
                    }
                }
                rubberStart = rubberCur = null;
            }
            default -> {}
        }
        drag = Drag.NONE;
        edgeSource = null;
        edgeMouse = null;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    private void onClick(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 2) return;
        Point2D.Double w = toWorld(e.getPoint());
        CanvasNode node = pickNode(w);
        if (node != null) {
            if (node.isNote() && node.getNoteId() != null) onOpenNote.accept(node.getNoteId());
            else if (node.isGroup()) editNodeText(node, "Nome do grupo");
            else if (node.isImage()) openImage(node);
            else editNodeText(node, "Texto do cartão");
            return;
        }
        CanvasEdge edge = pickEdge(w);
        if (edge != null) {
            editEdgeLabel(edge);
            return;
        }
        addCard(w);
    }

    private void onHover(MouseEvent e) {
        Point2D.Double w = toWorld(e.getPoint());
        hovered = pickNode(w);
        if (hovered != null && overHandle(hovered, w)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        } else if (pickPort(w) != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    private void showPopup(MouseEvent e) {
        Point2D.Double w = toWorld(e.getPoint());
        CanvasNode node = pickNode(w);
        if (node != null) {
            if (!selection.contains(node)) {
                selection.clear();
                selection.add(node);
            }
            JPopupMenu menu = new JPopupMenu();
            if (node.isNote() && node.getNoteId() != null) {
                JMenuItem open = new JMenuItem("Abrir nota");
                open.addActionListener(a -> onOpenNote.accept(node.getNoteId()));
                menu.add(open);
            } else if (node.isGroup()) {
                JMenuItem edit = new JMenuItem("Editar nome");
                edit.addActionListener(a -> editNodeText(node, "Nome do grupo"));
                menu.add(edit);
            } else if (node.isImage()) {
                JMenuItem open = new JMenuItem("Abrir imagem");
                open.addActionListener(a -> openImage(node));
                menu.add(open);
            } else {
                JMenuItem edit = new JMenuItem("Editar texto");
                edit.addActionListener(a -> editNodeText(node, "Texto do cartão"));
                menu.add(edit);
            }
            JMenu colors = new JMenu("Cor");
            for (String key : NODE_COLORS.keySet()) {
                JMenuItem item = new JMenuItem(COLOR_NAMES.get(key));
                item.addActionListener(a -> setNodeColor(node, key));
                colors.add(item);
            }
            menu.add(colors);
            menu.addSeparator();
            JMenuItem dup = new JMenuItem(selection.size() > 1 ? "Duplicar seleção" : "Duplicar");
            dup.addActionListener(a -> duplicateSelection());
            menu.add(dup);
            if (selection.size() > 1) {
                JMenuItem group = new JMenuItem("Agrupar seleção");
                group.addActionListener(a -> groupSelection());
                menu.add(group);
            }
            menu.addSeparator();
            JMenuItem del = new JMenuItem(selection.size() > 1
                    ? "Remover " + selection.size() + " cartões" : "Remover cartão");
            del.addActionListener(a -> deleteSelection());
            menu.add(del);
            menu.show(this, e.getX(), e.getY());
            repaint();
            return;
        }
        CanvasEdge edge = pickEdge(w);
        if (edge != null) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem label = new JMenuItem("Editar etiqueta");
            label.addActionListener(a -> editEdgeLabel(edge));
            menu.add(label);
            JMenu colors = new JMenu("Cor");
            for (String key : EDGE_COLORS.keySet()) {
                JMenuItem item = new JMenuItem(COLOR_NAMES.get(key));
                item.addActionListener(a -> setEdgeColor(edge, key));
                colors.add(item);
            }
            menu.add(colors);
            JMenu dir = new JMenu("Direção");
            String[][] opts = {{"→", "none", "arrow"}, {"←", "arrow", "none"},
                    {"↔", "arrow", "arrow"}, {"—", "none", "none"}};
            for (String[] o : opts) {
                JMenuItem item = new JMenuItem(o[0]);
                item.addActionListener(a -> setEdgeDirection(edge, o[1], o[2]));
                dir.add(item);
            }
            menu.add(dir);
            menu.addSeparator();
            JMenuItem del = new JMenuItem("Remover ligação");
            del.addActionListener(a -> deleteEdge(edge));
            menu.add(del);
            menu.show(this, e.getX(), e.getY());
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem addCard = new JMenuItem("Adicionar cartão aqui");
        addCard.addActionListener(a -> addCard(w));
        JMenuItem addNote = new JMenuItem("Adicionar nota...");
        addNote.addActionListener(a -> {
            Note choice = chooseNote(this);
            if (choice != null) addNoteNode(choice, w);
        });
        JMenuItem addImg = new JMenuItem("Adicionar imagem...");
        addImg.addActionListener(a -> addImage(this, w));
        JMenuItem addGroup = new JMenuItem("Adicionar grupo aqui");
        addGroup.addActionListener(a -> addGroup(w));
        menu.add(addCard);
        menu.add(addNote);
        menu.add(addImg);
        menu.add(addGroup);
        menu.show(this, e.getX(), e.getY());
    }

    // ---------- operações ----------

    private void addCard(Point2D.Double at) {
        CanvasNode n = new CanvasNode();
        n.setCanvasId(boardId);
        n.setX(at.x - n.getW() / 2);
        n.setY(at.y - n.getH() / 2);
        try {
            canvasRepo.addNode(n);
            nodes.add(n);
            selection.clear();
            selection.add(n);
            repaint();
            editNodeText(n, "Texto do cartão");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void addNoteNode(Note note, Point2D.Double at) {
        CanvasNode n = new CanvasNode();
        n.setCanvasId(boardId);
        n.setKind(CanvasNode.KIND_NOTE);
        n.setNoteId(note.getId());
        n.setX(at.x - n.getW() / 2);
        n.setY(at.y - n.getH() / 2);
        try {
            canvasRepo.addNode(n);
            nodes.add(n);
            refreshNotes();
            selection.clear();
            selection.add(n);
            repaint();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void addImage(Component parent, Point2D.Double at) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Adicionar imagem ao canvas");
        chooser.setFileFilter(new FileNameExtensionFilter("Imagens", "png", "jpg", "jpeg", "gif", "bmp", "webp"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try {
            File src = chooser.getSelectedFile();
            String name = src.getName();
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".png";
            Path dir = canvasRepo.dataDir().resolve("canvas");
            Files.createDirectories(dir);
            String rel = "canvas/" + UUID.randomUUID() + ext;
            Files.copy(src.toPath(), canvasRepo.dataDir().resolve(rel));

            CanvasNode n = new CanvasNode();
            n.setCanvasId(boardId);
            n.setKind(CanvasNode.KIND_IMAGE);
            n.setText(rel);
            n.setW(320);
            n.setH(240);
            n.setX(at.x - n.getW() / 2);
            n.setY(at.y - n.getH() / 2);
            canvasRepo.addNode(n);
            nodes.add(n);
            selection.clear();
            selection.add(n);
            repaint();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void addGroup(Point2D.Double at) {
        CanvasNode g = new CanvasNode();
        g.setCanvasId(boardId);
        g.setKind(CanvasNode.KIND_GROUP);
        g.setText("Grupo");
        g.setW(420);
        g.setH(300);
        g.setX(at.x - g.getW() / 2);
        g.setY(at.y - g.getH() / 2);
        try {
            canvasRepo.addNode(g);
            nodes.add(0, g);
            selection.clear();
            selection.add(g);
            repaint();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Seletor de notas; devolve a nota escolhida ou null se cancelado/sem notas. */
    private Note chooseNote(Component parent) {
        List<Note> notes;
        try {
            notes = noteRepo.list(null, false, false, null);
        } catch (SQLException ex) {
            showError(ex);
            return null;
        }
        if (notes.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Ainda não há notas para adicionar.",
                    "Canvas", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        JList<Note> list = new JList<>(notes.toArray(new Note[0]));
        list.setCellRenderer((l, note, i, sel, focus) -> {
            JLabel lbl = new JLabel(note.displayTitle());
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            lbl.setOpaque(true);
            lbl.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            lbl.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            return lbl;
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(300, 260));
        int r = JOptionPane.showConfirmDialog(parent, scroll,
                "Adicionar nota ao canvas", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION ? list.getSelectedValue() : null;
    }

    private void editNodeText(CanvasNode n, String title) {
        JTextArea area = new JTextArea(n.getText(), 8, 28);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        int r = JOptionPane.showConfirmDialog(this, new JScrollPane(area),
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        n.setText(area.getText());
        saveNode(n);
    }

    private void editEdgeLabel(CanvasEdge e) {
        String label = JOptionPane.showInputDialog(this, "Etiqueta da ligação:", e.getLabel());
        if (label == null) return;
        e.setLabel(label.trim());
        saveEdge(e);
    }

    private void setNodeColor(CanvasNode n, String color) {
        n.setColor(color);
        saveNode(n);
    }

    private void setEdgeColor(CanvasEdge e, String color) {
        e.setColor(color);
        saveEdge(e);
    }

    private void setEdgeDirection(CanvasEdge e, String fromEnd, String toEnd) {
        e.setFromEnd(fromEnd);
        e.setToEnd(toEnd);
        saveEdge(e);
    }

    private void deleteSelection() {
        if (selection.isEmpty()) return;
        List<CanvasNode> doomed = new ArrayList<>(selection);
        selection.clear();
        for (CanvasNode n : doomed) {
            try {
                canvasRepo.deleteNode(n.getId());
                nodes.remove(n);
                edges.removeIf(e -> e.getFromNode() == n.getId() || e.getToNode() == n.getId());
            } catch (SQLException ex) {
                showError(ex);
            }
        }
        repaint();
    }

    private void deleteEdge(CanvasEdge edge) {
        try {
            canvasRepo.deleteEdge(edge.getId());
            edges.remove(edge);
            repaint();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Duplica os cartões selecionados (com as ligações entre eles), deslocados 40px. */
    private void duplicateSelection() {
        if (selection.isEmpty()) return;
        Map<Long, CanvasNode> copies = new HashMap<>();
        List<CanvasNode> newSel = new ArrayList<>();
        try {
            for (CanvasNode src : new ArrayList<>(selection)) {
                CanvasNode copy = new CanvasNode();
                copy.setCanvasId(boardId);
                copy.setKind(src.getKind());
                copy.setNoteId(src.getNoteId());
                copy.setText(src.getText());
                copy.setX(src.getX() + 40);
                copy.setY(src.getY() + 40);
                copy.setW(src.getW());
                copy.setH(src.getH());
                copy.setColor(src.getColor());
                canvasRepo.addNode(copy);
                nodes.add(copy);
                copies.put(src.getId(), copy);
                newSel.add(copy);
            }
            for (CanvasEdge e : new ArrayList<>(edges)) {
                CanvasNode from = copies.get(e.getFromNode());
                CanvasNode to = copies.get(e.getToNode());
                if (from != null && to != null) {
                    CanvasEdge dup = canvasRepo.addEdge(boardId, from.getId(), to.getId());
                    dup.setLabel(e.getLabel());
                    dup.setColor(e.getColor());
                    dup.setFromEnd(e.getFromEnd());
                    dup.setToEnd(e.getToEnd());
                    canvasRepo.updateEdge(dup);
                    edges.add(dup);
                }
            }
        } catch (SQLException ex) {
            showError(ex);
        }
        selection.clear();
        selection.addAll(newSel);
        repaint();
    }

    private void openImage(CanvasNode n) {
        try {
            Path p = canvasRepo.dataDir().resolve(n.getText());
            if (Desktop.isDesktopSupported() && Files.exists(p)) Desktop.getDesktop().open(p.toFile());
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void saveNode(CanvasNode n) {
        try {
            canvasRepo.updateNode(n);
            repaint();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void saveEdge(CanvasEdge e) {
        try {
            canvasRepo.updateEdge(e);
            repaint();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // ---------- picking ----------

    private Point2D.Double toWorld(Point p) {
        return new Point2D.Double(p.x / zoom + originX, p.y / zoom + originY);
    }

    private Point centerScreen() {
        return new Point(getWidth() / 2, getHeight() / 2);
    }

    private Point2D.Double viewCenter() {
        return new Point2D.Double(originX + getWidth() / (2 * zoom), originY + getHeight() / (2 * zoom));
    }

    private void bringToFront(CanvasNode n) {
        nodes.remove(n);
        nodes.add(n);
    }

    /** Cartão mais à frente sob o ponto (mundo), ou null; grupos ficam sempre por baixo. */
    private CanvasNode pickNode(Point2D.Double w) {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = nodes.size() - 1; i >= 0; i--) {
                CanvasNode n = nodes.get(i);
                if ((pass == 0) == n.isGroup()) continue;
                if (w.x >= n.getX() && w.x <= n.getX() + n.getW()
                        && w.y >= n.getY() && w.y <= n.getY() + n.getH()) return n;
            }
        }
        return null;
    }

    /** Cartão cuja porta de saída (ponto à direita) está sob o cursor, ou null. */
    private CanvasNode pickPort(Point2D.Double w) {
        double tol = (PORT_R + 5) / zoom;
        for (int i = nodes.size() - 1; i >= 0; i--) {
            CanvasNode n = nodes.get(i);
            if (w.distance(outPort(n)) <= tol) return n;
        }
        return null;
    }

    /** Ligação cuja curva ou etiqueta passa perto do ponto (mundo), ou null. */
    private CanvasEdge pickEdge(Point2D.Double w) {
        double tol = 8 / zoom;
        for (int i = edges.size() - 1; i >= 0; i--) {
            CanvasEdge e = edges.get(i);
            CanvasNode from = nodeById(e.getFromNode());
            CanvasNode to = nodeById(e.getToNode());
            if (from == null || to == null) continue;
            CubicCurve2D.Double c = edgeCurve(from, to);
            Point2D.Double prev = new Point2D.Double(c.getX1(), c.getY1());
            for (int s = 1; s <= 32; s++) {
                Point2D.Double p = eval(c, s / 32.0);
                if (distToSegment(w, prev, p) <= tol) return e;
                prev = p;
            }
        }
        return null;
    }

    private CanvasNode nodeById(long id) {
        for (CanvasNode n : nodes) if (n.getId() == id) return n;
        return null;
    }

    private boolean overHandle(CanvasNode n, Point2D.Double w) {
        double s = HANDLE / zoom;
        return w.x >= n.getX() + n.getW() - s && w.x <= n.getX() + n.getW() + s / 2
                && w.y >= n.getY() + n.getH() - s && w.y <= n.getY() + n.getH() + s / 2;
    }

    /** true se o cartão {@code inner} está totalmente dentro do grupo {@code group}. */
    private static boolean containsFully(CanvasNode group, CanvasNode inner) {
        return inner.getX() >= group.getX() && inner.getY() >= group.getY()
                && inner.getX() + inner.getW() <= group.getX() + group.getW()
                && inner.getY() + inner.getH() <= group.getY() + group.getH();
    }

    private static Rectangle2D.Double rect(Point2D.Double a, Point2D.Double b) {
        return new Rectangle2D.Double(Math.min(a.x, b.x), Math.min(a.y, b.y),
                Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    private static double distToSegment(Point2D.Double p, Point2D.Double a, Point2D.Double b) {
        double dx = b.x - a.x, dy = b.y - a.y;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        return p.distance(a.x + t * dx, a.y + t * dy);
    }

    private static Point2D.Double eval(CubicCurve2D.Double c, double t) {
        double u = 1 - t;
        double x = u * u * u * c.getX1() + 3 * u * u * t * c.getCtrlX1()
                + 3 * u * t * t * c.getCtrlX2() + t * t * t * c.getX2();
        double y = u * u * u * c.getY1() + 3 * u * u * t * c.getCtrlY1()
                + 3 * u * t * t * c.getCtrlY2() + t * t * t * c.getY2();
        return new Point2D.Double(x, y);
    }

    // ---------- exportação ----------

    /** Caixa que envolve todos os cartões do quadro, ou {@code null} se estiver vazio. */
    public Rectangle2D.Double contentBounds() {
        if (nodes.isEmpty()) return null;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (CanvasNode n : nodes) {
            minX = Math.min(minX, n.getX());
            minY = Math.min(minY, n.getY());
            maxX = Math.max(maxX, n.getX() + n.getW());
            maxY = Math.max(maxY, n.getY() + n.getH());
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Pinta o quadro em coordenadas do mundo, sem grelha, seleção nem pegas —
     * é o que a exportação para imagem usa. O contexto recebido já deve estar
     * transladado para a origem do conteúdo.
     */
    public void paintForExport(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont());
        g2.setColor(getForeground());

        CanvasNode keepHovered = hovered;
        java.util.List<CanvasNode> keepSelection = new ArrayList<>(selection);
        double keepZoom = zoom;
        hovered = null;
        selection.clear();
        zoom = 1; // as espessuras de traço são divididas pelo zoom
        try {
            for (CanvasNode n : nodes) if (n.isGroup()) paintGroup(g2, n);
            for (CanvasEdge e : edges) paintEdge(g2, e);
            for (CanvasNode n : nodes) if (!n.isGroup()) paintNode(g2, n);
        } finally {
            hovered = keepHovered;
            selection.addAll(keepSelection);
            zoom = keepZoom;
        }
    }

    /** Cópia dos cartões do quadro (para o escritor de SVG). */
    public java.util.List<CanvasNode> nodesSnapshot() {
        return new ArrayList<>(nodes);
    }

    /** Cópia das ligações do quadro (para o escritor de SVG). */
    public java.util.List<CanvasEdge> edgesSnapshot() {
        return new ArrayList<>(edges);
    }

    /** Título a mostrar num cartão de nota (ou {@code null} se o cartão não é uma nota). */
    public String exportTitleOf(CanvasNode n) {
        if (!n.isNote()) return null;
        Note note = notesById.get(n.getNoteId());
        return note != null ? note.displayTitle() : "(nota apagada)";
    }

    /** Corpo a mostrar num cartão. */
    public String exportBodyOf(CanvasNode n) {
        if (!n.isNote()) return n.getText();
        Note note = notesById.get(n.getNoteId());
        return note != null ? note.snippet() : "";
    }

    /** Cor de preenchimento de um cartão, resolvida pela paleta do canvas. */
    public Color exportFillOf(CanvasNode n) {
        return NODE_COLORS.getOrDefault(n.getColor(), Theme.CARD_BG);
    }

    /** Cor de uma ligação, resolvida pela paleta do canvas. */
    public Color exportColorOf(CanvasEdge e) {
        return EDGE_COLORS.getOrDefault(e.getColor(), EDGE_COLORS.get(""));
    }

    /** A curva desenhada entre dois cartões, em coordenadas do mundo. */
    public CubicCurve2D.Double exportCurveOf(CanvasEdge e) {
        CanvasNode from = nodeById(e.getFromNode());
        CanvasNode to = nodeById(e.getToNode());
        return from == null || to == null ? null : edgeCurve(from, to);
    }

    // ---------- pintura ----------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        paintGrid(g2);

        g2.scale(zoom, zoom);
        g2.translate(-originX, -originY);

        for (CanvasNode n : nodes) if (n.isGroup()) paintGroup(g2, n);
        for (CanvasEdge e : edges) paintEdge(g2, e);
        if (drag == Drag.EDGE && edgeSource != null && edgeMouse != null) {
            CubicCurve2D.Double c = curve(outPort(edgeSource), edgeMouse);
            g2.setColor(Theme.accent());
            g2.setStroke(new BasicStroke((float) (2 / zoom)));
            g2.draw(c);
        }
        for (CanvasNode n : nodes) if (!n.isGroup()) paintNode(g2, n);

        if (drag == Drag.SELECT && rubberStart != null && rubberCur != null) {
            Rectangle2D.Double r = rect(rubberStart, rubberCur);
            Color accent = Theme.accent();
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
            g2.fill(r);
            g2.setColor(accent);
            g2.setStroke(new BasicStroke((float) (1 / zoom)));
            g2.draw(r);
        }

        g2.dispose();
    }

    private void paintGrid(Graphics2D g2) {
        int step = 40;
        g2.setColor(Theme.SEPARATOR);
        for (int x = step / 2; x < getWidth(); x += step)
            for (int y = step / 2; y < getHeight(); y += step)
                g2.fillRect(x, y, 2, 2);
    }

    private static Point2D.Double outPort(CanvasNode n) {
        return new Point2D.Double(n.getX() + n.getW(), n.getY() + n.getH() / 2);
    }

    private static Point2D.Double inPort(CanvasNode n) {
        return new Point2D.Double(n.getX(), n.getY() + n.getH() / 2);
    }

    private static CubicCurve2D.Double edgeCurve(CanvasNode from, CanvasNode to) {
        return curve(outPort(from), inPort(to));
    }

    private static CubicCurve2D.Double curve(Point2D.Double a, Point2D.Double b) {
        double dx = Math.max(50, Math.abs(b.x - a.x) / 2);
        return new CubicCurve2D.Double(a.x, a.y, a.x + dx, a.y, b.x - dx, b.y, b.x, b.y);
    }

    private void paintEdge(Graphics2D g2, CanvasEdge e) {
        CanvasNode from = nodeById(e.getFromNode());
        CanvasNode to = nodeById(e.getToNode());
        if (from == null || to == null) return;
        CubicCurve2D.Double c = edgeCurve(from, to);
        boolean hot = selection.contains(from) || selection.contains(to);
        Color base = EDGE_COLORS.getOrDefault(e.getColor(), EDGE_COLORS.get(""));
        g2.setColor(hot ? Theme.accent() : base);
        g2.setStroke(new BasicStroke((float) ((hot ? 2.2 : 1.6) / zoom)));
        g2.draw(c);

        double s = 9 / zoom;
        if ("arrow".equals(e.getToEnd())) {
            Point2D.Double near = eval(c, 0.97);
            Point2D.Double end = new Point2D.Double(c.getX2(), c.getY2());
            drawArrow(g2, end, Math.atan2(end.y - near.y, end.x - near.x), s);
        }
        if ("arrow".equals(e.getFromEnd())) {
            Point2D.Double near = eval(c, 0.03);
            Point2D.Double start = new Point2D.Double(c.getX1(), c.getY1());
            drawArrow(g2, start, Math.atan2(start.y - near.y, start.x - near.x), s);
        }

        if (!e.getLabel().isEmpty()) {
            Point2D.Double mid = eval(c, 0.5);
            Font f = getFont().deriveFont(Font.PLAIN, 11f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            double tw = fm.stringWidth(e.getLabel());
            double th = fm.getHeight();
            RoundRectangle2D.Double chip = new RoundRectangle2D.Double(
                    mid.x - tw / 2 - 8, mid.y - th / 2 - 4, tw + 16, th + 8, 10, 10);
            g2.setColor(Theme.CARD_BG);
            g2.fill(chip);
            g2.setColor(base);
            g2.setStroke(new BasicStroke((float) (1 / zoom)));
            g2.draw(chip);
            g2.setColor(Theme.TEXT);
            g2.drawString(e.getLabel(), (float) (mid.x - tw / 2), (float) (mid.y + fm.getAscent() / 2.0));
        }
    }

    private static void drawArrow(Graphics2D g2, Point2D.Double tip, double ang, double s) {
        Path2D.Double arrow = new Path2D.Double();
        arrow.moveTo(tip.x, tip.y);
        arrow.lineTo(tip.x - s * Math.cos(ang - 0.45), tip.y - s * Math.sin(ang - 0.45));
        arrow.lineTo(tip.x - s * Math.cos(ang + 0.45), tip.y - s * Math.sin(ang + 0.45));
        arrow.closePath();
        g2.fill(arrow);
    }

    private void paintGroup(Graphics2D g2, CanvasNode n) {
        double x = n.getX(), y = n.getY(), w = n.getW(), h = n.getH();
        Color base = NODE_COLORS.getOrDefault(n.getColor(), Theme.CARD_BG);
        boolean tinted = !n.getColor().isEmpty();
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), tinted ? 90 : 60));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 16, 16));
        boolean sel = selection.contains(n);
        g2.setColor(sel ? Theme.accent() : tinted
                ? new Color(base.getRed(), base.getGreen(), base.getBlue(), 200)
                : Theme.SEPARATOR);
        g2.setStroke(new BasicStroke((float) ((sel ? 2 : 1.2) / zoom)));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 16, 16));

        if (!n.getText().isEmpty()) {
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g2.setColor(Theme.dimForeground());
            g2.drawString(n.getText(), (float) (x + PAD), (float) (y + PAD + g2.getFontMetrics().getAscent()));
        }
        paintHandles(g2, n, sel);
    }

    private void paintNode(Graphics2D g2, CanvasNode n) {
        double x = n.getX(), y = n.getY(), w = n.getW(), h = n.getH();

        g2.setColor(NODE_COLORS.getOrDefault(n.getColor(), Theme.CARD_BG));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 14, 14));

        boolean sel = selection.contains(n);
        g2.setColor(sel ? Theme.accent() : Theme.borderColor());
        g2.setStroke(new BasicStroke((float) ((sel ? 2 : 1) / zoom)));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 14, 14));

        if (n.isImage()) {
            paintImage(g2, n, x, y, w, h);
        } else {
            String title = null;
            String body = n.getText();
            if (n.isNote()) {
                Note note = notesById.get(n.getNoteId());
                title = note != null ? note.displayTitle() : "(nota apagada)";
                body = note != null ? note.snippet() : "";
            }
            Shape oldClip = g2.getClip();
            g2.clip(new RoundRectangle2D.Double(x, y, w, h, 14, 14));
            double ty = y + PAD;
            if (title != null) {
                g2.setFont(getFont().deriveFont(Font.BOLD));
                g2.setColor(getForeground());
                ty = drawWrapped(g2, title, x + PAD, ty, w - 2 * PAD) + 4;
            }
            g2.setFont(getFont().deriveFont(Font.PLAIN));
            g2.setColor(title != null ? Theme.dimForeground() : getForeground());
            drawWrapped(g2, body, x + PAD, ty, w - 2 * PAD);
            g2.setClip(oldClip);
        }
        paintHandles(g2, n, sel);
    }

    /** Porta de ligação e pega de redimensionamento (quando o cartão está sob o cursor/selecionado). */
    private void paintHandles(Graphics2D g2, CanvasNode n, boolean sel) {
        if (n != hovered && !sel) return;
        Point2D.Double p = outPort(n);
        double r = PORT_R / zoom;
        g2.setColor(Theme.accent());
        g2.fill(new Ellipse2D.Double(p.x - r, p.y - r, 2 * r, 2 * r));
        if (sel && selection.size() == 1) {
            double hs = HANDLE / zoom;
            g2.fill(new RoundRectangle2D.Double(n.getX() + n.getW() - hs, n.getY() + n.getH() - hs,
                    hs, hs, 3 / zoom, 3 / zoom));
        }
    }

    private void paintImage(Graphics2D g2, CanvasNode n, double x, double y, double w, double h) {
        // containsKey+put (em vez de computeIfAbsent) para memorizar também os falhanços
        // e não sondar o disco em cada repaint
        Image img;
        if (imageCache.containsKey(n.getText())) {
            img = imageCache.get(n.getText());
        } else {
            try {
                Path p = canvasRepo.dataDir().resolve(n.getText());
                img = Files.exists(p) ? new ImageIcon(p.toString()).getImage() : null;
            } catch (Exception ex) {
                img = null;
            }
            imageCache.put(n.getText(), img);
        }
        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Double(x, y, w, h, 14, 14));
        if (img != null) {
            int iw = img.getWidth(null), ih = img.getHeight(null);
            if (iw > 0 && ih > 0) {
                double scale = Math.min((w - 2 * PAD) / iw, (h - 2 * PAD) / ih);
                double dw = iw * scale, dh = ih * scale;
                g2.drawImage(img, (int) (x + (w - dw) / 2), (int) (y + (h - dh) / 2),
                        (int) dw, (int) dh, null);
            }
        } else {
            g2.setColor(Theme.dimForeground());
            g2.drawString("(imagem não encontrada)", (float) (x + PAD), (float) (y + PAD + 14));
        }
        g2.setClip(oldClip);
    }

    /** Desenha texto com quebra de linha por palavras; devolve o y a seguir ao texto. */
    private double drawWrapped(Graphics2D g2, String text, double x, double y, double maxW) {
        if (text == null || text.isEmpty()) return y;
        FontMetrics fm = g2.getFontMetrics();
        for (String line : wrap(text, fm, maxW)) {
            g2.drawString(line, (float) x, (float) (y + fm.getAscent()));
            y += fm.getHeight();
        }
        return y;
    }

    private static List<String> wrap(String text, FontMetrics fm, double maxW) {
        List<String> lines = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String cand = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && fm.stringWidth(cand) > maxW) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(cand);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    // ---------- zoom ----------

    /** Zoom animado ancorado no ponto de ecrã {@code screen}: o ponto sob o cursor não se move. */
    private void zoomAt(Point screen, double newZoom) {
        double z1 = clamp(newZoom);
        zoomTarget = z1;
        if (z1 == zoom) return;
        double z0 = zoom;
        double wx = screen.x / z0 + originX; // ponto do mundo a manter fixo no ecrã
        double wy = screen.y / z0 + originY;
        animateView(t -> {
            zoom = z0 + (z1 - z0) * t;
            originX = wx - screen.x / zoom;
            originY = wy - screen.y / zoom;
        });
    }

    /** Interpola a vista com ease-out curto; {@code frame} aplica o zoom/origem de cada passo. */
    private void animateView(DoubleConsumer frame) {
        if (viewAnim != null) viewAnim.cancel();
        viewAnim = Animator.animate(Duration.ofMillis(180), Easing.EASE_OUT, t -> {
            frame.accept(t);
            onZoomChanged.run();
            repaint();
        });
    }

    private static double clamp(double z) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
