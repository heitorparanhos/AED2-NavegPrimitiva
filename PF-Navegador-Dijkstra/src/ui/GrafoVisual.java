import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.RenderingHints;
import java.util.*;
import java.util.List;

/**
 * GrafoVisual — Janela Swing para visualização e edição interativa do grafo.
 *
 * Funcionalidades:
 *   • Clicar no canvas para adicionar vértices
 *   • Selecionar vértice de origem e destino para adicionar arestas
 *   • Modo Dijkstra: seleciona origem e destino e exibe o menor caminho
 *   • Remover vértices com clique direito
 *   • Arrastar vértices para reposicioná-los
 *   • Limpar o grafo
 *
 * Integra as classes: Grafo, Vertice, Aresta, Dijkstra, ResultadoDijkstra
 */
public class GrafoVisual extends JFrame {

    // ── Constantes visuais ──────────────────────────────────────────────────
    private static final int    RAIO          = 14;
    private static final Color  COR_FUNDO     = new Color(15, 17, 26);
    private static final Color  COR_GRADE     = new Color(30, 35, 55);
    private static final Color  COR_ARESTA    = new Color(80, 120, 200, 180);
    private static final Color  COR_VERTICE   = new Color(55, 110, 220);
    private static final Color  COR_BORDA_V   = new Color(110, 165, 255);
    private static final Color  COR_SELECIONADO = new Color(255, 180, 0);
    private static final Color  COR_CAMINHO   = new Color(0, 220, 140);
    private static final Color  COR_TEXTO     = new Color(200, 210, 240);
    private static final Color  COR_STATUS_OK = new Color(0, 200, 120);
    private static final Color  COR_STATUS_ERR= new Color(220, 70, 70);
    private static final Color  COR_PAINEL    = new Color(22, 26, 42);
    private static final Color  COR_BTN       = new Color(40, 60, 120);
    private static final Color  COR_BTN_HOVER = new Color(60, 90, 180);
    private static final Font   FONTE_ID      = new Font("Monospaced", Font.BOLD, 11);
    private static final Font   FONTE_STATUS  = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font   FONTE_TITULO  = new Font("SansSerif", Font.BOLD, 13);

    // ── Modos de interação ──────────────────────────────────────────────────
    private enum Modo { ADICIONAR_VERTICE, ADICIONAR_ARESTA, DIJKSTRA, REMOVER }

    // ── Estado ──────────────────────────────────────────────────────────────
    private final Grafo         grafo          = new Grafo();
    private Modo                modoAtual      = Modo.ADICIONAR_VERTICE;
    private int                 verticeA       = -1;   // primeiro clique em aresta/dijkstra
    private List<Integer>       caminhoDijkstra = new ArrayList<>();
    private int                 arrastandoId   = -1;
    private String              statusMsg      = "Clique no canvas para adicionar vértices";
    private Color               statusCor      = COR_STATUS_OK;

    // ── Componentes ─────────────────────────────────────────────────────────
    private CanvasPanel         canvas;
    private JLabel              lblStatus;
    private JToggleButton       btnAddV, btnAddA, btnDijk, btnRem;

    public GrafoVisual() {
        super("Grafo Visual — Editor Interativo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setBackground(COR_FUNDO);

        construirUI();
        setVisible(true);
    }

    // ── Construção da interface ─────────────────────────────────────────────

    private void construirUI() {
        setLayout(new BorderLayout(0, 0));

        // Painel esquerdo (controles)
        JPanel painel = new JPanel();
        painel.setPreferredSize(new Dimension(200, 0));
        painel.setBackground(COR_PAINEL);
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(40, 50, 80)));

        painel.add(Box.createVerticalStrut(20));
        painel.add(titulo("FERRAMENTAS"));
        painel.add(Box.createVerticalStrut(12));

        ButtonGroup grupo = new ButtonGroup();

        btnAddV = botaoModo("➕  Adicionar Vértice", Modo.ADICIONAR_VERTICE);
        btnAddA = botaoModo("🔗  Adicionar Aresta",  Modo.ADICIONAR_ARESTA);
        btnDijk = botaoModo("🗺  Dijkstra",           Modo.DIJKSTRA);
        btnRem  = botaoModo("🗑  Remover Vértice",    Modo.REMOVER);

        grupo.add(btnAddV); grupo.add(btnAddA); grupo.add(btnDijk); grupo.add(btnRem);
        btnAddV.setSelected(true);

