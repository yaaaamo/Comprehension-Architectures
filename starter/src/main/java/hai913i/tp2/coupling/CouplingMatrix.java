package hai913i.tp2.coupling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hai913i.tp2.Facts;

/** A1 : couplage entre classes, C(A,B) = r(A,B) / |E_inter|. Ne connaît que les faits. */
public final class CouplingMatrix {

  /** Un couple de classes distinctes, toujours écrit a < b (ordre alphabétique). */
  public record Pair(String a, String b, int arcs, double coupling) {}

  private final List<String> classes;     // les M classes, triées
  private final int internalArcs;         // arcs A -> A, écartés
  private final int interArcs;            // |E_inter|
  // table creuse : r[min][max] = nombre d'arcs, seuls les couples non nuls sont stockés
  private final Map<String, Map<String, Integer>> r = new TreeMap<>();

  public CouplingMatrix(Facts facts) {
    this.classes = facts.classNames();
    int internal = 0;
    int inter = 0;
    for (Facts.CallArc arc : facts.calls()) {          // un arc = une ligne, on ne somme PAS les sites
      String a = arc.callerClass();
      String b = arc.calleeClass();
      if (a.equals(b)) {                              // arc interne : cohésion, pas couplage
        internal++;
        continue;
      }
      inter++;
      r.computeIfAbsent(min(a, b), k -> new TreeMap<>()).merge(max(a, b), 1, Integer::sum);
    }
    this.internalArcs = internal;
    this.interArcs = inter;
  }

  public List<String> classes() { return classes; }
  public int internalArcs()     { return internalArcs; }
  public int interArcs()        { return interArcs; }

  /** r(A,B), symétrique ; 0 pour A = B ou pour un couple absent. */
  public int arcs(String a, String b) {
    if (a.equals(b)) return 0;
    return r.getOrDefault(min(a, b), Map.of()).getOrDefault(max(a, b), 0);
  }

  /** C(A,B) = r(A,B) / |E_inter| ; C(A,A) = 0. */
  public double coupling(String a, String b) {
    return interArcs == 0 ? 0.0 : (double) arcs(a, b) / interArcs;
  }

  /** Couples de couplage non nul, par couplage décroissant puis par noms (déterministe). */
  public List<Pair> nonZeroPairs() {
    List<Pair> pairs = new ArrayList<>();
    r.forEach((a, row) -> row.forEach((b, n) -> pairs.add(new Pair(a, b, n, (double) n / interArcs))));
    pairs.sort(Comparator.comparingInt(Pair::arcs).reversed()
            .thenComparing(Pair::a).thenComparing(Pair::b));
    return pairs;
  }

  /** Somme de C(A,B) sur tous les couples distincts : doit valoir 1. */
  public double sum() {
    return nonZeroPairs().stream().mapToDouble(Pair::coupling).sum();
  }

  private static String min(String x, String y) { return x.compareTo(y) <= 0 ? x : y; }
  private static String max(String x, String y) { return x.compareTo(y) <= 0 ? y : x; }
}
