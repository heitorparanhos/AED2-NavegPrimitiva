package core;

import java.util.Arrays;

/**
 * Fila de prioridade mínima (min-heap) para pares (vértice, distância).
 *
 * Estrutura central: array heap[] armazena os ids dos vértices; array chave[]
 * guarda a distância de cada vértice; array posicao[] mantém o índice de cada
 * vértice dentro do heap (-1 = ausente), permitindo que diminuirChave() localize
 * o elemento e execute heapify-up em O(lg V) sem varredura linear.
 *
 * Toda troca de elementos atualiza posicao[] de ambos os envolvidos — esse
 * invariante é o que garante a corretude das operações de heap.
 */
public class HeapMinima {

    private final int[]    heap;     // heap[i] = id do vértice na posição i
    private final double[] chave;    // chave[v] = distância atual do vértice v
    private final int[]    posicao;  // posicao[v] = índice de v no heap; -1 se ausente
    private int            tamanho;  // número de elementos presentes no heap

    /**
     * @param capacidade número máximo de vértices distintos que podem ser inseridos
     *                   (tipicamente igual ao número total de vértices do grafo)
     */
    public HeapMinima(int capacidade) {
        heap    = new int[capacidade];
        chave   = new double[capacidade];
        posicao = new int[capacidade];
        Arrays.fill(posicao, -1);
        tamanho = 0;
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    /** @return true se o heap não contém nenhum elemento. */
    public boolean vazia() {
        return tamanho == 0;
    }

    /** @return true se o vértice v está no heap. */
    public boolean contem(int v) {
        return posicao[v] != -1;
    }

    // ─── Operações principais ─────────────────────────────────────────────────

    /**
     * Insere o vértice v com distância dist e restaura a propriedade de heap
     * via heapify-up.
     */
    public void inserir(int v, double dist) {
        heap[tamanho]  = v;
        chave[v]       = dist;
        posicao[v]     = tamanho;
        tamanho++;
        subir(posicao[v]);
    }

    /**
     * Remove e retorna o vértice com menor distância.
     * Move o último elemento para a raiz e restaura via heapify-down.
     */
    public int extrairMin() {
        int min = heap[0];

        tamanho--;
        if (tamanho > 0) {
            // Traz o último elemento para a raiz antes de descer
            trocar(0, tamanho);
            posicao[min] = -1;
            descer(0);
        } else {
            posicao[min] = -1;
        }

        return min;
    }

    /**
     * Atualiza a distância de v (que já está no heap) para novaDist (menor que
     * a atual) e restaura a propriedade de heap via heapify-up.
     */
    public void diminuirChave(int v, double novaDist) {
        chave[v] = novaDist;
        subir(posicao[v]);   // posicao[v] já aponta para o índice correto
    }

    // ─── Heapify interno ──────────────────────────────────────────────────────

    /** Sobe o elemento no índice i até que a propriedade de heap seja satisfeita. */
    private void subir(int i) {
        while (i > 0) {
            int pai = (i - 1) / 2;
            if (chave[heap[pai]] > chave[heap[i]]) {
                trocar(pai, i);
                i = pai;
            } else {
                break;
            }
        }
    }

    /** Desce o elemento no índice i até que a propriedade de heap seja satisfeita. */
    private void descer(int i) {
        while (true) {
            int menor = i;
            int esq   = 2 * i + 1;
            int dir   = 2 * i + 2;

            if (esq < tamanho && chave[heap[esq]] < chave[heap[menor]]) menor = esq;
            if (dir < tamanho && chave[heap[dir]] < chave[heap[menor]]) menor = dir;

            if (menor != i) {
                trocar(i, menor);
                i = menor;
            } else {
                break;
            }
        }
    }

    /**
     * Troca os elementos nas posições i e j do heap e atualiza posicao[]
     * para ambos — invariante obrigatório para que diminuirChave() funcione.
     */
    private void trocar(int i, int j) {
        int vi = heap[i];
        int vj = heap[j];
        heap[i]     = vj;
        heap[j]     = vi;
        posicao[vi] = j;
        posicao[vj] = i;
    }
}