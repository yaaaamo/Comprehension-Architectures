package hai913i.tp2.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un nœud du dendrogramme : une feuille (une classe) ou un groupe créé par une fusion.
 * id : 0..M-1 pour les classes (ordre alphabétique), puis M, M+1... pour les fusions.
 */
public final class Cluster {

  private final int id;
  private final Cluster left;          // null pour une feuille
  private final Cluster right;         // null pour une feuille
  private final double similarity;     // similarité de la fusion qui l'a créé (0 pour une feuille)
  private final List<String> members;  // classes du groupe, triées

  /** Feuille. */
  public Cluster(int id, String className) {
    this(id, null, null, 0.0, List.of(className));
  }

  /** Nœud interne : union disjointe des membres des deux fils. */
  public Cluster(int id, Cluster left, Cluster right, double similarity) {
    this(id, left, right, similarity, union(left.members, right.members));
  }

  private Cluster(int id, Cluster left, Cluster right, double similarity, List<String> members) {
    this.id = id;
    this.left = left;
    this.right = right;
    this.similarity = similarity;
    this.members = members;
  }

  private static List<String> union(List<String> a, List<String> b) {
    List<String> all = new ArrayList<>(a);
    all.addAll(b);
    Collections.sort(all);
    return List.copyOf(all);
  }

  public int id()              { return id; }
  public Cluster left()        { return left; }
  public Cluster right()       { return right; }
  public double similarity()   { return similarity; }
  public List<String> members(){ return members; }
  public boolean isLeaf()      { return left == null; }
  public int size()            { return members.size(); }
}