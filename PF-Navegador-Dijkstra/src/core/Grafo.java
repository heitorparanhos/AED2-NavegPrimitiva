package core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grafo esparso representado por lista de adjacência.
 *
 * Memória: O(V + E) — muito mais eficiente do que a matriz V×V da versão
 * antiga para grafos com dezenas de milhares de vértices.
 *
 * A lista adjacencia[u] contém todas as arestas que *saem* de u; cada
 * aresta guarda dest e dist (distância euclidiana calculada a partir dos
 * campos (x,y) dos dois vértices).
 *
 * O peso não vem do arquivo .poly; é calculado em adicionarAresta() usando
 * dist = sqrt((x0-x1)² + (y0-y1)²).
 */
public class Grafo {

    /** Vértices indexados pelo id_interno (posição = id). */
    private final ArrayList<Vertice>            vertices;

    /** adjacencia.get(u) = arestas que saem do vértice u. */
    private final ArrayList<ArrayList<Aresta>>  adjacencia;

    /**
     * NÃO mantemos mais um contador explícito: totalArestas() recomputa sob
     * demanda varrendo as listas de adjacência.  Isso elimina qualquer
     * dessincronização ao misturar arestas de mão única e mão dupla (RF06).
     *
     * Algoritmo de contagem em totalArestas():
     *   Para cada arco (u -> w):
     *     • u < w  → conta sempre 1 (pega o arco pelo "lado menor")
     *     • u == w → conta 1 (self-loop, caso raro)
     *     • u > w  → conta 1 SOMENTE se o arco inverso (w -> u) NÃO existe,
     *                i.e., é uma mão única "de trás para frente" que ainda
     *                não foi capturada pela regra u < w.
     * Bidirecionais contribuem 1 (via u < w) e seu par inverso u > w é
     * ignorado.  Mão únicas contribuem exatamente 1 independentemente de
     * qual sentido têm.
     */

    public Grafo() {
        vertices   = new ArrayList<>();
        adjacencia = new ArrayList<>();
    }

    // ─── Carga do arquivo ─────────────────────────────────────────────────────

    /**
     * Lê um arquivo no formato .poly e constrói o grafo a partir do zero.
     *
     * Formato esperado (campos separados por TAB):
     *   Linha 1 :  nVertices  dim  _  _        (só o 1º número importa)
     *   nVertices linhas:  id  x  y
     *   1 linha  :  nArestas  _                 (só o 1º número importa)
     *   nArestas linhas:  idAresta  orig  dest  flag
     *   Última linha:  0  (marcador de fim)
     *
     *   flag 0 = mão dupla  →  adicionarAresta(orig, dest, true)
     *   flag 1 = mão única  →  adicionarAresta(orig, dest, false)
     *
     * Robustez: .trim() em cada token elimina \r, espaços extras e tabs
     * residuais; linhas em branco são ignoradas.
     *
     * @param caminho caminho absoluto ou relativo do arquivo .poly
     * @throws IOException se o arquivo não puder ser lido
     */
    public void carregarDoArquivo(String caminho) throws IOException {
        vertices.clear();
        adjacencia.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {

            // ── Linha 1: cabeçalho de vértices ────────────────────────────────
            String linha = lerProximaLinhaNaoVazia(br);
            if (linha == null) throw new IOException("Arquivo vazio: " + caminho);

            int nVertices = Integer.parseInt(linha.trim().split("\\s+")[0].trim());

            // Aloca posições (vértices inseridos pelo id, então precisamos de
            // um ArrayList indexado; preenchemos com null e depois setamos)
            for (int i = 0; i < nVertices; i++) {
                vertices.add(null);
                adjacencia.add(new ArrayList<>());
            }

            // ── Vértices ──────────────────────────────────────────────────────
            for (int i = 0; i < nVertices; i++) {
                linha = lerProximaLinhaNaoVazia(br);
                if (linha == null) break;

                String[] p = linha.trim().split("\\s+");
                int    id = Integer.parseInt(p[0].trim());
                double x  = Double.parseDouble(p[1].trim());
                double y  = Double.parseDouble(p[2].trim());

                vertices.set(id, new Vertice(id, x, y));
            }

            // ── Linha de cabeçalho de arestas ─────────────────────────────────
            linha = lerProximaLinhaNaoVazia(br);
            if (linha == null) return;

            String[] hp = linha.trim().split("\\s+");
            // Uma linha com só "0" é o marcador de fim antecipado
            if (hp[0].trim().equals("0") && hp.length == 1) return;

            int nArestas = Integer.parseInt(hp[0].trim());

            // ── Arestas ───────────────────────────────────────────────────────
            for (int i = 0; i < nArestas; i++) {
                linha = lerProximaLinhaNaoVazia(br);
                if (linha == null) break;

                // Marcador de fim antes do esperado
                if (linha.trim().equals("0")) break;

                String[] p = linha.trim().split("\\s+");
                if (p.length < 4) continue;

                // int idAresta = Integer.parseInt(p[0].trim()); // ignorado
                int orig = Integer.parseInt(p[1].trim());
                int dest = Integer.parseInt(p[2].trim());
                int flag = Integer.parseInt(p[3].trim());

                // flag 0 = bidirecional; flag 1 = mão única
                boolean maoDupla = (flag == 0);
                adicionarAresta(orig, dest, maoDupla);
            }
        }
    }

    // ─── Edição dinâmica (RF05 / RF06) ───────────────────────────────────────

    /**
     * Cria um novo vértice com coordenadas (x, y) e retorna seu id_interno.
     * O id é atribuído sequencialmente (= tamanho atual da lista).
     */
    public int adicionarVertice(double x, double y) {
        int id = vertices.size();
        vertices.add(new Vertice(id, x, y));
        adjacencia.add(new ArrayList<>());
        return id;
    }

