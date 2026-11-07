package hai913i.tp2.clustering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hai913i.tp2.coupling.CouplingMatrix;
import hai913i.tp2.input.Tp1FactsAdapter;

/** Point de contrôle B2. */
class DendrogramTest {

  private static CouplingMatrix c;
  private static AgglomerativeClustering ac;

  @BeforeAll
  static void construire() throws IOException {
    c = new CouplingMatrix(Tp1FactsAdapter.from(
            Path.of("../../static-analysis-jdt/static-analysis/resources/validation")));
    ac = new AgglomerativeClustering(c.classes(), c::coupling);
  }

  @Test
  void quinzeFeuillesEtQuatorzeNoeudsInternes() {
    List<Cluster> leaves = new ArrayList<>(), internal = new ArrayList<>();
    walk(ac.root(), leaves, internal);
    assertEquals(15, leaves.size());
    assertEquals(14, internal.size());
  }

  @Test
  void membresDUnNoeudUnionDisjointeDeSesFils() {
    List<Cluster> leaves = new ArrayList<>(), internal = new ArrayList<>();
    walk(ac.root(), leaves, internal);
    for (Cluster n : internal) {
      Set<String> l = new HashSet<>(n.left().members());
      Set<String> r = new HashSet<>(n.right().members());
      assertTrue(l.stream().noneMatch(r::contains), "fils non disjoints au nœud " + n.id());
      Set<String> union = new HashSet<>(l);
      union.addAll(r);
      assertEquals(union, new HashSet<>(n.members()), "nœud " + n.id());
      assertEquals(l.size() + r.size(), n.size());
    }
  }

  @Test
  void racineContientLesQuinzeClassesUneSeuleFois() {
    assertEquals(c.classes(), ac.root().members());          // triées, sans doublon
    assertEquals(15, new HashSet<>(ac.root().members()).size());
  }

  @Test
  void deuxDernieresFusionsANull() {
    assertEquals(0.0, ac.root().similarity(), 1e-9);
    List<Merge> m = ac.merges();
    assertEquals(0.0, m.get(m.size() - 2).similarity(), 1e-9);
  }

  private static void walk(Cluster n, List<Cluster> leaves, List<Cluster> internal) {
    if (n.isLeaf()) { leaves.add(n); return; }
    internal.add(n);
    walk(n.left(), leaves, internal);
    walk(n.right(), leaves, internal);
  }
}