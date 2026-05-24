/**
 * Suite de testes do módulo core (HeapMinima, Grafo, Dijkstra).
 *
 * Cobre:
 *   T1  — contador de arestas com mistura de mão única e mão dupla
 *   T2  — contador correto após removerVertice em grafo misto
 *   T3  — verticeMaisProximo em grafo com posições conhecidas
 *   T4  — verticeMaisProximo em grafo vazio retorna -1
 *   T5  — verticeMaisProximo pula vértice removido
 *   T6  — regressão: campus .poly (10000V / 11526E / Dijkstra 1222→9997)
 *
 * Uso:
 *   javac Vertice.java Aresta.java HeapMinima.java ResultadoDijkstra.java \
 *         Grafo.java Dijkstra.java TesteCore.java
 *   java TesteCore <caminho_do_arquivo.poly>
 */
public class TesteCore {

    private static int passou = 0;
    private static int falhou = 0;

    public static void main(String[] args) throws Exception {
        String poly = args.length >= 1 ? args[0] : "../../data/ArquivosPoly";

        System.out.println("══════════════════════════════════════════");
        System.out.println(" TesteCore — módulo core (Grafo/Dijkstra) ");
        System.out.println("══════════════════════════════════════════\n");

        testeContadorArestas();
        testeContadorApostosRemocao();
        testeVerticeMaisProximo();
        testeVerticeMaisProximoGrafoVazio();
        testeVerticeMaisProximoComRemovido();
        testeRegressaoCampus(poly);

        System.out.println("\n══════════════════════════════════════════");
        System.out.printf(" Resultado: %d/%d passaram%n", passou, passou + falhou);
        System.out.println("══════════════════════════════════════════");

        if (falhou > 0) System.exit(1);
    }

    // ─── T1: contador mão única + mão dupla ──────────────────────────────────
    static void testeContadorArestas() {
        Grafo g = new Grafo();
        for (int i = 0; i < 5; i++) g.adicionarVertice(i * 10, 0);

        g.adicionarAresta(0, 1, false);  // mão única  → 1 lógica
        g.adicionarAresta(1, 2, false);  // mão única  → 1 lógica
        g.adicionarAresta(2, 3, true);   // mão dupla  → 1 lógica
        // vértice 4 isolado

        int esperado = 3;
        int obtido   = g.totalArestas();
        ok("T1 totalArestas() misto = " + esperado, obtido == esperado,
           "esperado=" + esperado + " obtido=" + obtido);
    }

    // ─── T2: contador após removerVertice de vértice isolado ─────────────────
    static void testeContadorApostosRemocao() {
        Grafo g = new Grafo();
        for (int i = 0; i < 5; i++) g.adicionarVertice(i * 10, 0);

        g.adicionarAresta(0, 1, false);
        g.adicionarAresta(1, 2, false);
        g.adicionarAresta(2, 3, true);

        g.removerVertice(4);   // isolado — não deve alterar o contador

        int esperado = 3;
        int obtido   = g.totalArestas();
        ok("T2 totalArestas() após removerVertice(isolado) = " + esperado,
           obtido == esperado, "esperado=" + esperado + " obtido=" + obtido);
    }

    // ─── T3: verticeMaisProximo — ponto perto de vértice conhecido ────────────
    static void testeVerticeMaisProximo() {
        // Vértices em posições exatas
        Grafo g = new Grafo();
        g.adicionarVertice(0,  0);   // id 0
        g.adicionarVertice(10, 0);   // id 1
        g.adicionarVertice(20, 0);   // id 2

        // Ponto (9.9, 0.1) é mais perto do vértice 1 (10,0)
        int id = g.verticeMaisProximo(9.9, 0.1);
        ok("T3 verticeMaisProximo(9.9,0.1) == 1", id == 1,
           "esperado=1 obtido=" + id);
    }

    // ─── T4: verticeMaisProximo — grafo vazio retorna -1 ─────────────────────
    static void testeVerticeMaisProximoGrafoVazio() {
        Grafo g = new Grafo();
        int id = g.verticeMaisProximo(5.0, 5.0);
        ok("T4 verticeMaisProximo em grafo vazio == -1", id == -1,
           "esperado=-1 obtido=" + id);
    }

    // ─── T5: verticeMaisProximo — pula vértice removido ──────────────────────
    static void testeVerticeMaisProximoComRemovido() {
        Grafo g = new Grafo();
        g.adicionarVertice(0,  0);   // id 0
        g.adicionarVertice(1,  0);   // id 1  ← mais próximo de (1,0) antes da remoção
        g.adicionarVertice(50, 0);   // id 2

        g.removerVertice(1);  // remove o mais próximo

        // Agora o mais próximo de (1,0) deve ser o id 0 (dist=1), não id 2 (dist=49)
        int id = g.verticeMaisProximo(1.0, 0.0);
        ok("T5 verticeMaisProximo pula removido → retorna id 0", id == 0,
           "esperado=0 obtido=" + id);
    }

    // ─── T6: regressão com o campus real ─────────────────────────────────────
    static void testeRegressaoCampus(String poly) throws Exception {
        System.out.println("  [T6] Carregando " + poly + " ...");
        Grafo g = new Grafo();
        g.carregarDoArquivo(poly);

        ok("T6a totalVertices() == 10000",
           g.totalVertices() == 10000,
           "obtido=" + g.totalVertices());

        ok("T6b totalArestas() == 11526",
           g.totalArestas() == 11526,
           "obtido=" + g.totalArestas());

        // getVertices() não deve lançar exceção e deve ter 10000 elementos
        ok("T6c getVertices().size() == 10000",
           g.getVertices().size() == 10000,
           "obtido=" + g.getVertices().size());

        // Dijkstra 1222 → 9997 (distância conhecida ≈ 1180.63)
        Dijkstra dijk = new Dijkstra();
        ResultadoDijkstra r = dijk.calcular(g, 1222, 9997);

        ok("T6d Dijkstra 1222→9997 tem caminho", r.temCaminho(),
           "caminho vazio");

        boolean distOk = Math.abs(r.distanciaTotal - 1180.6323) < 0.01;
        ok("T6e distância ≈ 1180.63",
           distOk,
           String.format("obtido=%.4f", r.distanciaTotal));

        ok("T6f vértices no caminho == 45",
           r.caminho.size() == 45,
           "obtido=" + r.caminho.size());

        System.out.printf("       dist=%.4f | saltos=%d | explorados=%d | %d ms%n",
            r.distanciaTotal, r.caminho.size(), r.nosExplorados, r.tempoMs);
    }

    // ─── Utilitário de asserção ───────────────────────────────────────────────
    static void ok(String nome, boolean cond, String detalhe) {
        if (cond) {
            System.out.println("  [OK] " + nome);
            passou++;
        } else {
            System.out.println("  [FALHOU] " + nome + "  ← " + detalhe);
            falhou++;
        }
    }
}
