package hai913i.tp2.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import hai913i.tp2.Facts;

/** Point de contrôle A0 sur le chemin 1, et validation croisée avec le jeu canonique. */
class Tp1FactsAdapterTest {

  // chemins relatifs au dossier du module (starter/), là où Maven lance les tests
  private static final Path VALIDATION =
          Path.of("../../static-analysis-jdt/static-analysis/resources/validation");
  private static final Path CANONIQUE = Path.of("../resources");

  @Test
  void pointDeControleA0() throws IOException {
    Facts f = Tp1FactsAdapter.from(VALIDATION);
    assertEquals(15, f.classes().size());
    assertEquals(59, f.methodOwners().size());
    assertEquals(44, f.calls().size());
  }

  @Test
  void memesClassesEtMemesArcsQueLeJeuCanonique() throws IOException {
    Facts tp1 = Tp1FactsAdapter.from(VALIDATION);
    Facts csv = Facts.load(CANONIQUE);
    assertEquals(csv.classNames(), tp1.classNames());
    assertEquals(arcsEntreClasses(csv), arcsEntreClasses(tp1));
  }

  /** Chaque arc réduit à "ClasseAppelante -> ClasseAppelée", trié. */
  private static List<String> arcsEntreClasses(Facts f) {
    return f.calls().stream()
            .map(a -> a.callerClass() + " -> " + a.calleeClass())
            .sorted()
            .toList();
  }
}