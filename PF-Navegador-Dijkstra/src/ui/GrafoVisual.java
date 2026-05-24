import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.RenderingHints;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * GrafoVisual — Visualizador de grafos com Dijkstra interativo.
 *
 * Funcionalidades:
 *   • Carrega grafo a partir de arquivo .poly
 *   • Zoom com scroll do mouse, pan com botão do meio ou Ctrl+arrasto
 *   • Modo Dijkstra: clique em dois vértices para ver o menor caminho
 *   • Auto-fit ao carregar
 *
 * Compilação:
 *   javac Vertice.java Aresta.java HeapMinima.java ResultadoDijkstra.java \
 *         Grafo.java Dijkstra.java GrafoVisual.java
 *   java GrafoVisual
 *   java GrafoVisual meugrafo.poly
 */
public class GrafoVisual extends JFrame {

    // ── Paleta ───────────────────────────────────────────────────────────────
    private static final Color COR_FUNDO        = new Color(13, 15, 24);
    private static final Color COR_GRADE        = new Color(22, 28, 46);
    private static final Color COR_ARESTA       = new Color(55, 85, 170, 140);
    private static final Color COR_ARESTA_CAMINHO = new Color(0, 210, 130);
    private static final Color COR_VERTICE_FILL = new Color(40, 90, 200);
    private static final Color COR_VERTICE_BORD = new Color(90, 145, 255);
    private static final Color COR_ORIGEM       = new Color(255, 200, 0);
    private static final Color COR_DESTINO      = new Color(0, 240, 150);
    private static final Color COR_CAMINHO_FILL = new Color(0, 160, 100);
    private static final Color COR_TEXTO        = new Color(195, 210, 240);
    private static final Color COR_OK           = new Color(0, 200, 120);
    private static final Color COR_ERR          = new Color(220, 65, 65);
    private static final Color COR_PAINEL       = new Color(18, 22, 38);
    private static final Color COR_BTN          = new Color(35, 55, 110);
    private static final Color COR_BTN_HOVER    = new Color(55, 85, 165);

    // ── Raio dos vértices (adaptativo ao zoom) ───────────────────────────────
    private static final double RAIO_BASE = 6.0;
    private static final double RAIO_MIN  = 2.5;
    private static final double RAIO_MAX  = 16.0;

    // ── Estado ───────────────────────────────────────────────────────────────
    private Grafo          grafo           = new Grafo();
    private int            origem          = -1;   // vértice de origem do Dijkstra
    private List<Integer>  caminho         = new ArrayList<>();
    private String         nomeArquivo     = "";

    // Transformação mundo → tela:  telaX = mundoX * escala + offX
    private double escala = 1.0;
    private double offX   = 0.0;
    private double offY   = 0.0;

    // Pan
    private int    panX0, panY0;
    private double offX0, offY0;
    private boolean panAtivo = false;

    // ── Componentes ──────────────────────────────────────────────────────────
    private CanvasPanel canvas;
    private JLabel      lblStatus;
    private JLabel      lblArquivo;
    private JLabel      lblInfo;      // exibe resultado do Dijkstra no painel

    // ════════════════════════════════════════════════════════════════════════
    public GrafoVisual(String polyInicial) {
        super("Grafo Visual");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        construirUI();
        setVisible(true);
        if (polyInicial != null) carregarArquivo(new File(polyInicial));
    }

