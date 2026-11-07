package hai913i.tp2.cut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hai913i.tp2.clustering.AgglomerativeClustering;
import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.coupling.CouplingMatrix;
import hai913i.tp2.input.Tp1FactsAdapter;

/** Point de contrôle B3 et invariants de la coupe (annexe 1, § 6). */
class ModuleCutterTest {

  private static CouplingMatrix c;
  private static Cluster root;

  @BeforeAll
  static void preparer() throws IOException {
    c = new CouplingMatrix(Tp1FactsAdapter.from(
            Path.of("../../static-analysis-jdt/static-analysis/resources/validation")));
    root = new AgglomerativeClustering(c.classes(), c::coupling).root();
  }

  @Test
  void pointDeControleB3() {
    CutResult r015 = ModuleCutter.cut(root, c::coupling, 0.015);
    assertEquals(4, r015.modules().size());
    assertTrue(r015.respectsMHalf());
    assertTrue(r015.modules().stream().anyMatch(m -> m.size() == 11));   // trop grossier

    CutResult r025 = ModuleCutter.cut(root, c::coupling, 0.025);
    assertEquals(7, r025.modules().size());
    assertTrue(r025.respectsMHalf());                                    // tout juste : 7 = M/2

    CutResult r045 = ModuleCutter.cut(root, c::coupling, 0.045);
    assertEquals(10, r045.modules().size());
    assertFalse(r045.respectsMHalf());                                   // rejetée
  }

  @Test
  void moduleprincipalACp025() {                     // annexe 1, § 4.1 : C(G) = 0,0335
    ClassModule big = ModuleCutter.cut(root, c::coupling, 0.025).modules().stream()
            .filter(m -> m.size() == 7).findFirst().orElseThrow();
    assertEquals(0.0335, big.internalCoupling(), 5e-5);
    assertTrue(big.internalCoupling() > big.node().similarity());      // ≠ similarité de fusion
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 0.015, 0.025, 0.045, 0.1})
  void invariantsDeLaCoupe(double cp) {
    CutResult r = ModuleCutter.cut(root, c::coupling, cp);
    // partition des 15 classes : aucune oubliée, aucune en double
    List<String> all = new ArrayList<>();
    r.modules().forEach(m -> all.addAll(m.classes()));
    assertEquals(c.classes(), all.stream().sorted().toList());
    // tout module de 2 classes ou plus vérifie C(G) > CP
    for (ClassModule m : r.modules()) {
      if (m.size() >= 2) assertTrue(m.internalCoupling() > cp, m.classes() + " à CP " + cp);
    }
    // chaque module est une branche du dendrogramme
    for (ClassModule m : r.modules()) assertTrue(isSubtree(root, m.node()));
  }

  private static boolean isSubtree(Cluster n, Cluster target) {
    if (n == target) return true;
    return !n.isLeaf() && (isSubtree(n.left(), target) || isSubtree(n.right(), target));
  }
}