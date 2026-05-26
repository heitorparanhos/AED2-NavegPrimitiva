package core;

import java.util.Scanner;

/**
 * Classe de teste do núcleo (HeapMinima + Grafo + Dijkstra).
 *
 * Uso:
 *   java core.TesteDijkstra <caminho_do_arquivo.poly> [origem] [destino]
 */
public class TesteDijkstra {

    public static void main(String[] args) throws Exception {

        System.out.println("=== Teste 1: HeapMinima ===");
        testarHeap();
        System.out.println();

        String caminho = args.length >= 1 ? args[0] : "../../data/ArquivosPoly";

        System.out.println("=== Teste 2: Grafo ===");
        System.out.println("Carregando: " + caminho);
        Grafo g = new Grafo();
        long t0 = System.nanoTime();
        g.carregarDoArquivo(caminho);
        long dtCarga = (System.nanoTime() - t0) / 1_000_000L;

        System.out.printf("Vértices : %d  (esperado: 10000)%n", g.totalVertices());
        System.out.printf("Arestas  : %d  (esperado: 11526)%n", g.totalArestas());
        System.out.printf("Carga    : %d ms%n", dtCarga);
        System.out.println();

        Vertice v0 = g.getVertice(0);
        Vertice v1 = g.getVertice(9999);
        System.out.printf("Vértice 0    : x=%.5f  y=%.5f%n", v0.x, v0.y);
        System.out.printf("Vértice 9999 : x=%.5f  y=%.5f%n", v1.x, v1.y);
        System.out.printf("Vizinhos do vértice 0 : %d%n", g.getAdjacencia(0).size());
        System.out.println();

        System.out.println("=== Teste 3: Dijkstra ===");

        int origem, destino;

        if (args.length >= 3) {
            origem  = Integer.parseInt(args[1]);
            destino = Integer.parseInt(args[2]);
        } else {
            Scanner sc = new Scanner(System.in);
            System.out.printf("Origem  (0 a %d): ", g.totalVertices() - 1);
            origem  = sc.nextInt();
            System.out.printf("Destino (0 a %d): ", g.totalVertices() - 1);
            destino = sc.nextInt();
            sc.close();
        }

        if (origem  < 0 || origem  >= g.totalVertices() ||
                destino < 0 || destino >= g.totalVertices()) {
            System.out.println("Índices inválidos.");
            return;
        }

        Dijkstra dijkstra = new Dijkstra();
        ResultadoDijkstra res = dijkstra.calcular(g, origem, destino);

        System.out.println(res);

        if (res.temCaminho()) {
            System.out.println("\nCaminho (vértices):");
            int lim = Math.min(res.caminho.size(), 20);
            for (int i = 0; i < lim; i++) {
                int id = res.caminho.get(i);
                Vertice vv = g.getVertice(id);
                System.out.printf("  [%d] id=%d  x=%.3f  y=%.3f%n", i, id, vv.x, vv.y);
            }
            if (res.caminho.size() > 20) {
                System.out.printf("  ... (%d vértices no total)%n", res.caminho.size());
            }
        }
    }

    private static void testarHeap() {
        boolean ok = true;

        HeapMinima h2 = new HeapMinima(10);
        double[] chaves = {5.0, 3.0, 7.0, 1.0, 4.0};
        for (int i = 0; i < 5; i++) h2.inserir(i, chaves[i]);

        double anterior = -1;
        boolean ordemOk = true;
        while (!h2.vazia()) {
            int v = h2.extrairMin();
            if (chaves[v] < anterior) { ordemOk = false; break; }
            anterior = chaves[v];
        }
        System.out.println("  [" + (ordemOk ? "OK" : "FALHOU") + "] extrairMin() retorna em ordem crescente");
        ok &= ordemOk;

        HeapMinima h3 = new HeapMinima(5);
        h3.inserir(0, 10.0);
        h3.inserir(1, 20.0);
        h3.inserir(2, 30.0);
        h3.diminuirChave(2, 5.0);
        int min = h3.extrairMin();
        boolean dimOk = (min == 2);
        System.out.println("  [" + (dimOk ? "OK" : "FALHOU") + "] diminuirChave() move elemento para o topo");
        ok &= dimOk;

        HeapMinima h4 = new HeapMinima(5);
        h4.inserir(0, 1.0);
        h4.inserir(1, 2.0);
        boolean contemOk = h4.contem(0) && h4.contem(1) && !h4.contem(2);
        System.out.println("  [" + (contemOk ? "OK" : "FALHOU") + "] contem() correto antes/depois de inserir");
        ok &= contemOk;

        h4.extrairMin();
        h4.extrairMin();
        boolean vaziaOk = h4.vazia();
        System.out.println("  [" + (vaziaOk ? "OK" : "FALHOU") + "] vazia() retorna true após extrair tudo");
        ok &= vaziaOk;

        System.out.println("  HeapMinima: " + (ok ? "TODOS OS TESTES PASSARAM ✓" : "HÁ FALHAS ✗"));
    }
}