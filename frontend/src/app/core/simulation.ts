export interface LigneAmortissement {
  n: number;
  mensualite: number;
  interets: number;
  capital: number;
  restant: number;
}

/** Mensualité d'un prêt à annuités constantes. */
export function mensualite(capital: number, tauxAnnuelPct: number, mois: number): number {
  if (mois <= 0) return 0;
  const i = tauxAnnuelPct / 100 / 12;
  return i === 0 ? capital / mois : (capital * i) / (1 - Math.pow(1 + i, -mois));
}

export function tableauAmortissement(capital: number, tauxAnnuelPct: number, mois: number): LigneAmortissement[] {
  const m = mensualite(capital, tauxAnnuelPct, mois);
  const i = tauxAnnuelPct / 100 / 12;
  let restant = capital;
  const lignes: LigneAmortissement[] = [];
  for (let n = 1; n <= mois; n++) {
    const interets = restant * i;
    const cap = m - interets;
    restant = Math.max(0, restant - cap);
    lignes.push({ n, mensualite: m, interets, capital: cap, restant });
  }
  return lignes;
}
