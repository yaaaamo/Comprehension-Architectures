package hai913i.tp2.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Merge;

/**
 * B2 : trois restitutions du dendrogramme (annexe 1, § 4.2), lisibles sans exécuter le code :
 *  - arbre indenté      : une ligne par nœud, indentée selon la profondeur ;
 *  - liste des fusions  : une ligne par fusion (étape, similarité, groupe A, groupe B) ;
 *  - DOT (Graphviz)     : en ordonnée l'ordre des fusions, similarité réelle en étiquette (diapo 33 du CM 03).
 */
public final class DendrogramExporter {

  private DendrogramExporter() {}

  // ---------- arbre indenté ----------

  public static List<String> indentedTree(Cluster root, List<Merge> merges) {
    Map<Integer, Integer> stepOf = stepsById(merges);
    List<String> lines = new ArrayList<>();
    indent(root, 0, stepOf, lines);
    return lines;
  }

  private static void indent(Cluster c, int depth, Map<Integer, Integer> stepOf, List<String> lines) {
    String pad = "  ".repeat(depth);
    if (c.isLeaf()) {
      lines.add(pad + "- " + simple(c.members().get(0)) + "  [" + c.id() + "]");
      return;
    }
    lines.add(String.format(Locale.ROOT, "%s+ [%d] fusion %d, similarité %.4f, %d classes",
            pad, c.id(), stepOf.get(c.id()), c.similarity(), c.size()));
    indent(c.left(), depth + 1, stepOf, lines);
    indent(c.right(), depth + 1, stepOf, lines);
  }

  // ---------- liste des fusions ----------

  public static List<String> mergeList(List<Merge> merges) {
    List<String> lines = new ArrayList<>();
    lines.add("etape;similarite;idA;idB;idNouveau;groupeA;groupeB");
    for (Merge m : merges) {
      lines.add(String.format(Locale.ROOT, "%d;%.6f;%d;%d;%d;%s;%s",
              m.step(), m.similarity(), m.a().id(), m.b().id(), m.result().id(),
              group(m.a()), group(m.b())));
    }
    return lines;
  }

  // ---------- DOT ----------

  public static List<String> dot(Cluster root, List<Merge> merges) {
    Map<Integer, Integer> stepOf = stepsById(merges);
    List<String> lines = new ArrayList<>();
    lines.add("digraph dendrogramme {");
    lines.add("  rankdir=TB; nodesep=0.15; ranksep=0.35;");
    lines.add("  node [fontname=\"Helvetica\", fontsize=10];");
    lines.add("  edge [arrowhead=none];");
    List<String> leaves = new ArrayList<>();
    dotNode(root, stepOf, lines, leaves);
    lines.add("  { rank=same; " + String.join("; ", leaves) + "; }");   // feuilles alignées en bas
    lines.add("}");
    return lines;
  }

  private static void dotNode(Cluster c, Map<Integer, Integer> stepOf, List<String> lines, List<String> leaves) {
    String name = "n" + c.id();
    if (c.isLeaf()) {
      lines.add("  " + name + " [shape=box, style=rounded, label=\"" + simple(c.members().get(0)) + "\"];");
      leaves.add(name);
      return;
    }
    lines.add(String.format(Locale.ROOT,
            "  %s [shape=ellipse, label=\"fusion %d\\n%.4f\"];", name, stepOf.get(c.id()), c.similarity()));
    lines.add("  " + name + " -> n" + c.left().id() + ";");
    lines.add("  " + name + " -> n" + c.right().id() + ";");
    dotNode(c.left(), stepOf, lines, leaves);
    dotNode(c.right(), stepOf, lines, leaves);
  }

  // ---------- écriture ----------

  public static void write(List<String> lines, Path file) throws IOException {
    Files.createDirectories(file.toAbsolutePath().getParent());
    Files.write(file, lines, StandardCharsets.UTF_8);
  }

  // ---------- utilitaires ----------

  private static Map<Integer, Integer> stepsById(List<Merge> merges) {
    Map<Integer, Integer> stepOf = new HashMap<>();
    for (Merge m : merges) stepOf.put(m.result().id(), m.step());
    return stepOf;
  }

  private static String group(Cluster c) {
    return "{" + String.join(", ", c.members().stream().map(DendrogramExporter::simple).toList()) + "}";
  }

  private static String simple(String q) {
    return q.substring(q.lastIndexOf('.') + 1);
  }
}