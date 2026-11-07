package hai913i.tp2.clustering;

/** Une fonction qui, à deux classes, associe un nombre. Le regroupement ne connaît rien d'autre. */
@FunctionalInterface
public interface Similarity {
  double between(String a, String b);
}