package hai913i.tp2.coupling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hai913i.tp2.Facts;
import hai913i.tp2.input.Tp1FactsAdapter;

/** Point de contrôle A1 et invariants du couplage (annexe 1, § 6). */
class CouplingMatrixTest {

  private static final double EPS = 1e-9;
  private static CouplingMatrix mini;
  private static CouplingMatrix validation;

  @BeforeAll
  static void charger() throws IOException {
    mini = new CouplingMatrix(Facts.load(Path.of("src/test/resources"), "facts-mini"));
    validation = new CouplingMatrix(Tp1FactsAdapter.from(
            Path.of("../../static-analysis-jdt/static-analysis/resources/validation")));
  }

  // ---- micro jeu de données : valeurs calculées à la main (annexe 1, § 2.1)
  @Test
  void miniValeursALaMain() {
    assertEquals(3, mini.interArcs());
    assertEquals(2.0 / 3, mini.coupling("demo.A", "demo.B"), EPS);
    assertEquals(1.0 / 3, mini.coupling("demo.B", "demo.C"), EPS);
    assertEquals(0.0, mini.coupling("demo.A", "demo.C"), EPS);
  }

  // ---- projet de validation : point de contrôle A1
  @Test
  void compteursA1() {
    assertEquals(7, validation.internalArcs());
    assertEquals(37, validation.interArcs());
    assertEquals(19, validation.nonZeroPairs().size());
  }

  @Test
  void coupleLePlusCouple() {
    CouplingMatrix.Pair top = validation.nonZeroPairs().get(0);
    assertEquals("library.model.Item", top.a());
    assertEquals("library.service.LoanService", top.b());
    assertEquals(4, top.arcs());
    assertEquals(4.0 / 37, top.coupling(), EPS);
  }

  @Test
  void sommeDesCouplagesVautUn() {          // le test « le plus rentable » de l'annexe
    assertEquals(1.0, validation.sum(), EPS);
    assertEquals(1.0, mini.sum(), EPS);
  }

  @Test
  void symetrieDiagonaleNulleEtBornes() {
    for (String a : validation.classes()) {
      assertEquals(0.0, validation.coupling(a, a), EPS);
      for (String b : validation.classes()) {
        double c = validation.coupling(a, b);
        assertEquals(c, validation.coupling(b, a), EPS);
        assertFalse(c < 0 || c > 1, a + " / " + b + " = " + c);
      }
    }
  }

  @Test
  void dvdEtLoanExceptionNApparaissentDansAucunCouple() {
    for (CouplingMatrix.Pair p : validation.nonZeroPairs()) {
      assertFalse(p.a().endsWith(".Dvd") || p.b().endsWith(".Dvd"));
      assertFalse(p.a().endsWith(".LoanException") || p.b().endsWith(".LoanException"));
    }
  }
}
