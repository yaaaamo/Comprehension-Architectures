package hai913i.tp2;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.nio.file.Files;

import hai913i.tp2.clustering.AgglomerativeClustering;
import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Merge;
import hai913i.tp2.coupling.CouplingMatrix;
import hai913i.tp2.cut.ClassModule;
import hai913i.tp2.cut.CutResult;
import hai913i.tp2.cut.ModuleCutter;
import hai913i.tp2.graph.WeightedGraph;
import hai913i.tp2.input.Tp1FactsAdapter;
import hai913i.tp2.output.DendrogramExporter;
import hai913i.tp2.output.GraphExporter;

/**
 * Point d'entrée unique de l'outil du TP 2.
 *
 * <p>Charge les faits, soit par notre extracteur du TP 1 sur un dossier de
 * sources Java (chemin 1, via {@link hai913i.tp2.input.Tp1FactsAdapter}), soit
 * depuis un jeu de faits au format canonique ({@code *-classes.csv}), puis
 * enchaîne les étapes A0 à B3 : couplage, graphe pondéré, regroupement
 * hiérarchique, dendrogramme et coupe selon le seuil {@code --cp}.</p>
 *
 * <pre>
 *   mvn clean package
 *   java -jar target/hai913i-tp2.jar DOSSIER [--cp 0.025] [--out DOSSIER] [--prefix PREFIXE]
 * </pre>
 *
 * <p><b>Codes de sortie</b> : 0 si l'exécution se termine (y compris quand la
 * contrainte M/2 est violée, ce qui est signalé dans la sortie), 1 si les
 * arguments sont invalides, 2 si les faits sont illisibles ou les exports
 * impossibles à écrire.</p>
 */
public final class Main {

    /**
     * Sortie standard forcée en UTF-8.
     *
     * <p>Sans cela, une console Windows configurée sur une autre page de codes
     * afficherait les accents de travers. Le jeu de données, les sources et
     * cette sortie sont tous en UTF-8.</p>
     */
    private static final PrintStream OUT =
            new PrintStream(System.out, true, StandardCharsets.UTF_8);

    /** Sortie d'erreur, forcée en UTF-8 pour la même raison. */
    private static final PrintStream ERR =
            new PrintStream(System.err, true, StandardCharsets.UTF_8);

    /** Seuil de coupe par défaut, repris du point de contrôle B3 du sujet. */
    private static final double CP_DEFAUT = 0.025;

    private Main() {
    }

