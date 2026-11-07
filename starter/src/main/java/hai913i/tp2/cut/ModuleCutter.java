package hai913i.tp2.cut;

import java.util.ArrayList;
import java.util.List;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Similarity;

/**
 * B3 : coupe du dendrogramme. Parcours en profondeur depuis la racine :
 *  - feuille           -> module d'une classe (aucun couple, la contrainte CP ne s'applique pas) ;
 *  - C(G) > CP         -> le nœud devient un module, on ne descend plus ;
 *  - sinon             -> on descend dans les deux fils.
 * La contrainte M/2 est vérifiée APRÈS, et une coupe qui la viole est signalée, jamais « réparée ».
 */
public final class ModuleCutter {

  private ModuleCutter() {}

  public static CutResult cut(Cluster root, Similarity c, double cp) {
    List<ClassModule> modules = new ArrayList<>();
    descend(root, c, cp, modules);
    return new CutResult(cp, root.size(), List.copyOf(modules));
  }

  private static void descend(Cluster node, Similarity c, double cp, List<ClassModule> modules) {
    if (node.isLeaf()) {
      modules.add(new ClassModule(node, 0.0));
      return;
    }
    double cg = internalCoupling(node.members(), c);
    if (cg > cp) {                                   // strictement supérieur, jamais >=
      modules.add(new ClassModule(node, cg));
    } else {
      descend(node.left(), c, cp, modules);
      descend(node.right(), c, cp, modules);
    }
  }

  /**
   * Couplage interne moyen : moyenne de C(A,B) sur TOUS les couples de classes du groupe,
   * soit n(n-1)/2 couples. Ce n'est PAS la similarité de la fusion qui a créé le nœud.
   */
  public static double internalCoupling(List<String> members, Similarity c) {
    int n = members.size();
    if (n < 2) return 0.0;
    double sum = 0.0;
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        sum += c.between(members.get(i), members.get(j));
      }
    }
    return sum / (n * (n - 1) / 2.0);
  }
}