    // ════════════════════════════════════════════════════════════════════════
    // UI
    // ════════════════════════════════════════════════════════════════════════
    private void construirUI() {
        setLayout(new BorderLayout());

        // ── Painel lateral ───────────────────────────────────────────────
        JPanel painel = new JPanel();
        painel.setPreferredSize(new Dimension(215, 0));
        painel.setBackground(COR_PAINEL);
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(32, 42, 72)));

        // Logo / título
        painel.add(Box.createVerticalStrut(22));
        JLabel titulo = new JLabel("GRAFO VISUAL");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 15));
        titulo.setForeground(new Color(100, 140, 255));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(titulo);
        painel.add(Box.createVerticalStrut(4));
        JLabel sub = new JLabel("Visualizador + Dijkstra");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 10));
        sub.setForeground(new Color(60, 80, 130));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(sub);

        painel.add(separador(24, 12));

        // ── Arquivo ──────────────────────────────────────────────────────
        painel.add(secao("ARQUIVO"));
        painel.add(Box.createVerticalStrut(10));

        JButton btnAbrir = btn("📂  Abrir arquivo .poly",
                new Color(28, 68, 48), new Color(42, 105, 72));
        btnAbrir.addActionListener(e -> abrirArquivo());
        painel.add(centrado(btnAbrir, 185, 40));
        painel.add(Box.createVerticalStrut(8));

        lblArquivo = new JLabel("Nenhum arquivo carregado");
        lblArquivo.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblArquivo.setForeground(new Color(70, 90, 140));
        lblArquivo.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(lblArquivo);

        painel.add(separador(22, 10));

        // ── Dijkstra ─────────────────────────────────────────────────────
        painel.add(secao("DIJKSTRA"));
        painel.add(Box.createVerticalStrut(10));

        JLabel instrucao = new JLabel(
            "<html><div style='text-align:center;line-height:1.7'>" +
            "1º clique — vértice de <b>origem</b><br>" +
            "2º clique — vértice de <b>destino</b>" +
            "</div></html>");
        instrucao.setFont(new Font("SansSerif", Font.PLAIN, 11));
        instrucao.setForeground(new Color(120, 145, 200));
        instrucao.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(instrucao);
        painel.add(Box.createVerticalStrut(10));

        JButton btnLimparCam = btn("✖  Limpar caminho",
                new Color(65, 25, 25), new Color(110, 35, 35));
        btnLimparCam.addActionListener(e -> {
            origem  = -1;
            caminho.clear();
            lblInfo.setText(" ");
            setStatus("Caminho limpo — clique em um vértice para iniciar", COR_OK);
            canvas.repaint();
        });
        painel.add(centrado(btnLimparCam, 185, 36));

        painel.add(separador(22, 10));

        // ── Resultado Dijkstra ────────────────────────────────────────────
        painel.add(secao("RESULTADO"));
        painel.add(Box.createVerticalStrut(8));

        lblInfo = new JLabel(" ");
        lblInfo.setFont(new Font("Monospaced", Font.PLAIN, 10));
        lblInfo.setForeground(COR_OK);
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblInfo.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        painel.add(lblInfo);

        painel.add(separador(22, 10));

        // ── Navegação ────────────────────────────────────────────────────
        painel.add(secao("NAVEGAÇÃO"));
        painel.add(Box.createVerticalStrut(8));

        JLabel nav = new JLabel(
            "<html><div style='text-align:center;line-height:1.8'>" +
            "🖱 Scroll — zoom<br>" +
            "🖱 Meio / Ctrl+drag — pan<br>" +
            "🔍 Duplo clique — ajustar tela" +
            "</div></html>");
        nav.setFont(new Font("SansSerif", Font.PLAIN, 10));
        nav.setForeground(new Color(65, 85, 135));
        nav.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(nav);
        painel.add(Box.createVerticalStrut(10));

        JButton btnFit = btn("🔍  Ajustar à tela",
                new Color(35, 50, 100), new Color(55, 80, 155));
        btnFit.addActionListener(e -> { ajustarView(); canvas.repaint(); });
        painel.add(centrado(btnFit, 185, 36));

        painel.add(Box.createVerticalGlue());

        // ── Canvas ───────────────────────────────────────────────────────
        canvas = new CanvasPanel();

        // ── Barra de status ───────────────────────────────────────────────
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(new Color(14, 18, 30));
        barra.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 40, 68)));
        barra.setPreferredSize(new Dimension(0, 30));
        lblStatus = new JLabel("Abra um arquivo .poly para começar");
        lblStatus.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblStatus.setForeground(COR_OK);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        barra.add(lblStatus, BorderLayout.CENTER);

        add(painel, BorderLayout.WEST);
        add(canvas,  BorderLayout.CENTER);
        add(barra,   BorderLayout.SOUTH);
    }

    // ── Helpers de UI ───────────────────────────────────────────────────────
    private JLabel secao(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        l.setForeground(new Color(60, 80, 135));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    /** Linha divisória com espaço acima e abaixo. */
    private Component separador(int antes, int depois) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(Box.createVerticalStrut(antes));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(30, 40, 68));
        sep.setMaximumSize(new Dimension(185, 1));
        p.add(sep);
        p.add(Box.createVerticalStrut(depois));
        return p;
    }

    private JButton btn(String rotulo, Color normal, Color hover) {
        JButton b = new JButton(rotulo);
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        b.setForeground(COR_TEXTO);
        b.setBackground(normal);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(normal); }
        });
        return b;
    }

    private Component centrado(JComponent c, int maxW, int maxH) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.setMaximumSize(new Dimension(maxW, maxH));
        return c;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Carregamento
    // ════════════════════════════════════════════════════════════════════════
    private void abrirArquivo() {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Selecionar arquivo .poly");
        fc.setFileFilter(new FileNameExtensionFilter("Grafos (*.poly)", "poly"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            carregarArquivo(fc.getSelectedFile());
    }

    private void carregarArquivo(File arquivo) {
        setStatus("Carregando " + arquivo.getName() + " …", COR_OK);
        lblArquivo.setText("Carregando…");

        SwingWorker<Grafo, Void> w = new SwingWorker<>() {
            @Override protected Grafo doInBackground() throws Exception {
                Grafo g = new Grafo();
                g.carregarDoArquivo(arquivo.getAbsolutePath());
                return g;
            }
            @Override protected void done() {
                try {
                    grafo   = get();
                    origem  = -1;
                    caminho.clear();
                    lblInfo.setText(" ");
                    nomeArquivo = arquivo.getName();

                    long nv = grafo.getVertices().stream().filter(v -> v != null).count();
                    int  na = grafo.totalArestas();

                    lblArquivo.setText("<html><center>" + nomeArquivo + "</center></html>");
                    setTitle("Grafo Visual — " + nomeArquivo);
                    setStatus(String.format("✔  %s  —  %,d vértices,  %,d arestas", nomeArquivo, nv, na), COR_OK);

                    ajustarView();
                    canvas.repaint();
                } catch (Exception ex) {
                    lblArquivo.setText("Erro ao carregar");
                    setStatus("Erro: " + ex.getMessage(), COR_ERR);
                }
            }
        };
        w.execute();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Transformações
    // ════════════════════════════════════════════════════════════════════════
    private int tx(double wx) { return (int) Math.round(wx * escala + offX); }
    private int ty(double wy) { return (int) Math.round(wy * escala + offY); }
    private double mx(int px) { return (px - offX) / escala; }
    private double my(int py) { return (py - offY) / escala; }

    private int raio() {
        return (int) Math.max(RAIO_MIN, Math.min(RAIO_MAX, RAIO_BASE * Math.sqrt(escala)));
    }

    /** Encontra o vértice dentro do raio de toque do ponto (px,py) na tela. */
    private int verticeEm(int px, int py) {
        int r = raio() + 5;
        for (Vertice v : grafo.getVertices()) {
            if (v == null) continue;
            int dx = tx(v.x) - px, dy = ty(v.y) - py;
            if (dx*dx + dy*dy <= r*r) return v.id_interno;
        }
        return -1;
    }

    private void ajustarView() {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Vertice v : grafo.getVertices()) {
            if (v == null) continue;
            if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x;
            if (v.y < minY) minY = v.y; if (v.y > maxY) maxY = v.y;
        }
        if (minX == Double.MAX_VALUE) { escala = 1; offX = offY = 0; return; }

        int cw = canvas.getWidth()  > 0 ? canvas.getWidth()  : 950;
        int ch = canvas.getHeight() > 0 ? canvas.getHeight() : 660;
        int m  = 55;
        double rx = maxX - minX, ry = maxY - minY;
        if (rx < 1) rx = 1; if (ry < 1) ry = 1;
        escala = Math.min((cw - 2.0*m) / rx, (ch - 2.0*m) / ry);
        offX   = m - minX * escala;
        offY   = m - minY * escala;
    }

    private void setStatus(String msg, Color cor) {
        lblStatus.setText(msg);
        lblStatus.setForeground(cor);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Canvas
    // ════════════════════════════════════════════════════════════════════════
    class CanvasPanel extends JPanel {

        CanvasPanel() {
            setBackground(COR_FUNDO);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            // Zoom centrado no cursor
            addMouseWheelListener(e -> {
                double f = e.getWheelRotation() < 0 ? 1.13 : (1.0 / 1.13);
                double mx = e.getX(), my = e.getY();
                offX = mx - (mx - offX) * f;
                offY = my - (my - offY) * f;
                escala *= f;
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    // Pan: botão do meio ou Ctrl+esquerdo
                    if (SwingUtilities.isMiddleMouseButton(e) ||
                       (SwingUtilities.isLeftMouseButton(e) && e.isControlDown())) {
                        panAtivo = true;
                        panX0 = e.getX(); panY0 = e.getY();
                        offX0 = offX;     offY0 = offY;
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        return;
                    }

                    // Duplo clique esquerdo → fit
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2
                            && !e.isControlDown()) {
                        ajustarView(); repaint(); return;
                    }

                    // Clique simples esquerdo → Dijkstra
                    if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown()) {
                        int v = verticeEm(e.getX(), e.getY());
                        if (v == -1) return;

                        if (origem == -1) {
                            // Seleciona origem
                            origem = v;
                            caminho.clear();
                            lblInfo.setText(" ");
                            setStatus("Origem: " + v + " — clique no vértice de destino", COR_OK);
                        } else {
                            // Calcula Dijkstra
                            int dest = v;
                            if (dest == origem) {
                                setStatus("Origem e destino iguais — escolha outro vértice", COR_ERR);
                                return;
                            }
                            setStatus("Calculando Dijkstra " + origem + " → " + dest + " …", COR_OK);
                            final int orig0 = origem;
                            origem = -1;

                            SwingWorker<ResultadoDijkstra, Void> dw = new SwingWorker<>() {
                                @Override protected ResultadoDijkstra doInBackground() {
                                    return new Dijkstra().calcular(grafo, orig0, dest);
                                }
                                @Override protected void done() {
                                    try {
                                        ResultadoDijkstra r = get();
                                        if (r.temCaminho()) {
                                            caminho = r.caminho;
                                            String info = String.format(
                                                "<html><center>dist: <b>%.2f</b><br>saltos: <b>%d</b><br>tempo: <b>%d ms</b></center></html>",
                                                r.distanciaTotal, r.caminho.size() - 1, r.tempoMs);
                                            lblInfo.setText(info);
                                            lblInfo.setForeground(COR_OK);
                                            setStatus(String.format(
                                                "✔  %d → %d | dist=%.2f | %d vértices | %d ms",
                                                orig0, dest, r.distanciaTotal,
                                                r.caminho.size(), r.tempoMs), COR_OK);
                                        } else {
                                            caminho.clear();
                                            lblInfo.setText("<html><center><b>Sem caminho</b></center></html>");
                                            lblInfo.setForeground(COR_ERR);
                                            setStatus("Sem caminho entre " + orig0 + " e " + dest, COR_ERR);
                                        }
                                    } catch (Exception ex) {
                                        setStatus("Erro: " + ex.getMessage(), COR_ERR);
                                    }
                                    repaint();
                                }
                            };
                            dw.execute();
                        }
                        repaint();
                    }
                }

                @Override public void mouseReleased(MouseEvent e) {
                    if (panAtivo) {
                        panAtivo = false;
                        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (panAtivo) {
                        offX = offX0 + (e.getX() - panX0);
                        offY = offY0 + (e.getY() - panY0);
                        repaint();
                    }
                }
            });
        }

        // ── Renderização ─────────────────────────────────────────────────
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth(), h = getHeight();
            g.setColor(COR_FUNDO);
            g.fillRect(0, 0, w, h);
            desenharGrade(g, w, h);

            if (grafo.totalVertices() == 0) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g.setColor(new Color(50, 65, 110));
                String msg = "Abra um arquivo .poly para visualizar o grafo";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                return;
            }

            int raio = raio();
            boolean mostrarIds = escala > 0.8 && raio >= 8;

            // Lookup rápido O(1) para caminho
            Set<Integer> idsCam = new HashSet<>(caminho);
            Set<Long>    arestaCam = new HashSet<>();
            for (int i = 0; i < caminho.size() - 1; i++) {
                int a = caminho.get(i), b = caminho.get(i + 1);
                arestaCam.add((long) a * 200_000L + b);
                arestaCam.add((long) b * 200_000L + a);
            }

            // ── Arestas ──────────────────────────────────────────────────
            List<List<Aresta>> adj = (List<List<Aresta>>) grafo.getAd();
            Set<Long> desenhadas = new HashSet<>();

            float espessuraAresta = Math.max(0.4f, (float)(escala * 0.7));

            for (int u = 0; u < adj.size(); u++) {
                Vertice vu = grafo.getVertice(u);
                if (vu == null) continue;
                int ux = tx(vu.x), uy = ty(vu.y);

                for (Aresta a : adj.get(u)) {
                    Vertice vd = grafo.getVertice(a.dest);
                    if (vd == null) continue;

                    boolean noCam = arestaCam.contains((long) u * 200_000L + a.dest);

                    long chave = Math.min(u, a.dest) * 200_000L + Math.max(u, a.dest);
                    if (!noCam && desenhadas.contains(chave)) continue;
                    if (!noCam) desenhadas.add(chave);

                    int dx = tx(vd.x), dy = ty(vd.y);

                    if (noCam) {
                        g.setColor(COR_ARESTA_CAMINHO);
                        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    } else {
                        int alpha = Math.max(50, Math.min(150, (int)(140 / (escala + 0.1))));
                        g.setColor(new Color(55, 85, 170, alpha));
                        g.setStroke(new BasicStroke(espessuraAresta));
                    }
                    g.drawLine(ux, uy, dx, dy);
                }
            }

            // ── Vértices ─────────────────────────────────────────────────
            for (Vertice v : grafo.getVertices()) {
                if (v == null) continue;
                int cx = tx(v.x), cy = ty(v.y);
                int id = v.id_interno;

                // Culling
                if (cx < -raio*3 || cy < -raio*3 || cx > w+raio*3 || cy > h+raio*3) continue;

                boolean ehOrigem = (id == origem);
                boolean noCam   = idsCam.contains(id);
                boolean ehPrim  = !caminho.isEmpty() && caminho.get(0) == id;
                boolean ehUlt   = !caminho.isEmpty() && caminho.get(caminho.size()-1) == id;

                // Anel externo para origem selecionada / pontos do caminho extremos
                if (ehOrigem || ehPrim || ehUlt) {
                    Color corAnel = ehOrigem ? COR_ORIGEM : (ehPrim ? COR_ORIGEM : COR_DESTINO);
                    g.setColor(corAnel);
                    g.setStroke(new BasicStroke(2.2f));
                    int r2 = raio + 5;
                    g.drawOval(cx - r2, cy - r2, r2*2, r2*2);
                }

                // Sombra
                if (raio >= 5) {
                    g.setColor(new Color(0, 0, 0, 60));
                    g.fillOval(cx - raio + 2, cy - raio + 2, raio*2, raio*2);
                }

                // Corpo
                Color fill = noCam    ? COR_CAMINHO_FILL
                           : ehOrigem ? COR_ORIGEM.darker()
                           : COR_VERTICE_FILL;
                g.setColor(fill);
                g.fillOval(cx - raio, cy - raio, raio*2, raio*2);

                // Borda
                Color bord = noCam    ? COR_ARESTA_CAMINHO
                           : ehOrigem ? COR_ORIGEM
                           : COR_VERTICE_BORD;
                g.setStroke(new BasicStroke(noCam || ehOrigem ? 1.8f : 1.0f));
                g.setColor(bord);
                g.drawOval(cx - raio, cy - raio, raio*2, raio*2);

                // ID
                if (mostrarIds) {
                    Font fid = new Font("Monospaced", Font.BOLD, Math.min(10, raio));
                    g.setFont(fid);
                    g.setColor(Color.WHITE);
                    FontMetrics fm = g.getFontMetrics();
                    String lbl = String.valueOf(id);
                    g.drawString(lbl, cx - fm.stringWidth(lbl)/2, cy + fm.getAscent()/2 - 1);
                }
            }

            // ── Label "ORIGEM" sobre o vértice selecionado ────────────────
            if (origem != -1) {
                Vertice vs = grafo.getVertice(origem);
                if (vs != null) {
                    g.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g.setColor(COR_ORIGEM);
                    g.drawString("ORIGEM", tx(vs.x) + raio + 5, ty(vs.y) - 3);
                }
            }

            // ── HUD ───────────────────────────────────────────────────────
            long nv = grafo.getVertices().stream().filter(x -> x != null).count();
            int  na = grafo.totalArestas();
            String hud = String.format("V: %,d   E: %,d   zoom: %.2f×", nv, na, escala);
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            int hw = fm.stringWidth(hud);
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(w - hw - 24, 8, hw + 18, 20, 6, 6);
            g.setColor(new Color(65, 88, 155));
            g.drawString(hud, w - hw - 15, 22);
        }

        // ── Grade adaptativa ─────────────────────────────────────────────
        private void desenharGrade(Graphics2D g, int w, int h) {
            double espacoMundo = 60.0 / escala;
            double pot = Math.pow(10, Math.floor(Math.log10(Math.max(espacoMundo, 1e-9))));
            double mult = espacoMundo / pot;
            double passo = mult < 2 ? pot : mult < 5 ? 2*pot : 5*pot;

            double sx = Math.floor(mx(0) / passo) * passo;
            double sy = Math.floor(my(0) / passo) * passo;

            g.setStroke(new BasicStroke(1f));
            g.setColor(COR_GRADE);
            for (double wx = sx; tx(wx) < w+1; wx += passo) { int s = tx(wx); g.drawLine(s, 0, s, h); }
            for (double wy = sy; ty(wy) < h+1; wy += passo) { int s = ty(wy); g.drawLine(0, s, w, s); }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        String poly = args.length >= 1 ? args[0] : null;
        SwingUtilities.invokeLater(() -> new GrafoVisual(poly));
    }
}