    /**
     * Charge les faits et affiche les compteurs de l'étape A0.
     *
     * @param args {@code <dossier-des-faits> [--cp <valeur>] [--prefix <préfixe>]}
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            usage("chemin du dossier des faits manquant");
            return;
        }
        Path dir = Path.of(args[0]);
        Path out = Path.of("sorties");
        double cp = CP_DEFAUT;
        String prefix = null;

        for (int i = 1; i < args.length; i++) {
            if ("--cp".equals(args[i]) && i + 1 < args.length) {
                try {
                    cp = Double.parseDouble(args[++i]);
                } catch (NumberFormatException e) {
                    usage("valeur de --cp invalide : " + args[i]);
                    return;
                }
            } else if ("--prefix".equals(args[i]) && i + 1 < args.length) {
                prefix = args[++i];
            } else if ("--out".equals(args[i]) && i + 1 < args.length) {
                out = Path.of(args[++i]);
            } else {
                usage("argument inconnu : " + args[i]);
                return;
            }
        }
        if (cp < 0.0) {
            usage("--cp doit être positif ou nul");
            return;
        }

      Facts facts;
      String source;
      try {
        if (prefix != null || containsCanonicalFacts(dir)) {
          // chemin 2 : jeu de données au format canonique (CSV)
          if (prefix == null) {
            prefix = Facts.discoverPrefix(dir);
          }
          facts = Facts.load(dir, prefix);
          source = "jeu de données canonique (CSV)";
        } else {
          // chemin 1 : notre extracteur du TP 1 sur les sources Java
          facts = Tp1FactsAdapter.from(dir);
          prefix = dir.toAbsolutePath().getFileName().toString();
          source = "extracteur du TP 1 (JDT)";
        }
      } catch (IOException e) {
        ERR.println("Lecture des faits impossible : " + e.getMessage());
        System.exit(2);
        return;
      }



      OUT.println("Source des faits    : " + source);

        OUT.println("Dossier des faits   : " + dir.toAbsolutePath());
        OUT.println("Jeu de données      : " + prefix);
        OUT.printf(Locale.ROOT, "Classes             : %d%n",
                facts.classes().size());
        OUT.printf(Locale.ROOT, "Méthodes            : %d%n",
                facts.methodOwners().size());
        OUT.printf(Locale.ROOT, "Arcs du graphe      : %d%n",
                facts.calls().size());
        OUT.printf(Locale.ROOT, "Seuil CP retenu     : %.4f%n", cp);

        CouplingMatrix coupling = new CouplingMatrix(facts);
        OUT.println();
        OUT.println("== A1 : couplage entre classes ==");
        OUT.printf(Locale.ROOT, "Arcs internes écartés : %d%n", coupling.internalArcs());
        OUT.printf(Locale.ROOT, "|E_inter|             : %d%n", coupling.interArcs());
        OUT.printf(Locale.ROOT, "Couples non nuls      : %d%n", coupling.nonZeroPairs().size());
        OUT.printf(Locale.ROOT, "Somme des couplages   : %.4f%n", coupling.sum());
        for (CouplingMatrix.Pair p : coupling.nonZeroPairs()) {
          OUT.printf(Locale.ROOT, "  %-16s - %-16s %d/%d = %.4f%n",
                  simple(p.a()), simple(p.b()), p.arcs(), coupling.interArcs(), p.coupling());
        }
        WeightedGraph graph = WeightedGraph.from(coupling);
        OUT.println();
        OUT.println("== A2 : graphe de couplage pondéré ==");
        OUT.printf(Locale.ROOT, "Nœuds : %d   Arêtes : %d%n", graph.nodes().size(), graph.edges().size());
        OUT.println("Cinq couples les plus couplés :");
        for (WeightedGraph.Edge e : graph.top(5)) {
          OUT.printf(Locale.ROOT, "  %-16s - %-16s %.4f%n", simple(e.a()), simple(e.b()), e.weight());
        }
        OUT.println("Classes isolées : "
                + graph.isolated().stream().map(Main::simple).toList());
        try {
          GraphExporter.writeCsv(graph, out.resolve("graphe.csv"));
          GraphExporter.writeDot(graph, out.resolve("graphe.dot"));
          OUT.println("Exports : " + out.resolve("graphe.csv") + ", " + out.resolve("graphe.dot"));
        } catch (IOException e) {
          ERR.println("Écriture des exports impossible : " + e.getMessage());
          System.exit(2);
        }
        AgglomerativeClustering clustering =
                new AgglomerativeClustering(coupling.classes(), coupling::coupling);
        OUT.println();
        OUT.println("== B1 : regroupement hiérarchique (lien moyen) ==");
        for (Merge m : clustering.merges()) {
          OUT.printf(Locale.ROOT, "%2d  %.4f  %s  +  %s%n", m.step(), m.similarity(),
                  group(m.a()), group(m.b()));
        }

        OUT.println();
        OUT.println("== B2 : dendrogramme ==");
        DendrogramExporter.indentedTree(clustering.root(), clustering.merges()).forEach(OUT::println);
        try {
          DendrogramExporter.write(DendrogramExporter.indentedTree(clustering.root(), clustering.merges()),
                  out.resolve("dendrogramme.txt"));
          DendrogramExporter.write(DendrogramExporter.mergeList(clustering.merges()),
                  out.resolve("fusions.csv"));
          DendrogramExporter.write(DendrogramExporter.dot(clustering.root(), clustering.merges()),
                  out.resolve("dendrogramme.dot"));
          OUT.println("Exports : dendrogramme.txt, fusions.csv, dendrogramme.dot dans " + out);
        } catch (IOException e) {
          ERR.println("Écriture des exports impossible : " + e.getMessage());
          System.exit(2);
        }

        CutResult cut = ModuleCutter.cut(clustering.root(), coupling::coupling, cp);
        List<String> report = new ArrayList<>();
        report.add(String.format(Locale.ROOT, "== B3 : modules pour CP = %.4f ==", cp));
        int k = 1;
        for (ClassModule m : cut.modules()) {
          report.add(String.format(Locale.ROOT, "Module %d : %d classe(s), C(G) = %s  %s",
                  k++, m.size(),
                  m.size() < 2 ? "sans objet" : String.format(Locale.ROOT, "%.4f", m.internalCoupling()),
                  group(m.node())));
        }
        report.add(String.format(Locale.ROOT, "Modules : %d   plafond M/2 = %d",
                cut.modules().size(), cut.maxModules()));
        report.add(cut.respectsMHalf()
                ? "Contrainte M/2 : respectée"
                : "CONTRAINTE M/2 VIOLÉE : coupe rejetée (" + cut.modules().size() + " > "
                + cut.maxModules() + "). Avec ce seuil, les contraintes sont incompatibles.");
        OUT.println();
        report.forEach(OUT::println);
        try {
          DendrogramExporter.write(report,
                  out.resolve(String.format(Locale.ROOT, "modules-cp%.3f.txt", cp)));
        } catch (IOException e) {
          ERR.println("Écriture des exports impossible : " + e.getMessage());
          System.exit(2);
        }
    }

    private static String group(Cluster c) {
      return c.members().stream().map(Main::simple).toList()
              .toString().replace('[', '{').replace(']', '}');
    }

    private static String simple(String qualifiedName) {
      return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }


    /**
     * Affiche un message d'erreur, le mode d'emploi, et sort avec le code 1.
     *
     * @param message cause de l'échec, en une ligne
     */
    private static void usage(String message) {
        ERR.println("Erreur : " + message);
        ERR.println();
        ERR.println("Usage : java -jar hai913i-tp2.jar <dossier-des-faits>"
                + " [--cp <valeur>] [--prefix <préfixe>] [--out <dossier>]");
        ERR.println();
        ERR.println("  <dossier-des-faits>  dossier contenant les trois fichiers");
        ERR.println("                       <préfixe>-classes.csv,");
        ERR.println("                       <préfixe>-methods.csv et");
        ERR.println("                       <préfixe>-calls.csv");
        ERR.println("  --cp <valeur>        seuil de couplage interne moyen"
                + " (défaut : " + CP_DEFAUT + ")");
        ERR.println("  --prefix <préfixe>   force le préfixe ; par défaut il est");
        ERR.println("                       déduit du dossier");
        ERR.println("  --out <dossier>      dossier des exports (défaut : sorties)");
        System.exit(1);
    }

    /** Vrai si le dossier contient un jeu au format canonique (*-classes.csv). */
    private static boolean containsCanonicalFacts(Path dir) throws IOException {
      if (!Files.isDirectory(dir)) {
        throw new IOException("dossier introuvable : " + dir.toAbsolutePath());
      }
      try (var entries = Files.list(dir)) {
        return entries.anyMatch(p -> p.getFileName().toString().endsWith("-classes.csv"));
      }
    }
}
