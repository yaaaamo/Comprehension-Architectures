package hai913i.tp2.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hai913i.tp2.graph.WeightedGraph;

/**
 * Exports du graphe (A2) :
 *  - CSV relisible par l'outil : une ligne "node;Nom" par classe (isolées comprises),
 *    une ligne "edge;A;B;arcs;poids" par arête ;
 *  - DOT pour Graphviz.
 */
public final class GraphExporter {

  private GraphExporter() {}

  public static void writeCsv(WeightedGraph g, Path file) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("kind;a;b;arcs;weight");
    for (String n : g.nodes()) {
      lines.add("node;" + n + ";;;");
    }
    for (WeightedGraph.Edge e : g.edges()) {
      // Double.toString : relecture exacte (aucun arrondi)
      lines.add("edge;" + e.a() + ";" + e.b() + ";" + e.arcs() + ";" + e.weight());
    }
    Files.createDirectories(file.toAbsolutePath().getParent());
    Files.write(file, lines, StandardCharsets.UTF_8);
  }

  public static WeightedGraph readCsv(Path file) throws IOException {
    List<String> nodes = new ArrayList<>();
    List<WeightedGraph.Edge> edges = new ArrayList<>();
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    for (int i = 1; i < lines.size(); i++) {               // ligne 0 = en-tête
      String[] f = lines.get(i).split(";", -1);
      try {
        switch (f[0]) {
          case "node" -> nodes.add(f[1]);
          case "edge" -> edges.add(new WeightedGraph.Edge(
                  f[1], f[2], Integer.parseInt(f[3]), Double.parseDouble(f[4])));
          default -> throw new IOException("type inconnu");
        }
      } catch (RuntimeException e) {
        throw new IOException(file + ", ligne " + (i + 1) + " mal formée : " + lines.get(i));
      }
    }
    return new WeightedGraph(nodes, edges);
  }

  public static void writeDot(WeightedGraph g, Path file) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("graph couplage {");
    lines.add("  layout=neato; overlap=false; node [shape=box, fontname=\"Helvetica\"];");
    for (String n : g.nodes()) {
      lines.add("  \"" + simple(n) + "\";");
    }
    for (WeightedGraph.Edge e : g.edges()) {
      lines.add(String.format(Locale.ROOT, "  \"%s\" -- \"%s\" [label=\"%d/%.4f\", penwidth=%.1f];",
              simple(e.a()), simple(e.b()), e.arcs(), e.weight(), 1 + 2.0 * e.arcs()));
    }
    lines.add("}");
    Files.createDirectories(file.toAbsolutePath().getParent());
    Files.write(file, lines, StandardCharsets.UTF_8);
  }

  private static String simple(String q) {
    return q.substring(q.lastIndexOf('.') + 1);
  }
}