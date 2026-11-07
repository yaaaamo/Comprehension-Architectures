package hai913i.tp2.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * B1 : clustering hiérarchique ascendant, lien moyen, départage déterministe.
 * Ne reçoit que des identifiants de classes et une fonction de similarité (annexe 1, § 1).
 */
public final class AgglomerativeClustering {

  /** Tolérance de comparaison des similarités (sujet § 5.4). */
  public static final double EPS = 1e-9;

  private final List<Merge> merges = new ArrayList<>();
  private final Cluster root;

  /**
   * @param classes    les M classes ; triées ici par nom qualifié, ce qui fixe les ids 0..M-1
   * @param similarity C(a, b) entre deux classes
   */
  public AgglomerativeClustering(List<String> classes, Similarity similarity) {
    if (classes.isEmpty()) throw new IllegalArgumentException("aucune classe à regrouper");

    // groupes actifs, indexés par id : un TreeMap les parcourt toujours dans l'ordre croissant
    TreeMap<Integer, Cluster> active = new TreeMap<>();
    List<String> sorted = classes.stream().sorted().toList();
    for (int i = 0; i < sorted.size(); i++) {
      active.put(i, new Cluster(i, sorted.get(i)));
    }
    int nextId = sorted.size();

    for (int step = 1; active.size() > 1; step++) {
      // 1. Chercher le meilleur couple. Parcours trié (i < j croissants) + comparaison avec tolérance :
      //    on ne remplace le meilleur que si s le dépasse STRICTEMENT de plus de EPS,
      //    donc à égalité on garde le premier rencontré = le plus petit couple (règle de départage).
      Cluster bestA = null, bestB = null;
      double best = Double.NEGATIVE_INFINITY;
      List<Cluster> groups = new ArrayList<>(active.values());
      for (int x = 0; x < groups.size(); x++) {
        for (int y = x + 1; y < groups.size(); y++) {
          double s = averageLink(groups.get(x), groups.get(y), similarity);
          if (s > best + EPS) {
            best = s;
            bestA = groups.get(x);
            bestB = groups.get(y);
          }
        }
      }

      // 2. Fusionner : le nouveau groupe reçoit l'id suivant, ses fils quittent les actifs
      Cluster merged = new Cluster(nextId++, bestA, bestB, best);
      active.remove(bestA.id());
      active.remove(bestB.id());
      active.put(merged.id(), merged);
      merges.add(new Merge(step, bestA, bestB, best, merged));
    }
    this.root = active.firstEntry().getValue();
  }

  /**
   * Lien moyen, recalculé à partir des couplages entre classes (jamais des anciennes similarités) :
   * S(G1,G2) = somme des C(a,b) / (|G1| * |G2|), couples nuls compris.
   */
  public static double averageLink(Cluster g1, Cluster g2, Similarity c) {
    double sum = 0.0;
    for (String a : g1.members()) {
      for (String b : g2.members()) {
        sum += c.between(a, b);
      }
    }
    return sum / (g1.size() * g2.size());
  }

  public List<Merge> merges() { return List.copyOf(merges); }
  public Cluster root()       { return root; }
}