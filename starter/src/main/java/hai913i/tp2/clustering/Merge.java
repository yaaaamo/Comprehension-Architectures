package hai913i.tp2.clustering;

/** Une fusion : étape (1..M-1), les deux groupes réunis (a.id < b.id), la similarité, le groupe créé. */
public record Merge(int step, Cluster a, Cluster b, double similarity, Cluster result) {}