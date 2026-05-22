import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Algoritmo de Dijkstra para menor caminho em grafos ponderados com pesos ≥ 0.
 *
 * Complexidade: O((V + E) lg V)
 *   — onde V = total de vértices e E = total de arestas (direcionadas)
 *   — graças à combinação de lista de adjacência no Grafo e HeapMinima.
 *
 * Comparação com a versão antiga (MenorCaminhoDijkstra):
 *   Antiga : O(V²) com matriz de adjacência + busca linear do mínimo
 *   Nova   : O((V+E) lg V) com lista de adjacência + heap binária
 *   Para V=10 000 e E≈23 000: ~100 M ops → ~460 K ops  (220× mais rápido)
 */
public class Dijkstra {

    /**
     * Calcula o menor caminho de origem a destino no grafo g.
     *
     * @param g       grafo com lista de adjacência (Grafo.getAdjacencia)
     * @param origem  id_interno do vértice de partida
     * @param destino id_interno do vértice de chegada
     * @return ResultadoDijkstra preenchido com caminho, distância, estatísticas e tempo
     */
    public ResultadoDijkstra calcular(Grafo g, int origem, int destino) {
        int n = g.totalVertices();
        ResultadoDijkstra resultado = new ResultadoDijkstra();

        // ── Inicialização ─────────────────────────────────────────────────────
        double[] dist = new double[n];
        int[]    prev = new int[n];

        for (int i = 0; i < n; i++) {
            dist[i] = Double.POSITIVE_INFINITY;
            prev[i] = -1;
        }

        dist[origem] = 0.0;
        HeapMinima heap = new HeapMinima(n);
        heap.inserir(origem, 0.0);

        // ── Relaxamento ───────────────────────────────────────────────────────
        long inicio = System.nanoTime();

        while (!heap.vazia()) {
            int u = heap.extrairMin();
            resultado.nosExplorados++;

            // Early exit: assim que o destino for extraído, temos a distância mínima
            if (u == destino) break;

            // Se dist[u] == INF, todos os vértices restantes são inalcançáveis
            if (dist[u] == Double.POSITIVE_INFINITY) break;

            // Percorre vizinhos de u
            for (Aresta a : g.getAdjacencia(u)) {
                int    v        = a.dest;
                double novaDist = dist[u] + a.dist;

                if (novaDist < dist[v]) {
                    dist[v] = novaDist;
                    prev[v] = u;

                    if (heap.contem(v)) {
                        // v já está no heap: apenas ajusta a chave
                        heap.diminuirChave(v, novaDist);
                    } else {
                        // v ainda não foi inserido
                        heap.inserir(v, novaDist);
                    }
                }
            }
        }

        resultado.tempoMs        = (System.nanoTime() - inicio) / 1_000_000L;
        resultado.distanciaTotal = dist[destino];

        // ── Reconstrução do caminho ───────────────────────────────────────────
        if (dist[destino] < Double.POSITIVE_INFINITY) {
            List<Integer> caminho = new ArrayList<>();
            for (int v = destino; v != -1; v = prev[v]) {
                caminho.add(v);
                // Guarda contra ciclos inesperados no array prev
                if (caminho.size() > n) break;
            }
            Collections.reverse(caminho);
            resultado.caminho = caminho;
        }

        return resultado;
    }
}
