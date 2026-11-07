package hai913i.tp2.cut;

import java.util.List;

import hai913i.tp2.clustering.Cluster;

/** Un module retenu : un nœud du dendrogramme (une branche) et son couplage interne moyen C(G). */
public record ClassModule(Cluster node, double internalCoupling) {
  public List<String> classes() { return node.members(); }
  public int size()             { return node.size(); }
}