# AED2-NavegPrimitiva
**Sistema de Navegação Primitivo — Algoritmo de Dijkstra**  
INF/UFG — Algoritmos e Estruturas de Dados 2 — 2026-1  
Prof. André Luiz Moura

---

## Integrantes
- Heitor Paranhos Carvalho
- Lucas Santana Dalla Dea
- Matheus Gomes Rodrigues
- Vitor Fernandes de Paula

---

## Pré-requisitos

- **Java JDK 17 ou superior**  
  Verificar com: `java -version`  
  Download: https://adoptium.net (selecionar **Temurin 21 LTS**)

---

## Estrutura do projeto

```
PF-Navegador-Dijkstra/
├── src/
│   ├── core/       → Grafo, Dijkstra, HeapMinima, Vertice, Aresta, ResultadoDijkstra
│   └── ui/         → GrafoVisual (interface gráfica)
├── data/           → Arquivos de mapa (.poly, .osm)
├── bin/            → Classes compiladas (gerado na compilação)
├── GrafoVisual.jar → Executável empacotado
└── README.md
```

---

## Instalação e execução

### Opção A — JAR executável (recomendado)

```bash
java -jar GrafoVisual.jar
```

### Opção B — Compilar e executar pelo código-fonte

**1. Compilar:**
```bash
javac -d bin src/core/*.java src/ui/*.java
```

**2. Executar:**
```bash
java -cp bin ui.GrafoVisual
```

**3. Opcional — passar arquivo de mapa direto:**
```bash
java -cp bin ui.GrafoVisual data/Campus2UFG_Regiao.poly
```

### Gerar o JAR (se necessário recriar)

```bash
echo "Main-Class: ui.GrafoVisual" > manifest.txt
jar cfm GrafoVisual.jar manifest.txt -C bin .
```

---

## Como usar

### Abrir um mapa
1. Clique em **Abrir arquivo** no painel lateral
2. Selecione um arquivo `.poly` ou `.txt`
3. O mapa é carregado e exibido no canvas

Formatos suportados:
- `.poly` — formato do professor (Campus2UFG_Regiao.poly) e formato customizado (`*VERTICES` / `*ARESTAS`)
- `.txt` — lista de arestas (`origem destino` por linha), com layout circular automático

### Calcular o menor caminho (Dijkstra)
1. Certifique-se de estar no modo **Navegar**
2. Clique no vértice de **origem** (fica amarelo)
3. Clique no vértice de **destino** (o caminho é traçado em verde)
4. As estatísticas aparecem no painel: custo total, saltos, nós explorados, tempo

### Modos de edição
Clique no botão **MODO** para alternar entre:

| Modo | Ação |
|------|------|
| Navegar | Clique em dois vértices para calcular rota |
| Adicionar vértice | Clique no canvas para criar um vértice |
| Adicionar aresta | Clique em dois vértices para ligá-los |
| Remover vértice | Clique em um vértice para apagá-lo |
| Remover aresta | Clique em uma aresta para apagá-la |

**Mão única:** marque o checkbox **Mao unica** antes de criar a aresta.

### Navegação no canvas
- **Scroll** — zoom
- **Botão do meio / Ctrl+arrastar** — deslocar (pan)
- **Duplo clique** — ajustar à tela

### Outras funções
- **Limpar caminho** — remove a rota exibida
- **Ajustar à tela** — centraliza o grafo
- **Copiar imagem** — copia o canvas para a área de transferência (Ctrl+V em qualquer editor de imagem)
- **Salvar grafo** — exporta o grafo atual como `.poly`

---

## Arquivos de teste

| Arquivo | Vértices | Arestas | Descrição |
|---------|----------|---------|-----------|
| `Campus2UFG_Regiao.poly` | 10.000 | 11.526 | Campus Samambaia UFG e arredores |
| `data/teste.poly` | 3 | — | Grafo mínimo de teste |

---

## Executar testes do núcleo

```bash
java -cp bin core.TesteCore data/Campus2UFG_Regiao.poly
```

Roda bateria de testes sobre `Grafo`, `HeapMinima` e `Dijkstra`, incluindo regressão com o mapa do campus.

---

## Compatibilidade

- Windows 10/11
- Linux (Ubuntu 20.04+, Debian)
- macOS 12+ (execução via terminal)