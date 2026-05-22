import java.util.ArrayList;
import java.util.List;

/**
 * Encapsula o resultado de uma execução do algoritmo de Dijkstra.
 *
 * Campos populados por Dijkstra.calcular():
 *   caminho        — vértices do menor caminho, em ordem origem → destino
 *                    (lista vazia se não existir caminho)
 *   distanciaTotal — custo total do caminho (Double.POSITIVE_INFINITY se sem caminho)
 *   nosExplorados  — quantidade de vértices extraídos do heap (métrica de desempenho)
 *   tempoMs        — tempo de execução em milissegundos (medido com nanoTime)
 */
public class ResultadoDijkstra {

    public List<Integer> caminho;
    public double        distanciaTotal;
    public int           nosExplorados;
    public long          tempoMs;

    public ResultadoDijkstra() {
        caminho        = new ArrayList<>();
        distanciaTotal = Double.POSITIVE_INFINITY;
        nosExplorados  = 0;
        tempoMs        = 0;
    }

    /** @return true se o algoritmo encontrou um caminho válido. */
    public boolean temCaminho() {
        return !caminho.isEmpty();
    }

    @Override
    public String toString() {
        if (!temCaminho()) {
            return String.format(
                "Sem caminho encontrado | nós explorados: %d | tempo: %d ms",
                nosExplorados, tempoMs
            );
        }
        return String.format(
            "Distância total: %.4f | vértices no caminho: %d | nós explorados: %d | tempo: %d ms",
            distanciaTotal, caminho.size(), nosExplorados, tempoMs
        );
    }
}
