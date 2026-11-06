package hai913i.tp2.input;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.extraction.FactExtractor;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp2.Facts;

/**
 * Chemin 1 : branche le modèle de faits du TP 1 sur l'entrée du TP 2.
 * C'est la SEULE classe du TP 2 qui connaît les classes du TP 1.
 */
public final class Tp1FactsAdapter {

  private Tp1FactsAdapter() {}

  public static Facts from(Path projectDir) throws IOException {
    // 1. Chaîne du TP 1 : sources -> AST -> faits -> graphe d'appel
    ProjectSources sources = ProjectSources.of(projectDir);
    if (sources.javaFiles().isEmpty()) {
      throw new IOException("aucun fichier .java sous " + projectDir.toAbsolutePath());
    }
    ProjectFacts pf = FactExtractor.extract(JdtParser.parse(sources, List.of()));
    CallGraph graph = CallGraph.build(pf);

    // 2. Classes et propriétaire de chaque méthode : lus dans les types,
    //    PAS dans les appels (sinon Dvd et LoanException disparaissent)
    List<Facts.ClassInfo> classes = new ArrayList<>();
    Map<String, String> owners = new TreeMap<>();
    for (TypeFact t : pf.types()) {
      String q = t.qualifiedName();
      String simple = q.substring(q.lastIndexOf('.') + 1);
      classes.add(new Facts.ClassInfo(q, simple, t.packageName()));
      for (MethodFact m : t.methods()) {
        owners.put(m.id(), q);
      }
    }
    classes.sort(Comparator.comparing(Facts.ClassInfo::name));   // ordre = numérotation du départage

    // 3. Arcs : un par couple (appelante, appelée) distinct ; weight = nombre de sites
    List<Facts.CallArc> arcs = new ArrayList<>();
    for (CallGraph.Edge e : graph.edges()) {
      arcs.add(new Facts.CallArc(e.caller(), owners.get(e.caller()),
              e.callee(), owners.get(e.callee()),
              e.weight()));
    }
    return Facts.of(classes, owners, arcs);
  }
}