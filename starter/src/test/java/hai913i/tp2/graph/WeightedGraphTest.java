package hai913i.tp2.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import hai913i.tp2.coupling.CouplingMatrix;
import hai913i.tp2.input.Tp1FactsAdapter;
import hai913i.tp2.output.GraphExporter;

/** Point de contrôle A2. */
class WeightedGraphTest {

  static WeightedGraph validation() throws IOException {
    return WeightedGraph.from(new CouplingMatrix(Tp1FactsAdapter.from(
            Path.of("../../static-analysis-jdt/static-analysis/resources/validation"))));
  }

  @Test
  void noeudsAretesEtIsoles() throws IOException {
    WeightedGraph g = validation();
    assertEquals(15, g.nodes().size());
    assertEquals(19, g.edges().size());
    assertEquals(List.of("library.model.Dvd", "library.service.LoanException"), g.isolated());
  }

  @Test
  void cinqAretesLesPlusLourdes() throws IOException {
    List<String> top = validation().top(5).stream()
            .map(e -> simple(e.a()) + "-" + simple(e.b())).toList();
    assertEquals(List.of("Item-LoanService", "LoanService-Loan",
            "LibraryApp-Item", "LibraryApp-Catalog", "LibraryApp-LoanService"), top);
  }

  @Test
  void lExportSeRechargeAvecLesMemesValeurs(@TempDir Path tmp) throws IOException {
    WeightedGraph g = validation();
    Path csv = tmp.resolve("graphe.csv");
    GraphExporter.writeCsv(g, csv);
    WeightedGraph relu = GraphExporter.readCsv(csv);
    assertEquals(g.nodes(), relu.nodes());
    assertEquals(g.edges(), relu.edges());     // records : égalité champ par champ, doubles exacts
  }

  private static String simple(String q) { return q.substring(q.lastIndexOf('.') + 1); }
}
