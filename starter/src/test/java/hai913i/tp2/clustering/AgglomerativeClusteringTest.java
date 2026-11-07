package hai913i.tp2.clustering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hai913i.tp2.Facts;
import hai913i.tp2.coupling.CouplingMatrix;
import hai913i.tp2.input.Tp1FactsAdapter;

/** Point de contrôle B1 et invariants du regroupement (annexe 1, § 6). */
class AgglomerativeClusteringTest {

  private static final double EPS = 1e-9;
  private static CouplingMatrix c;
  private static List<Merge> merges;

  @BeforeAll
  static void regrouper() throws IOException {
    c = new CouplingMatrix(Tp1FactsAdapter.from(
            Path.of("../../static-analysis-jdt/static-analysis/resources/validation")));
    merges = new AgglomerativeClustering(c.classes(), c::coupling).merges();
  }

  @Test
  void miniCalculeALaMain() throws IOException {          // annexe 1, § 2.1
    CouplingMatrix mini = new CouplingMatrix(Facts.load(Path.of("src/test/resources"), "facts-mini"));
    List<Merge> m = new AgglomerativeClustering(mini.classes(), mini::coupling).merges();
    assertEquals(2, m.size());
    assertEquals(2.0 / 3, m.get(0).similarity(), EPS);    // {A} + {B}
    assertEquals(1.0 / 6, m.get(1).similarity(), EPS);    // {A,B} + {C}
  }

  @Test
  void quatorzeFusions() {
    assertEquals(14, merges.size());
  }

  @Test
  void troisPremieresFusions() {
    assertMerge(merges.get(0), "[Item]", "[LoanService]", 4.0 / 37);
    assertMerge(merges.get(1), "[LibraryApp]", "[Catalog]", 3.0 / 37);
    assertMerge(merges.get(2), "[Item, LoanService]", "[LibraryApp, Catalog]", 9.0 / 148);
  }

  @Test
  void similaritesNonCroissantesEtDeuxDernieresNulles() {
    for (int i = 1; i < merges.size(); i++) {
      assertTrue(merges.get(i).similarity() <= merges.get(i - 1).similarity() + EPS,
              "remontée à l'étape " + (i + 1));
    }
    assertEquals(0.0, merges.get(12).similarity(), EPS);
    assertEquals(0.0, merges.get(13).similarity(), EPS);
  }

  @Test
  void lesGroupesActifsFormentUnePartition() {
    TreeSet<Integer> active = new TreeSet<>();
    for (int i = 0; i < 15; i++) active.add(i);
    for (Merge m : merges) {
      assertTrue(active.remove(m.a().id()) && active.remove(m.b().id()), "groupe fusionné deux fois");
      active.add(m.result().id());
    }
    assertEquals(1, active.size());
    assertEquals(c.classes(), merges.get(13).result().members());
  }

  @Test
  void lienMoyenEgalFormuleIncrementale() {               // annexe 1, § 2 : excellent test
    Merge m = merges.get(2);                             // {Item,LoanService} + {LibraryApp,Catalog}
    Cluster g1 = m.a().left(), g2 = m.a().right(), g3 = m.b();
    double direct = AgglomerativeClustering.averageLink(m.a(), g3, c::coupling);
    double incr = (g1.size() * AgglomerativeClustering.averageLink(g1, g3, c::coupling)
            + g2.size() * AgglomerativeClustering.averageLink(g2, g3, c::coupling))
            / (g1.size() + g2.size());
    assertEquals(direct, incr, EPS);
  }

  @Test
  void deuxExecutionsDonnentLaMemeSuite() {
    assertEquals(trace(merges), trace(new AgglomerativeClustering(c.classes(), c::coupling).merges()));
  }

  private static List<String> trace(List<Merge> ms) {
    List<String> t = new ArrayList<>();
    for (Merge m : ms) t.add(m.a().id() + "+" + m.b().id() + "@" + m.similarity());
    return t;
  }

  private static void assertMerge(Merge m, String a, String b, double sim) {
    assertEquals(a, simple(m.a().members()));
    assertEquals(b, simple(m.b().members()));
    assertEquals(sim, m.similarity(), EPS);
  }

  private static String simple(List<String> qs) {
    return qs.stream().map(q -> q.substring(q.lastIndexOf('.') + 1)).toList().toString();
  }
}