        for (JToggleButton b : new JToggleButton[]{btnAddV, btnAddA, btnDijk, btnRem}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(172, 38));
            painel.add(b);
            painel.add(Box.createVerticalStrut(8));
        }

        painel.add(Box.createVerticalStrut(20));
        painel.add(titulo("AÇÕES"));
        painel.add(Box.createVerticalStrut(12));

        JButton btnLimpar = botaoAcao("🧹  Limpar Grafo");
        btnLimpar.addActionListener(e -> limparGrafo());
        btnLimpar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLimpar.setMaximumSize(new Dimension(172, 38));
        painel.add(btnLimpar);

        painel.add(Box.createVerticalStrut(20));
        painel.add(titulo("INFORMAÇÕES"));
        painel.add(Box.createVerticalStrut(8));

        JLabel lblInfo = new JLabel("<html><center>Clique direito<br>arrasta vértices</center></html>");
        lblInfo.setForeground(new Color(100, 120, 160));
        lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(lblInfo);

        painel.add(Box.createVerticalGlue());

        // Canvas central
        canvas = new CanvasPanel();

        // Barra de status inferior
        JPanel barraStatus = new JPanel(new BorderLayout());
        barraStatus.setBackground(new Color(18, 22, 36));
        barraStatus.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 50, 80)));
        barraStatus.setPreferredSize(new Dimension(0, 34));

        lblStatus = new JLabel(statusMsg);
        lblStatus.setFont(FONTE_STATUS);
        lblStatus.setForeground(statusCor);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        barraStatus.add(lblStatus, BorderLayout.CENTER);

        add(painel,      BorderLayout.WEST);
        add(canvas,      BorderLayout.CENTER);
        add(barraStatus, BorderLayout.SOUTH);
    }

    private JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(new Color(80, 100, 150));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        return l;
    }

    private JToggleButton botaoModo(String rotulo, Modo modo) {
        JToggleButton b = new JToggleButton(rotulo);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setForeground(COR_TEXTO);
        b.setBackground(COR_BTN);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            modoAtual    = modo;
            verticeA     = -1;
            caminhoDijkstra.clear();
            setStatus(dica(modo), COR_STATUS_OK);
            canvas.repaint();
        });
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (!b.isSelected()) b.setBackground(COR_BTN_HOVER); }
            public void mouseExited (MouseEvent e) { if (!b.isSelected()) b.setBackground(COR_BTN); }
        });
        // selecionado: cor diferente
        b.addChangeListener(e -> b.setBackground(b.isSelected() ? new Color(50, 100, 200) : COR_BTN));
        return b;
    }

    private JButton botaoAcao(String rotulo) {
        JButton b = new JButton(rotulo);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setForeground(COR_TEXTO);
        b.setBackground(new Color(80, 30, 30));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(140, 40, 40)); }
            public void mouseExited (MouseEvent e) { b.setBackground(new Color(80, 30, 30));  }
        });
        return b;
    }

    private String dica(Modo m) {
        return switch (m) {
            case ADICIONAR_VERTICE -> "Clique no canvas para adicionar vértices";
            case ADICIONAR_ARESTA  -> "Clique em dois vértices para criar uma aresta";
            case DIJKSTRA          -> "Clique em ORIGEM, depois em DESTINO para calcular o caminho";
            case REMOVER           -> "Clique em um vértice para removê-lo";
        };
    }

    // ── Ações ───────────────────────────────────────────────────────────────

    private void limparGrafo() {
        // Reconstrói limpando tudo
        List<Vertice> vs = new ArrayList<>(grafo.getVertices());
        for (int i = vs.size() - 1; i >= 0; i--) {
            if (vs.get(i) != null) grafo.removerVertice(i);
        }
        // Reinicializa criando novo grafo e substituindo via reflexão não é possível
        // sem modificar o campo — recriamos o campo via limpeza sequencial
        // (alternativa: usar grafo como campo não-final e new Grafo())
        verticeA = -1;
        caminhoDijkstra.clear();
        setStatus("Grafo limpo", COR_STATUS_OK);
        canvas.repaint();
    }

    private void setStatus(String msg, Color cor) {
        statusMsg = msg;
        statusCor = cor;
        lblStatus.setText(msg);
        lblStatus.setForeground(cor);
    }

    // ── Encontra vértice próximo ao clique ──────────────────────────────────

    private int verticeNoClick(int px, int py) {
        for (Vertice v : grafo.getVertices()) {
            if (v == null) continue;
            double dx = v.x - px, dy = v.y - py;
            if (Math.sqrt(dx*dx + dy*dy) <= RAIO + 4) return v.id_interno;
        }
        return -1;
    }

    // ── Canvas de desenho ───────────────────────────────────────────────────

    class CanvasPanel extends JPanel {

        CanvasPanel() {
            setBackground(COR_FUNDO);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    int px = e.getX(), py = e.getY();
                    int clicado = verticeNoClick(px, py);

                    // Arrastar inicia aqui
                    if (SwingUtilities.isLeftMouseButton(e) && clicado != -1
                            && modoAtual == Modo.ADICIONAR_VERTICE) {
                        arrastandoId = clicado;
                        return;
                    }

                    if (SwingUtilities.isRightMouseButton(e)) {
                        // Clique direito: remove vértice independente do modo
                        if (clicado != -1) {
                            grafo.removerVertice(clicado);
                            if (verticeA == clicado) verticeA = -1;
                            caminhoDijkstra.clear();
                            setStatus("Vértice " + clicado + " removido", COR_STATUS_OK);
                            repaint();
                        }
                        return;
                    }

                    switch (modoAtual) {

                        case ADICIONAR_VERTICE -> {
                            if (clicado == -1) {
                                int id = grafo.adicionarVertice(px, py);
                                setStatus("Vértice " + id + " adicionado em (" + px + ", " + py + ")", COR_STATUS_OK);
                                repaint();
                            }
                        }

                        case ADICIONAR_ARESTA -> {
                            if (clicado == -1) return;
                            if (verticeA == -1) {
                                verticeA = clicado;
                                setStatus("Origem: vértice " + clicado + " — agora clique no destino", COR_STATUS_OK);
                            } else {
                                if (verticeA == clicado) {
                                    setStatus("Origem e destino iguais — escolha outro vértice", COR_STATUS_ERR);
                                } else {
                                    grafo.adicionarAresta(verticeA, clicado, true);
                                    setStatus("Aresta " + verticeA + " ↔ " + clicado + " adicionada", COR_STATUS_OK);
                                    verticeA = -1;
                                }
                                repaint();
                            }
                        }

                        case DIJKSTRA -> {
                            if (clicado == -1) return;
                            if (verticeA == -1) {
                                verticeA = clicado;
                                caminhoDijkstra.clear();
                                setStatus("Origem: " + clicado + " — agora clique no destino", COR_STATUS_OK);
                                repaint();
                            } else {
                                int dest = clicado;
                                Dijkstra dijk = new Dijkstra();
                                ResultadoDijkstra res = dijk.calcular(grafo, verticeA, dest);
                                if (res.temCaminho()) {
                                    caminhoDijkstra = res.caminho;
                                    setStatus(String.format(
                                        "Caminho %d→%d | dist=%.2f | %d vértices | %d ms",
                                        verticeA, dest, res.distanciaTotal,
                                        res.caminho.size(), res.tempoMs), COR_STATUS_OK);
                                } else {
                                    caminhoDijkstra.clear();
                                    setStatus("Sem caminho entre " + verticeA + " e " + dest, COR_STATUS_ERR);
                                }
                                verticeA = -1;
                                repaint();
                            }
                        }

                        case REMOVER -> {
                            if (clicado != -1) {
                                grafo.removerVertice(clicado);
                                if (verticeA == clicado) verticeA = -1;
                                caminhoDijkstra.clear();
                                setStatus("Vértice " + clicado + " removido", COR_STATUS_OK);
                                repaint();
                            }
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    arrastandoId = -1;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (arrastandoId != -1) {
                        Vertice v = grafo.getVertice(arrastandoId);
                        if (v != null) {
                            v.x = e.getX();
                            v.y = e.getY();
                            repaint();
                        }
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth(), h = getHeight();

            // Fundo com grade
            g.setColor(COR_FUNDO);
            g.fillRect(0, 0, w, h);
            desenharGrade(g, w, h);

            // Conjunto de ids no caminho para lookup O(1)
            Set<Integer> idsCaminho = new HashSet<>(caminhoDijkstra);

            // ── Arestas ──────────────────────────────────────────────────
            List<List<Aresta>> adj = (List<List<Aresta>>) grafo.getAd();
            Set<Long> desenhadas = new HashSet<>();

            for (int u = 0; u < adj.size(); u++) {
                Vertice vu = grafo.getVertice(u);
                if (vu == null) continue;
                for (Aresta a : adj.get(u)) {
                    Vertice vd = grafo.getVertice(a.dest);
                    if (vd == null) continue;

                    long chave = Math.min(u, a.dest) * 100000L + Math.max(u, a.dest);
                    if (desenhadas.contains(chave)) continue;
                    desenhadas.add(chave);

                    // Aresta no caminho?
                    boolean noCaminho = false;
                    for (int i = 0; i < caminhoDijkstra.size() - 1; i++) {
                        int ca = caminhoDijkstra.get(i), cb = caminhoDijkstra.get(i+1);
                        if ((ca == u && cb == a.dest) || (cb == u && ca == a.dest)) {
                            noCaminho = true; break;
                        }
                    }

                    if (noCaminho) {
                        g.setColor(COR_CAMINHO);
                        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    } else {
                        g.setColor(COR_ARESTA);
                        g.setStroke(new BasicStroke(1.6f));
                    }
                    g.drawLine((int)vu.x, (int)vu.y, (int)vd.x, (int)vd.y);
                }
            }

            // ── Vértices ─────────────────────────────────────────────────
            for (Vertice v : grafo.getVertices()) {
                if (v == null) continue;
                int cx = (int) v.x, cy = (int) v.y;
                int id = v.id_interno;

                boolean selecionado = (id == verticeA);
                boolean noCaminho   = idsCaminho.contains(id);
                boolean ehOrigem    = !caminhoDijkstra.isEmpty() && caminhoDijkstra.get(0) == id;
                boolean ehDestino   = !caminhoDijkstra.isEmpty() && caminhoDijkstra.get(caminhoDijkstra.size()-1) == id;

                // Sombra
                g.setColor(new Color(0, 0, 0, 80));
                g.fillOval(cx - RAIO + 2, cy - RAIO + 2, RAIO * 2, RAIO * 2);

                // Corpo
                Color corFill = noCaminho ? COR_CAMINHO.darker()
                              : selecionado ? COR_SELECIONADO.darker()
                              : COR_VERTICE;
                g.setColor(corFill);
                g.fillOval(cx - RAIO, cy - RAIO, RAIO * 2, RAIO * 2);

                // Borda
                Color corBorda = selecionado ? COR_SELECIONADO
                               : noCaminho   ? COR_CAMINHO
                               : COR_BORDA_V;
                g.setStroke(new BasicStroke(selecionado || noCaminho ? 2.5f : 1.5f));
                g.setColor(corBorda);
                g.drawOval(cx - RAIO, cy - RAIO, RAIO * 2, RAIO * 2);

                // Marcador de origem/destino
                if (ehOrigem || ehDestino) {
                    g.setColor(ehOrigem ? new Color(255, 220, 0) : new Color(0, 255, 160));
                    g.setStroke(new BasicStroke(2f));
                    g.drawOval(cx - RAIO - 5, cy - RAIO - 5, (RAIO+5) * 2, (RAIO+5) * 2);
                }

                // ID do vértice
                g.setFont(FONTE_ID);
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                String label = String.valueOf(id);
                int tx = cx - fm.stringWidth(label) / 2;
                int ty = cy + fm.getAscent() / 2 - 1;
                g.drawString(label, tx, ty);
            }

            // ── Legenda do vértice selecionado ────────────────────────────
            if (verticeA != -1 && modoAtual != Modo.ADICIONAR_VERTICE) {
                Vertice vs = grafo.getVertice(verticeA);
                if (vs != null) {
                    int cx = (int) vs.x, cy = (int) vs.y;
                    g.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g.setColor(COR_SELECIONADO);
                    g.drawString(modoAtual == Modo.DIJKSTRA ? "ORIGEM" : "A", cx + RAIO + 4, cy - 2);
                }
            }

            // ── Contadores no canto superior direito ──────────────────────
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.setColor(new Color(60, 80, 140));
            long nv = grafo.getVertices().stream().filter(Objects::nonNull).count();
            int  na = grafo.totalArestas();
            g.drawString("V: " + nv + "   E: " + na, w - 120, 22);
        }

        private void desenharGrade(Graphics2D g, int w, int h) {
            g.setStroke(new BasicStroke(1f));
            g.setColor(COR_GRADE);
            int passo = 40;
            for (int x = 0; x < w; x += passo) g.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += passo) g.drawLine(0, y, w, y);
        }
    }

    // ── Entry point ─────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GrafoVisual::new);
    }
}