    /**
     * Remove o vértice v e TODAS as arestas incidentes (tanto saindo de v
     * quanto chegando em v).  O slot fica null para preservar os ids dos
     * demais vértices.
     *
     * O contador totalArestas() recomputa sozinho — nenhum ajuste manual aqui.
     */
    public void removerVertice(int v) {
        if (v < 0 || v >= vertices.size() || vertices.get(v) == null) return;

        adjacencia.get(v).clear();
        vertices.set(v, null);

        for (int u = 0; u < adjacencia.size(); u++) {
            if (u == v) continue;
            adjacencia.get(u).removeIf(a -> a.dest == v);
        }
    }

    /**
     * Adiciona uma aresta de orig para dest com peso euclidiano.
     * Se maoDupla == true, insere também a direção inversa.
     * Incrementa totalArestas em 1 (aresta lógica), independente de maoDupla.
     */
    public void adicionarAresta(int orig, int dest, boolean maoDupla) {
        if (!verticeValido(orig) || !verticeValido(dest)) return;

        double dist = calcDist(
                vertices.get(orig).x, vertices.get(orig).y,
                vertices.get(dest).x, vertices.get(dest).y
        );

        adjacencia.get(orig).add(new Aresta(orig, dest, dist));

        if (maoDupla) {
            adjacencia.get(dest).add(new Aresta(dest, orig, dist));
        }
        // totalArestas() recomputa sob demanda — nenhum contador para atualizar
    }

    /**
     * Remove a aresta na direção orig → dest (apenas um sentido).
     * Para remover os dois sentidos de uma aresta bidirecional, chame
     * removerAresta(orig, dest) e removerAresta(dest, orig).
     */
    public void removerAresta(int orig, int dest) {
        if (orig < 0 || orig >= adjacencia.size()) return;
        adjacencia.get(orig).removeIf(a -> a.dest == dest);
        // totalArestas() recomputa sob demanda — nenhum contador para atualizar
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public int totalVertices() {
        return vertices.size();
    }

    /**
     * Conta arestas lógicas varrendo as listas de adjacência.
     * Ver comentário do campo para o critério de contagem.
     * Complexidade: O(V + E × grau_médio) — imperceptível para grafos esparsos.
     */
    public int totalArestas() {
        int count = 0;
        for (int u = 0; u < adjacencia.size(); u++) {
            for (Aresta a : adjacencia.get(u)) {
                int w = a.dest;
                if (u <= w || !existeArco(w, u)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Retorna o vértice de id_interno i, ou null se o slot foi removido.
     */
    public Vertice getVertice(int i) {
        if (i < 0 || i >= vertices.size()) return null;
        return vertices.get(i);
    }

    /**
     * Retorna a lista de arestas que saem do vértice v.
     * Usado pelo Dijkstra para iterar sobre os vizinhos de v.
     */
    public List<Aresta> getAdjacencia(int v) {
        if (v < 0 || v >= adjacencia.size()) return Collections.emptyList();
        return adjacencia.get(v);
    }

    /**
     * Retorna todas as listas de adjacência (uma por vértice), somente leitura.
     * Conveniente para a UI renderizar todas as arestas de uma vez.
     */
    public List<List<Aresta>> getAd() {
        return Collections.unmodifiableList(adjacencia);
    }

    /**
     * Retorna a lista de vértices indexada por id_interno, somente leitura.
     * Posições com null representam vértices removidos — a UI deve ignorá-las
     * ao iterar (verificar != null antes de usar cada elemento).
     */
    public List<Vertice> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    /**
     * Retorna o id_interno do vértice cujas coordenadas (x,y) estão mais
     * próximas do ponto fornecido (distância euclidiana).
     * Ignora slots removidos (null).
     *
     * Usa distância ao quadrado para comparar — evita sqrt desnecessário.
     * Complexidade: O(V).
     *
     * @return id_interno do vértice mais próximo, ou -1 se não houver vértice válido.
     */
    public int verticeMaisProximo(double x, double y) {
        int    melhorId   = -1;
        double melhorDist = Double.POSITIVE_INFINITY;

        for (int i = 0; i < vertices.size(); i++) {
            Vertice v = vertices.get(i);
            if (v == null) continue;

            double dx   = v.x - x;
            double dy   = v.y - y;
            double dist2 = dx * dx + dy * dy;   // distância ao quadrado

            if (dist2 < melhorDist) {
                melhorDist = dist2;
                melhorId   = i;
            }
        }

        return melhorId;
    }

    // ─── Auxiliares privados ──────────────────────────────────────────────────

    /** Lê a próxima linha não-vazia do reader; retorna null se EOF. */
    private String lerProximaLinhaNaoVazia(BufferedReader br) throws IOException {
        String linha;
        while ((linha = br.readLine()) != null) {
            if (!linha.trim().isEmpty()) return linha;
        }
        return null;
    }

    /** Distância euclidiana entre dois pontos. */
    private double calcDist(double x0, double y0, double x1, double y1) {
        double dx = x0 - x1;
        double dy = y0 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Verifica se o índice v corresponde a um vértice válido e não removido. */
    private boolean verticeValido(int v) {
        return v >= 0 && v < vertices.size() && vertices.get(v) != null;
    }

    /**
     * Retorna true se existe um arco físico de u para w na lista de adjacência.
     * Usado por totalArestas() para detectar arestas de mão única "para trás".
     */
    private boolean existeArco(int u, int w) {
        if (u < 0 || u >= adjacencia.size()) return false;
        for (Aresta a : adjacencia.get(u)) {
            if (a.dest == w) return true;
        }
        return false;
    }
}