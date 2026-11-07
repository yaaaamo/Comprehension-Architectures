package hai913i.tp2.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import hai913i.tp2.coupling.CouplingMatrix;

/** A2 : graphe de couplage pondéré, non orienté. Un nœud par classe, une arête par couple non nul. */
public final class WeightedGraph {

  /** Arête non orientée, toujours écrite a < b ; weight = C(a,b). */
  public record Edge(String a, String b, int arcs, double weight) {}

  private final List<String> nodes;   // triés
  private final List<Edge> edges;     // par poids décroissant, puis noms

  public WeightedGraph(List<String> nodes, List<Edge> edges) {
    this.nodes = List.copyOf(nodes);
    this.edges = List.copyOf(edges);
  }

  /** Construit le graphe à partir du couplage (A1). */
  public static WeightedGraph from(CouplingMatrix coupling) {
    List<Edge> edges = new ArrayList<>();
    for (CouplingMatrix.Pair p : coupling.nonZeroPairs()) {
      edges.add(new Edge(p.a(), p.b(), p.arcs(), p.coupling()));
    }
    return new WeightedGraph(coupling.classes(), edges);
  }

  public List<String> nodes() { return nodes; }
  public List<Edge> edges()   { return edges; }

  /** Les n arêtes les plus lourdes. */
  public List<Edge> top(int n) {
    return edges.subList(0, Math.min(n, edges.size()));
  }

  /** Classes sans aucune arête. */
  public List<String> isolated() {
    Set<String> touched = new TreeSet<>();
    for (Edge e : edges) {
      touched.add(e.a());
      touched.add(e.b());
    }
    return nodes.stream().filter(n -> !touched.contains(n)).toList();
  }
}