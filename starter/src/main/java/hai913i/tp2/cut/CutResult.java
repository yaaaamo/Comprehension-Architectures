package hai913i.tp2.cut;

import java.util.List;

/** Résultat d'une coupe : les modules, et le verdict sur la contrainte M/2. */
public record CutResult(double cp, int classCount, List<ClassModule> modules) {
  /** Plafond M/2 en division entière (M = 15 => 7). */
  public int maxModules()        { return classCount / 2; }
  public boolean respectsMHalf() { return modules.size() <= maxModules(); }
}