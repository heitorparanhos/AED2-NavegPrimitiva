package ui;

import core.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.RenderingHints;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Locale;

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

    // ── Modo de interacao (RF05) ─────────────────────────────────────────────
    private enum Modo { NAVEGAR, ADICIONAR_VERTICE, ADICIONAR_ARESTA, REMOVER_VERTICE, REMOVER_ARESTA }
    private Modo modo = Modo.NAVEGAR;

    // Primeiro vertice selecionado ao criar uma aresta (-1 = nenhum ainda)
    private int arestaOrigem = -1;
    private boolean criarMaoUnica = false; // RF06: mao unica ou dupla

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

        // ── Modo de interacao (RF05) ─────────────────────────────────────
        painel.add(Box.createVerticalStrut(14));
        painel.add(secao("MODO"));
        painel.add(Box.createVerticalStrut(10));

        JButton btnModo = btn("Navegar",
                new Color(35, 55, 110), new Color(55, 85, 165));
        btnModo.addActionListener(e -> {
            // Cicla: NAVEGAR -> ADICIONAR_VERTICE -> ADICIONAR_ARESTA -> NAVEGAR
            if (modo == Modo.NAVEGAR) {
                modo = Modo.ADICIONAR_VERTICE;
                btnModo.setText("Adicionar vertice");
                setStatus("Modo edicao: clique no canvas para criar um vertice", COR_OK);
            } else if (modo == Modo.ADICIONAR_VERTICE) {
                modo = Modo.ADICIONAR_ARESTA;
                btnModo.setText("Adicionar aresta");
                setStatus("Modo aresta: clique em dois vertices para liga-los", COR_OK);
            } else if (modo == Modo.ADICIONAR_ARESTA) {
                modo = Modo.REMOVER_VERTICE;
                btnModo.setText("Remover vertice");
                setStatus("Modo remover: clique em um vertice para apaga-lo", COR_OK);
            } else if (modo == Modo.REMOVER_VERTICE) {
                modo = Modo.REMOVER_ARESTA;
                btnModo.setText("Remover aresta");
                setStatus("Modo remover aresta: clique sobre uma aresta para apaga-la", COR_OK);
            } else {
                modo = Modo.NAVEGAR;
                btnModo.setText("Navegar");
                setStatus("Modo navegar: clique em dois vertices para o caminho", COR_OK);
            }
            origem       = -1;
            arestaOrigem = -1;
            caminho.clear();
            canvas.repaint();
        });
        painel.add(centrado(btnModo, 185, 40));
        painel.add(Box.createVerticalStrut(6));

        JCheckBox chkMaoUnica = new JCheckBox("Mao unica");
        chkMaoUnica.setFont(new Font("SansSerif", Font.PLAIN, 10));
        chkMaoUnica.setForeground(COR_TEXTO);
        chkMaoUnica.setBackground(COR_PAINEL);
        chkMaoUnica.setAlignmentX(Component.CENTER_ALIGNMENT);
        chkMaoUnica.addActionListener(e -> criarMaoUnica = chkMaoUnica.isSelected());
        painel.add(chkMaoUnica);
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
        painel.add(Box.createVerticalStrut(8));

        JButton btnCopiar = btn("Copiar imagem",
                new Color(60, 35, 90), new Color(90, 55, 140));
        btnCopiar.addActionListener(e -> copiarImagemParaClipboard());
        painel.add(centrado(btnCopiar, 185, 36));

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
        fc.setDialogTitle("Selecionar arquivo de Grafo");
        fc.setFileFilter(new FileNameExtensionFilter("Grafos (*.poly, *.txt)", "poly", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File arquivo = fc.getSelectedFile();
            if (arquivo.getName().toLowerCase().endsWith(".txt")) {
                if (carregarGrafoTXT(arquivo)) {
                    nomeArquivo = arquivo.getName();
                    lblArquivo.setText("<html><center>" + nomeArquivo + "</center></html>");
                    setTitle("Grafo Visual — " + nomeArquivo);
                }
            } else {
                carregarArquivo(arquivo);
            }
        }
    }

    // ── Salvar ───────────────────────────────────────────────────────────────
    private void abrirSalvarArquivo() {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Salvar grafo como .poly");
        fc.setFileFilter(new FileNameExtensionFilter("Grafos (*.poly)", "poly"));
        fc.setSelectedFile(new File(nomeArquivo.isEmpty() ? "grafo.poly" : nomeArquivo));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File arquivo = fc.getSelectedFile();
        if (!arquivo.getName().toLowerCase().endsWith(".poly"))
            arquivo = new File(arquivo.getAbsolutePath() + ".poly");

        final File destino = arquivo;
        setStatus("Salvando " + destino.getName() + " …", COR_OK);

        SwingWorker<Boolean, Void> sw = new SwingWorker<>() {
            @Override protected Boolean doInBackground() throws Exception {
                return salvarGrafoEmArquivo(destino);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        nomeArquivo = destino.getName();
                        lblArquivo.setText("<html><center>" + nomeArquivo + "</center></html>");
                        setTitle("Grafo Visual — " + nomeArquivo);
                        setStatus("✔  Grafo salvo em " + destino.getName(), COR_OK);
                    }
                } catch (Exception ex) {
                    setStatus("Erro ao salvar: " + ex.getMessage(), COR_ERR);
                }
            }
        };
        sw.execute();
    }

    private void carregarArquivo(File arquivo) {
        setStatus("Carregando " + arquivo.getName() + " …", COR_OK);
        lblArquivo.setText("Carregando…");

        SwingWorker<Boolean, Void> w = new SwingWorker<>() {
            @Override protected Boolean doInBackground() throws Exception {
                return carregarGrafoDeArquivo(arquivo);
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        nomeArquivo = arquivo.getName();
                        lblArquivo.setText("<html><center>" + nomeArquivo + "</center></html>");
                        setTitle("Grafo Visual — " + nomeArquivo);
                    } else {
                        lblArquivo.setText("Erro ao carregar");
                    }
                } catch (Exception ex) {
                    lblArquivo.setText("Erro ao carregar");
                    setStatus("Erro: " + ex.getMessage(), COR_ERR);
                }
            }
        };
        w.execute();
    }

    /**
     * Lê arquivo .poly no formato customizado:
     *   # linhas de comentário (ignoradas)
     *   // linhas de comentário (ignoradas)
     *   *VERTICES
     *   id x y          (uma linha por vértice)
     *   *ARESTAS
     *   orig dest        (uma linha por aresta — bidirecional por padrão)
     *
     * Estratégia de inserção de vértices:
     *   Lemos TODOS os vértices em um Map<Integer,double[]> primeiro,
     *   depois os inserimos em ordem de id crescente. Isso garante que
     *   o id_interno atribuído por adicionarVertice() coincide com o id
     *   do arquivo sem depender de "preencher slots" com nulls.
     */
    private boolean carregarGrafoDeArquivo(File arquivo) {
        // Detecta formato: cabecalho numerico = formato professor; "*" = formato customizado
        try {
            String prim = lerPrimeiraLinhaNaoVaziaDeArquivo(arquivo);
            if (prim != null && !prim.startsWith("*")) {
                return carregarFormatoOriginal(arquivo);
            }
        } catch (IOException ex) {
            setStatus("Erro ao detectar formato: " + ex.getMessage(), COR_ERR);
            return false;
        }

        // Formato customizado (*VERTICES / *ARESTAS)
        TreeMap<Integer, double[]> mapaVertices = new TreeMap<>();
        ArrayList<int[]>           listaArestas = new ArrayList<>();
        String secao = null;

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                linha = linha.trim();

                // Ignora linhas em branco e comentários
                if (linha.isEmpty() || linha.startsWith("#") || linha.startsWith("//"))
                    continue;

                // Marcadores de seção
                if (linha.equalsIgnoreCase("*VERTICES")) { secao = "V"; continue; }
                if (linha.equalsIgnoreCase("*ARESTAS"))  { secao = "A"; continue; }
                if (secao == null) continue;

                String[] p = linha.split("\\s+");

                if (secao.equals("V")) {
                    if (p.length < 3) {
                        setStatus("Linha de vértice inválida: " + linha, COR_ERR);
                        return false;
                    }
                    int    id = Integer.parseInt(p[0]);
                    double x  = Double.parseDouble(p[1].replace(',', '.'));
                    double y  = Double.parseDouble(p[2].replace(',', '.'));
                    mapaVertices.put(id, new double[]{x, y});

                } else { // "A"
                    if (p.length < 2) {
                        setStatus("Linha de aresta inválida: " + linha, COR_ERR);
                        return false;
                    }
                    int orig = Integer.parseInt(p[0]);
                    int dest = Integer.parseInt(p[1]);
                    listaArestas.add(new int[]{orig, dest});
                }
            }
        } catch (NumberFormatException ex) {
            setStatus("Número inválido no arquivo: " + ex.getMessage(), COR_ERR);
            return false;
        } catch (IOException ex) {
            setStatus("Erro de leitura: " + ex.getMessage(), COR_ERR);
            return false;
        }

        // Constrói o grafo a partir dos dados coletados
        Grafo g = new Grafo();

        // Mapeia id do arquivo → id interno do grafo
        HashMap<Integer, Integer> idMap = new HashMap<>();
        for (Map.Entry<Integer, double[]> e : mapaVertices.entrySet()) {
            int    idArquivo = e.getKey();
            double x         = e.getValue()[0];
            double y         = e.getValue()[1];
            int    idInterno = g.adicionarVertice(x, y);
            idMap.put(idArquivo, idInterno);
        }

        for (int[] ar : listaArestas) {
            Integer u = idMap.get(ar[0]);
            Integer v = idMap.get(ar[1]);
            if (u == null || v == null) {
                setStatus("Aresta com id inexistente: " + ar[0] + " -> " + ar[1], COR_ERR);
                return false;
            }
            g.adicionarAresta(u, v, true);
        }

        // Atualiza o estado da aplicação
        grafo = g;
        origem = -1;
        caminho.clear();
        lblInfo.setText(" ");

        long nv = grafo.getVertices().stream().filter(vt -> vt != null).count();
        int  na = grafo.totalArestas();
        setStatus(String.format("✔  %s  —  %,d vértices,  %,d arestas",
                arquivo.getName(), nv, na), COR_OK);
        ajustarView();
        canvas.repaint();
        return true;
    }

    /**
     * Grava o grafo no formato .poly customizado.
     * Arestas bidirecionais são salvas uma única vez (chave min_max).
     */
    private boolean salvarGrafoEmArquivo(File arquivo) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo))) {

            long nv = grafo.getVertices().stream().filter(v -> v != null).count();

            bw.write("# Grafo exportado por GrafoVisual");        bw.newLine();
            bw.write("# Vertices: " + nv);                        bw.newLine();
            bw.write("# Arestas:  " + grafo.totalArestas());      bw.newLine();
            bw.newLine();

            // ── Vértices ─────────────────────────────────────────────────────
            bw.write("*VERTICES"); bw.newLine();
            for (Vertice v : grafo.getVertices()) {
                if (v == null) continue;
                bw.write(String.format(Locale.US, "%d %.6f %.6f",
                        v.id_interno, v.x, v.y));
                bw.newLine();
            }
            bw.newLine();

            // ── Arestas — salva cada par lógico apenas uma vez ────────────────
            bw.write("*ARESTAS"); bw.newLine();
            Set<String> jaEscritas = new HashSet<>();
            List<List<Aresta>> adj = (List<List<Aresta>>) grafo.getAd();

            for (int u = 0; u < adj.size(); u++) {
                if (grafo.getVertice(u) == null) continue;
                for (Aresta a : adj.get(u)) {
                    if (grafo.getVertice(a.dest) == null) continue;

                    // Chave canônica: menor id sempre primeiro
                    String chave = Math.min(u, a.dest) + "_" + Math.max(u, a.dest);
                    if (jaEscritas.contains(chave)) continue;
                    jaEscritas.add(chave);

                    bw.write(u + " " + a.dest);
                    bw.newLine();
                }
            }

            bw.flush();
            return true;

        } catch (IOException ex) {
            setStatus("Erro ao salvar: " + ex.getMessage(), COR_ERR);
            return false;
        }
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

    /**
     * Encontra a aresta cujo segmento passa mais perto do ponto (px,py) na tela.
     * Retorna um int[]{orig, dest} da aresta clicada, ou null se nenhuma estiver
     * dentro da tolerancia de toque.
     *
     * Usa distancia ponto-segmento: para cada aresta, calcula a menor distancia
     * entre o clique e o segmento que liga os dois vertices na tela.
     */
    private int[] arestaEm(int px, int py) {
        double tolerancia = 6.0;          // raio de toque em pixels
        double melhorDist = tolerancia;
        int[]  melhor     = null;

        List<List<Aresta>> adj = grafo.getAd();
        for (int u = 0; u < adj.size(); u++) {
            Vertice vu = grafo.getVertice(u);
            if (vu == null) continue;
            int ux = tx(vu.x), uy = ty(vu.y);

            for (Aresta a : adj.get(u)) {
                Vertice vd = grafo.getVertice(a.dest);
                if (vd == null) continue;
                int vx = tx(vd.x), vy = ty(vd.y);

                double d = distPontoSegmento(px, py, ux, uy, vx, vy);
                if (d < melhorDist) {
                    melhorDist = d;
                    melhor     = new int[]{ u, a.dest };
                }
            }
        }
        return melhor;
    }

    /**
     * Distancia do ponto (px,py) ao segmento de reta (ax,ay)-(bx,by).
     * Projeta o ponto sobre a reta do segmento e limita a projecao ao
     * intervalo [0,1] para nao "vazar" para fora das extremidades.
     */
    private double distPontoSegmento(int px, int py,
                                     int ax, int ay, int bx, int by) {
        double dx = bx - ax, dy = by - ay;
        double comprimento2 = dx*dx + dy*dy;
        if (comprimento2 == 0) {
            // Segmento degenerado: os dois extremos coincidem
            double ex = px - ax, ey = py - ay;
            return Math.sqrt(ex*ex + ey*ey);
        }
        // t = posicao da projecao ao longo do segmento (0 = a, 1 = b)
        double t = ((px - ax)*dx + (py - ay)*dy) / comprimento2;
        t = Math.max(0, Math.min(1, t));
        double projX = ax + t*dx, projY = ay + t*dy;
        double ex = px - projX, ey = py - projY;
        return Math.sqrt(ex*ex + ey*ey);
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

                        // ── Modo edicao: criar vertice (RF05) ──────────────
                        if (modo == Modo.ADICIONAR_VERTICE) {
                            double wx = mx(e.getX());
                            double wy = my(e.getY());
                            int novoId = grafo.adicionarVertice(wx, wy);
                            setStatus("Vertice " + novoId + " criado em ("
                                    + String.format("%.1f, %.1f", wx, wy) + ")", COR_OK);
                            repaint();
                            return;
                        }

                        // ── Modo edicao: criar aresta (RF05) ───────────────
                        if (modo == Modo.ADICIONAR_ARESTA) {
                            int v = verticeEm(e.getX(), e.getY());
                            if (v == -1) {
                                setStatus("Clique em cima de um vertice para criar a aresta", COR_ERR);
                                return;
                            }
                            if (arestaOrigem == -1) {
                                // Primeiro vertice da aresta
                                arestaOrigem = v;
                                setStatus("Aresta: vertice " + v
                                        + " selecionado - clique no segundo vertice", COR_OK);
                            } else if (v == arestaOrigem) {
                                setStatus("Selecione um vertice diferente do primeiro", COR_ERR);
                            } else {
                                // RF06: mao unica ou dupla conforme checkbox
                                grafo.adicionarAresta(arestaOrigem, v, !criarMaoUnica);
                                setStatus("Aresta " + (criarMaoUnica ? "mao unica" : "mao dupla")
                                        + " criada entre " + arestaOrigem + " e " + v, COR_OK);
                                arestaOrigem = -1;
                            }
                            repaint();
                            return;
                        }

                        // ── Modo edicao: remover vertice (RF05) ────────────
                        if (modo == Modo.REMOVER_VERTICE) {
                            int v = verticeEm(e.getX(), e.getY());
                            if (v == -1) {
                                setStatus("Clique em cima de um vertice para remove-lo", COR_ERR);
                                return;
                            }
                            grafo.removerVertice(v);
                            // Limpa selecoes que possam apontar para o vertice removido
                            if (origem == v)       origem = -1;
                            if (arestaOrigem == v) arestaOrigem = -1;
                            caminho.clear();
                            setStatus("Vertice " + v + " removido (e suas arestas)", COR_OK);
                            repaint();
                            return;
                        }

                        // ── Modo edicao: remover aresta (RF05) ─────────────
                        if (modo == Modo.REMOVER_ARESTA) {
                            int[] ar = arestaEm(e.getX(), e.getY());
                            if (ar == null) {
                                setStatus("Clique mais perto de uma aresta para remove-la", COR_ERR);
                                return;
                            }
                            // Remove os dois sentidos (cobre mao unica e mao dupla)
                            grafo.removerAresta(ar[0], ar[1]);
                            grafo.removerAresta(ar[1], ar[0]);
                            caminho.clear();
                            setStatus("Aresta entre " + ar[0] + " e " + ar[1]
                                    + " removida", COR_OK);
                            repaint();
                            return;
                        }

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
                                            // RF07: custo total, saltos, nos explorados e tempo
                                            String info = String.format(
                                                    "<html><div style='font-family:monospace;line-height:1.9'>" +
                                                            "Custo total:&nbsp;&nbsp;<b>%.2f</b><br>" +
                                                            "Saltos:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>%d</b><br>" +
                                                            "Nos explorados: <b>%d</b><br>" +
                                                            "Tempo:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>%d ms</b>" +
                                                            "</div></html>",
                                                    r.distanciaTotal, r.caminho.size() - 1,
                                                    r.nosExplorados, r.tempoMs);
                                            lblInfo.setText(info);
                                            lblInfo.setForeground(COR_OK);
                                            setStatus(String.format(
                                                    "OK  %d -> %d | custo=%.2f | %d nos explorados | %d ms",
                                                    orig0, dest, r.distanciaTotal,
                                                    r.nosExplorados, r.tempoMs), COR_OK);
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
                    final int uFinal = u;
                    boolean ehMaoUnica = !grafo.getAdjacencia(a.dest)
                            .stream().anyMatch(ar -> ar.dest == uFinal);
                    desenharAresta(g, ux, uy, dx, dy, ehMaoUnica && !noCam);

                    // RF02: rotulo de peso no meio da aresta (so em zoom alto)
                    if (escala > 1.5) {
                        String peso = String.format("%.0f", a.dist);
                        int mx2 = (ux + dx) / 2, my2 = (uy + dy) / 2;
                        g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                        FontMetrics fmA = g.getFontMetrics();
                        int pw = fmA.stringWidth(peso);
                        g.setColor(new Color(0, 0, 0, 120));
                        g.fillRoundRect(mx2 - pw/2 - 2, my2 - 7, pw + 4, 11, 3, 3);
                        g.setColor(new Color(180, 200, 255));
                        g.drawString(peso, mx2 - pw/2, my2 + 2);
                    }
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

                // Destaque do 1o vertice ao criar uma aresta (RF05)
                if (id == arestaOrigem) {
                    g.setColor(COR_ORIGEM);
                    g.setStroke(new BasicStroke(2.4f));
                    int r3 = raio + 5;
                    g.drawOval(cx - r3, cy - r3, r3*2, r3*2);
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
    // ── RF06: desenha aresta com seta se mao unica ───────────────────────────
    private void desenharAresta(Graphics2D g, int ux, int uy, int vx, int vy, boolean ehMaoUnica) {
        g.drawLine(ux, uy, vx, vy);
        if (!ehMaoUnica) return;

        double ddx = vx - ux, ddy = vy - uy;
        double len = Math.sqrt(ddx*ddx + ddy*ddy);
        if (len < 10) return;

        // Ponta da seta em 60% do segmento
        double mx = ux + ddx * 0.6, my = uy + ddy * 0.6;
        double ux2 = ddx/len, uy2 = ddy/len;
        double px = -uy2, py = ux2;
        int tam = 7;

        int x1 = (int)(mx - ux2*tam + px*tam*0.5);
        int y1 = (int)(my - uy2*tam + py*tam*0.5);
        int x2 = (int)(mx - ux2*tam - px*tam*0.5);
        int y2 = (int)(my - uy2*tam - py*tam*0.5);

        g.fillPolygon(new int[]{(int)mx, x1, x2},
                      new int[]{(int)my, y1, y2}, 3);
    }

    // ── Parser .txt: lista de arestas com layout circular ────────────────────
    private boolean carregarGrafoTXT(File arquivo) {
        setStatus("Processando arquivo TXT...", COR_OK);
        Grafo g = new Grafo();
        List<int[]> arestasLidas = new ArrayList<>();
        Set<Integer> verticesUnicos = new TreeSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith("#") || linha.startsWith("//")) continue;
                String[] p = linha.split("\\s+");
                if (p.length >= 2) {
                    int u = Integer.parseInt(p[0]);
                    int v = Integer.parseInt(p[1]);
                    verticesUnicos.add(u); verticesUnicos.add(v);
                    arestasLidas.add(new int[]{u, v});
                }
            }
        } catch (Exception ex) {
            setStatus("Erro na leitura do TXT: " + ex.getMessage(), COR_ERR);
            return false;
        }
        if (verticesUnicos.isEmpty()) {
            setStatus("O arquivo TXT nao contem arestas validas.", COR_ERR);
            return false;
        }
        int n = verticesUnicos.size();
        double raioD = Math.max(80, n * 12);
        double passo = (2 * Math.PI) / n;
        int idx = 0;
        HashMap<Integer, Integer> mapaIds = new HashMap<>();
        for (int idTxt : verticesUnicos) {
            double x = raioD * Math.cos(idx * passo);
            double y = raioD * Math.sin(idx * passo);
            mapaIds.put(idTxt, g.adicionarVertice(x, y));
            idx++;
        }
        for (int[] ar : arestasLidas) {
            g.adicionarAresta(mapaIds.get(ar[0]), mapaIds.get(ar[1]), true);
        }
        grafo = g; origem = -1; caminho.clear(); lblInfo.setText(" ");
        long nv = grafo.getVertices().stream().filter(vt -> vt != null).count();
        setStatus(String.format("\u2714  %s  \u2014  %,d v\u00e9rtices,  %,d arestas",
                arquivo.getName(), nv, grafo.totalArestas()), COR_OK);
        ajustarView(); canvas.repaint();
        return true;
    }

    // ── Detecta formato pela primeira linha nao vazia ─────────────────────────
    private String lerPrimeiraLinhaNaoVaziaDeArquivo(File arquivo) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                linha = linha.trim();
                if (!linha.isEmpty() && !linha.startsWith("#") && !linha.startsWith("//"))
                    return linha;
            }
        }
        return null;
    }

    // ── Parser formato original do professor (cabecalho numerico) ─────────────
    private boolean carregarFormatoOriginal(File arquivo) {
        try {
            Grafo g = new Grafo();
            g.carregarDoArquivo(arquivo.getAbsolutePath());
            grafo = g; origem = -1; caminho.clear(); lblInfo.setText(" ");
            long nv = grafo.getVertices().stream().filter(vt -> vt != null).count();
            setStatus(String.format("\u2714  %s  \u2014  %,d v\u00e9rtices,  %,d arestas",
                    arquivo.getName(), nv, grafo.totalArestas()), COR_OK);
            ajustarView(); canvas.repaint();
            return true;
        } catch (IOException ex) {
            setStatus("Erro ao carregar: " + ex.getMessage(), COR_ERR);
            return false;
        }
    }

    // ── RF08: copia imagem do canvas para area de transferencia ──────────────
    private void copiarImagemParaClipboard() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                canvas.getWidth(), canvas.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        canvas.paint(img.getGraphics());
        java.awt.datatransfer.Transferable t = new java.awt.datatransfer.Transferable() {
            public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                return new java.awt.datatransfer.DataFlavor[]{
                    java.awt.datatransfer.DataFlavor.imageFlavor };
            }
            public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) {
                return java.awt.datatransfer.DataFlavor.imageFlavor.equals(f);
            }
            public Object getTransferData(java.awt.datatransfer.DataFlavor f) {
                return img;
            }
        };
        java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard().setContents(t, null);
        setStatus("Imagem copiada para a area de transferencia", COR_OK);
    }

    public static void main(String[] args) {
        String poly = args.length >= 1 ? args[0] : null;
        SwingUtilities.invokeLater(() -> new GrafoVisual(poly));
    }
}