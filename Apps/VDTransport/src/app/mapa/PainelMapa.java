package app.mapa;

import app.modelo.Paragem;
import app.modelo.Rota;
import app.modelo.Veiculo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Mapa real de Moçambique (tiles OpenStreetMap via JXMapViewer2) com as
 * camadas de rotas, paragens e veículos animados desenhadas por cima.
 */
public class PainelMapa extends JXMapViewer {

    private static final Color COR_PARAGEM_FORA = new Color(0x9E9E9E);
    private static final Color COR_PARAGEM = new Color(0x37474F);
    private static final Color COR_PERCURSO = new Color(0xFF6F00);

    private final List<Rota> rotas;
    private final List<Paragem> paragens;
    private final List<Veiculo> veiculos;
    private Rota rotaSelecionada;
    private List<Paragem> percurso = List.of();
    private Consumer<GeoPosition> aoClicar;

    public PainelMapa(List<Rota> rotas, List<Paragem> paragens, List<Veiculo> veiculos) {
        this.rotas = rotas;
        this.paragens = paragens;
        this.veiculos = veiculos;

        DefaultTileFactory fabrica = new DefaultTileFactory(new OSMTileFactoryInfo());
        fabrica.setThreadPoolSize(8);
        setTileFactory(fabrica);
        setCenterPosition(new GeoPosition(-18.7, 35.5)); // centro aproximado de Moçambique
        setZoom(13); // no JXMapViewer2 valores maiores = mais afastado

        PanMouseInputListener arrastar = new PanMouseInputListener(this);
        addMouseListener(arrastar);
        addMouseMotionListener(arrastar);
        addMouseWheelListener(new ZoomMouseWheelListenerCursor(this));
        addKeyListener(new PanKeyListener(this));
        setFocusable(true);

        setOverlayPainter(this::pintarCamadas);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Consumer<GeoPosition> c = aoClicar;
                if (c != null && SwingUtilities.isLeftMouseButton(e)) {
                    c.accept(convertPointToGeoPosition(e.getPoint()));
                }
            }
        });
    }

    public void setRotaSelecionada(Rota rota) {
        this.rotaSelecionada = rota;
        repaint();
    }

    /** Percurso calculado a destacar por cima das rotas (null ou vazio limpa-o). */
    public void setPercurso(List<Paragem> percurso) {
        this.percurso = percurso == null ? List.of() : percurso;
        repaint();
    }

    /** Activa (ou desactiva, com null) o modo "clique no mapa cria paragem". */
    public void setModoAdicionarParagem(Consumer<GeoPosition> accao) {
        this.aoClicar = accao;
        setCursor(accao != null
                ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : Cursor.getDefaultCursor());
    }

    /** Ajusta o zoom para mostrar todas as paragens. */
    public void enquadrar() {
        enquadrar(paragens);
    }

    /** Ajusta o zoom e o centro para mostrar as paragens indicadas. */
    public void enquadrar(List<Paragem> alvo) {
        if (alvo == null || alvo.isEmpty()) {
            return;
        }
        Set<GeoPosition> posicoes = new HashSet<>();
        for (Paragem p : alvo) {
            posicoes.add(p.geo());
        }
        if (posicoes.size() == 1) {
            setCenterPosition(posicoes.iterator().next()); // uma só paragem: centrar, sem mexer no zoom
            return;
        }
        calculateZoomFrom(posicoes);
    }

    public void zoomMais() {
        setZoom(Math.max(getTileFactory().getInfo().getMinimumZoomLevel(), getZoom() - 1));
    }

    public void zoomMenos() {
        setZoom(Math.min(getTileFactory().getInfo().getMaximumZoomLevel(), getZoom() + 1));
    }

    // ---------------- desenho ----------------

    private void pintarCamadas(Graphics2D g, JXMapViewer mapa, int largura, int altura) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle vista = mapa.getViewportBounds();
        for (Rota rota : rotas) {
            pintarRota(g, mapa, vista, rota);
        }
        pintarPercurso(g, mapa, vista);
        for (Paragem p : paragens) {
            pintarParagem(g, mapa, vista, p);
        }
        for (Veiculo v : veiculos) {
            pintarVeiculo(g, mapa, vista, v);
        }
    }

    private Point2D pixel(JXMapViewer mapa, Rectangle vista, GeoPosition pos) {
        Point2D p = mapa.getTileFactory().geoToPixel(pos, mapa.getZoom());
        return new Point2D.Double(p.getX() - vista.getX(), p.getY() - vista.getY());
    }

    private void pintarRota(Graphics2D g, JXMapViewer mapa, Rectangle vista, Rota rota) {
        List<Paragem> ps = rota.getParagens();
        if (ps.size() < 2) {
            return;
        }
        boolean seleccionada = rota == rotaSelecionada;

        Path2D caminho = new Path2D.Double();
        for (int i = 0; i < ps.size(); i++) {
            Point2D p = pixel(mapa, vista, ps.get(i).geo());
            if (i == 0) {
                caminho.moveTo(p.getX(), p.getY());
            } else {
                caminho.lineTo(p.getX(), p.getY());
            }
        }

        Color cor = rota.getCor();
        if (rotaSelecionada != null && !seleccionada) {
            cor = new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 70); // esbatidas
        }
        if (seleccionada) { // halo por baixo da linha
            g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 60));
            g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(caminho);
        }
        g.setColor(cor);
        g.setStroke(new BasicStroke(seleccionada ? 4.5f : 3f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(caminho);
    }

    private void pintarPercurso(Graphics2D g, JXMapViewer mapa, Rectangle vista) {
        if (percurso.size() < 2) {
            return;
        }
        Path2D caminho = new Path2D.Double();
        for (int i = 0; i < percurso.size(); i++) {
            Point2D p = pixel(mapa, vista, percurso.get(i).geo());
            if (i == 0) {
                caminho.moveTo(p.getX(), p.getY());
            } else {
                caminho.lineTo(p.getX(), p.getY());
            }
        }
        g.setColor(Color.WHITE); // contorno, para o percurso se ler por cima de qualquer rota
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(caminho);
        g.setColor(COR_PERCURSO);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(caminho);

        marcarExtremo(g, mapa, vista, percurso.get(0));
        marcarExtremo(g, mapa, vista, percurso.get(percurso.size() - 1));
    }

    private void marcarExtremo(Graphics2D g, JXMapViewer mapa, Rectangle vista, Paragem p) {
        Point2D pt = pixel(mapa, vista, p.geo());
        int x = (int) pt.getX();
        int y = (int) pt.getY();
        g.setColor(Color.WHITE);
        g.fillOval(x - 7, y - 7, 14, 14);
        g.setColor(COR_PERCURSO);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(x - 7, y - 7, 14, 14);
    }

    private void pintarParagem(Graphics2D g, JXMapViewer mapa, Rectangle vista, Paragem p) {
        Point2D pt = pixel(mapa, vista, p.geo());
        int x = (int) pt.getX();
        int y = (int) pt.getY();
        boolean naRota = rotaSelecionada != null && rotaSelecionada.getParagens().contains(p);

        if (rotaSelecionada != null && !naRota) {
            g.setColor(COR_PARAGEM_FORA);
            g.fillOval(x - 3, y - 3, 6, 6);
            return;
        }
        g.setColor(naRota ? rotaSelecionada.getCor() : COR_PARAGEM);
        g.fillOval(x - 5, y - 5, 10, 10);
        g.setColor(Color.WHITE);
        g.drawOval(x - 5, y - 5, 10, 10);
        if (naRota || mapa.getZoom() <= 11) {
            g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            g.setColor(new Color(0, 0, 0, 170));
            g.drawString(p.getNome(), x + 8, y + 4);
        }
    }

    private void pintarVeiculo(Graphics2D g, JXMapViewer mapa, Rectangle vista, Veiculo v) {
        GeoPosition pos = v.getPosicao();
        if (pos == null) {
            return;
        }
        Point2D pt = pixel(mapa, vista, pos);
        int x = (int) pt.getX();
        int y = (int) pt.getY();
        Color cor = v.getRota() != null ? v.getRota().getCor() : Color.DARK_GRAY;

        g.setColor(cor);
        g.fillOval(x - 9, y - 9, 18, 18);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x - 9, y - 9, 18, 18);
        iconeTipo(v.getTipo()).paintIcon(mapa, g, x - 5, y - 5);
        g.setFont(getFont().deriveFont(Font.BOLD, 11f));
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(v.getMatricula(), x + 12, y - 4);
    }

    private static FontIcon iconeTipo(String tipo) {
        FontAwesomeSolid ikon;
        switch (tipo) {
            case "Machimbombo":
                ikon = FontAwesomeSolid.BUS_ALT;
                break;
            case "Chapa":
                ikon = FontAwesomeSolid.SHUTTLE_VAN;
                break;
            case "Comboio":
                ikon = FontAwesomeSolid.TRAIN;
                break;
            default:
                ikon = FontAwesomeSolid.BUS;
        }
        return FontIcon.of(ikon, 10, Color.WHITE);
    }
}
