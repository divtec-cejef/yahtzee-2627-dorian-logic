import java.util.Scanner;

/**
 * Petit jeu de dés inspiré du Yahtzee.
 * <p>
 * Le joueur lance {MAXDICE} dés, puis peut relancer ceux de son choix
 * jusqu'à {MAXREROLL} fois. Après chaque lancer, le programme affiche
 * les combinaisons présentes (paire, brelan, suite, etc.) et leur score.
 */
public class Main {

    /** Nombre de dés utilisés dans une partie. */
    static final int MAXDICE = 5;

    /** Nombre maximum de relances autorisées après le premier lancer. */
    static final int MAXREROLL = 2;

    /** Nombre de faces d'un dé. */
    static final int SIDECOUNT = 6;

    /** Lecteur partagé pour les saisies clavier. */
    static Scanner reader = new Scanner(System.in);

    /**
     * Les combinaisons possibles, dans l'ordre d'affichage.
     * L'enum ne fait que nommer les combinaisons : la détection se fait avec
     * {isPresent(Combination, int[], int[])} et le score avec {score(Combination, int[])}.
     */
    enum Combination {
        PAIRE("Paire"),
        DOUBLE_PAIRE("Double paire"),
        BRELAN("Brelan"),
        CARRE("Carré"),
        FULL_HOUSE("Full house"),
        PETITE_SUITE("Petite suite"),
        GRANDE_SUITE("Grande suite"),
        YAHTZEE("Yahtzee");

        final String label;

        Combination(String label) {
            this.label = label;
        }
    }

    /**
     * Demande au joueur quels dés il veut relancer.
     * Le joueur tape les numéros des dés (de 1 à {MAXDICE}), avec ou
     * sans espaces, par exemple {135} ou {1 3 5}. Une ligne vide
     * ou {0} signifie qu'il garde tous ses dés. Tant que la saisie est
     * invalide, la question est reposée. Un dé cité plusieurs fois n'est
     * relancé qu'une seule fois.
     *
     * @return les indices (commençant à 0) des dés à relancer, sans doublon ;
     *         un tableau vide si le joueur ne veut rien relancer
     */
    static int[] ask() {
        while (true) {
            System.out.print("Quel(s) dé(s) aimeriez-vous relancer ? (ex : 135, Entrée pour garder) ");

            // Plus rien à lire (fin de l'entrée) : on considère que le joueur garde ses dés
            if (!reader.hasNextLine()) {
                return new int[0];
            }
            String input = reader.nextLine().replace(" ", "");

            if (input.isEmpty() || input.equals("0")) {
                return new int[0];
            }

            // selected[i] vaut true si le dé i doit être relancé.
            // Passer par ce tableau élimine automatiquement les doublons ("113" -> dés 1 et 3).
            boolean[] selected = new boolean[MAXDICE];
            int selectedCount = 0;
            boolean valid = true;

            for (int i = 0; i < input.length(); i++) {
                // getNumericValue renvoie -1 pour un symbole et 10+ pour une lettre,
                // donc tout ce qui n'est pas un numéro de dé tombe hors de [1, MAXDICE]
                int number = Character.getNumericValue(input.charAt(i));
                if (number < 1 || number > MAXDICE) {
                    valid = false;
                    break;
                }
                if (!selected[number - 1]) {
                    selected[number - 1] = true;
                    selectedCount++;
                }
            }

            if (!valid) {
                System.out.println("Saisie invalide : utilisez uniquement des chiffres de 1 à " + MAXDICE + ".");
                continue;
            }

            // Conversion du tableau de booléens en liste d'indices
            int[] indices = new int[selectedCount];
            int next = 0;
            for (int i = 0; i < MAXDICE; i++) {
                if (selected[i]) {
                    indices[next] = i;
                    next++;
                }
            }
            return indices;
        }
    }

    /**
     * Lance un dé.
     *
     * @return une valeur aléatoire entre 1 et {SIDECOUNT} inclus
     */
    static int roll() {
        return (int) (Math.random() * SIDECOUNT) + 1;
    }

    /**
     * Crée un ensemble de dés pas encore lancés.
     *
     * @param count le nombre de dés à créer
     * @return un tableau de {count} dés (valeurs à 0 tant qu'ils n'ont pas été lancés)
     */
    static int[] makeDice(int count) {
        return new int[count];
    }

    /**
     * Affiche la valeur de chaque dé, précédée de son numéro,
     * pour que le joueur sache quel numéro taper pour le relancer.
     *
     * @param dice les dés à afficher
     */
    static void showDice(int[] dice) {
        for (int i = 0; i < dice.length; i++) {
            System.out.println("Dé " + (i + 1) + " : " + dice[i]);
        }
    }

    /**
     * Lance tous les dés.
     *
     * @param dice les dés à lancer (modifiés sur place)
     */
    static void rollAll(int[] dice) {
        for (int i = 0; i < dice.length; i++) {
            dice[i] = roll();
        }
    }

    /**
     * Relance uniquement les dés choisis. Les indices hors limites sont ignorés.
     *
     * @param dice    les dés (modifiés sur place)
     * @param indices les indices (commençant à 0) des dés à relancer
     */
    static void rerollChosen(int[] dice, int[] indices) {
        for (int i = 0; i < indices.length; i++) {
            int index = indices[i];
            if (index >= 0 && index < dice.length) {
                dice[index] = roll();
            }
        }
    }

    /**
     * Analyse les dés en deux tableaux de comptage.
     * {count[face]} : nombre de dés montrant cette face
     * (l'indice 0 n'est pas utilisé, les faces vont de 1 à {SIDECOUNT}) ;</li>
     * {tally[n]} : nombre de faces qui apparaissent exactement {n} fois.</li>
     *
     * Exemple : pour les dés {3 3 5 5 5}, {count[3] = 2}, {count[5] = 3},
     * et {tally[2] = 1}, {tally[3] = 1}, {tally[0] = 4}.
     *
     * @param dice les dés à analyser
     * @return un tableau dont l'élément [0] est {count} et l'élément [1] est {tally}
     */
    static int[][] analyse(int[] dice) {
        int[] count = new int[SIDECOUNT + 1];
        // Une face peut apparaître de 0 à dice.length fois, d'où la taille dice.length + 1
        int[] tally = new int[dice.length + 1];

        // Avant de compter, aucune face n'est sortie : elles sont toutes dans le groupe "0 occurrence"
        tally[0] = SIDECOUNT;

        for (int die : dice) {
            // La face passe du groupe "n occurrences" au groupe "n + 1 occurrences"
            tally[count[die]]--;
            count[die]++;
            tally[count[die]]++;
        }

        return new int[][] { count, tally };
    }

    /**
     * Compte le nombre de faces qui apparaissent au moins {n} fois.
     *
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @param n     le nombre minimum d'occurrences
     * @return le nombre de faces présentes au moins {n} fois
     */
    static int facesWithAtLeast(int[] tally, int n) {
        int total = 0;
        // tally[i] = faces présentes exactement i fois : on additionne tous les groupes à partir de n
        for (int i = n; i < tally.length; i++) {
            total += tally[i];
        }
        return total;
    }

    /**
     * Trouve la face qui apparaît au moins {n} fois.
     * Avec 5 dés, pour n >= 3, il ne peut y en avoir qu'une (3 + 3 > 5).
     *
     * @param count le tableau {count} renvoyé par {analyse(int[])}
     * @param n     le nombre minimum d'occurrences
     * @return la face présente au moins {n} fois, ou 0 s'il n'y en a pas
     */
    static int faceRepeated(int[] count, int n) {
        for (int face = 1; face <= SIDECOUNT; face++) {
            if (count[face] >= n) {
                return face;
            }
        }
        return 0;
    }

    /**
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @return {true} si au moins une face apparaît 2 fois ou plus
     */
    static boolean isPair(int[] tally) {
        return facesWithAtLeast(tally, 2) >= 1;
    }

    /**
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @return {true} si au moins deux faces différentes apparaissent 2 fois ou plus
     */
    static boolean isTwoPair(int[] tally) {
        return facesWithAtLeast(tally, 2) >= 2;
    }

    /**
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @return {true} si au moins une face apparaît 3 fois ou plus
     */
    static boolean isBrelan(int[] tally) {
        return facesWithAtLeast(tally, 3) >= 1;
    }

    /**
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @return {true} si au moins une face apparaît 4 fois ou plus
     */
    static boolean isCarre(int[] tally) {
        return facesWithAtLeast(tally, 4) >= 1;
    }

    /**
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @return {true} si une face apparaît 5 fois (tous les dés identiques)
     */
    static boolean isYahtzee(int[] tally) {
        return facesWithAtLeast(tally, 5) >= 1;
    }

    /**
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @return {true} s'il y a exactement un brelan et une paire (ex : 2 2 6 6 6)
     */
    static boolean isFullHouse(int[] tally) {
        return tally[3] == 1 && tally[2] == 1;
    }

    /**
     * Vérifie la présence d'une grande suite (1-2-3-4-5 ou 2-3-4-5-6).
     *
     * @param count le tableau {count} renvoyé par {analyse(int[])}
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @return {true} si les dés forment une suite de 5 valeurs
     */
    static boolean isGrandeSuite(int[] count, int[] tally) {
        // 5 faces différentes sur 6 possibles : il en manque exactement une.
        // Ce n'est une suite que si la face manquante est une extrémité (1 ou 6).
        return tally[1] == 5 && (count[1] == 0 || count[6] == 0);
    }

    /**
     * Vérifie la présence d'une petite suite (1-2-3-4, 2-3-4-5 ou 3-4-5-6).
     *
     * @param count le tableau {count} renvoyé par {analyse(int[])}
     * @return {true} si les dés contiennent une suite d'au moins 4 valeurs
     */
    static boolean isPetiteSuite(int[] count) {
        // Les trois petites suites possibles contiennent toutes le 3 et le 4 ;
        // il reste à vérifier qu'une des paires 1-2, 2-5 ou 5-6 les complète.
        return count[3] > 0 && count[4] > 0
                && ((count[1] > 0 && count[2] > 0)
                || (count[2] > 0 && count[5] > 0)
                || (count[5] > 0 && count[6] > 0));
    }

    /**
     * Relie chaque combinaison à sa fonction de détection.
     *
     * @param c     la combinaison à vérifier
     * @param count le tableau {count} renvoyé par {analyse(int[])}
     * @param tally le tableau renvoyé par {analyse(int[])}
     * @return {true} si la combinaison est présente dans les dés
     */
    static boolean isPresent(Combination c, int[] count, int[] tally) {
        // Pas de "default" : le compilateur oblige à traiter chaque combinaison
        return switch (c) {
            case PAIRE        -> isPair(tally);
            case DOUBLE_PAIRE -> isTwoPair(tally);
            case BRELAN       -> isBrelan(tally);
            case CARRE        -> isCarre(tally);
            case FULL_HOUSE   -> isFullHouse(tally);
            case PETITE_SUITE -> isPetiteSuite(count);
            case GRANDE_SUITE -> isGrandeSuite(count, tally);
            case YAHTZEE      -> isYahtzee(tally);
        };
    }

    /**
     * Donne le score d'une combinaison, en supposant qu'elle est présente
     * (à vérifier avant avec {isPresent(Combination, int[], int[])}).
     *
     * @param c     la combinaison
     * @param count le tableau {count} renvoyé par {analyse(int[])}
     * @return le nombre de points rapportés par la combinaison
     */
    static int score(Combination c, int[] count) {
        // Pas de "default" : le compilateur oblige à donner un score à chaque combinaison
        return switch (c) {
            case PAIRE        -> 5;
            case DOUBLE_PAIRE -> 10;
            case BRELAN       -> 3 * faceRepeated(count, 3); // somme des 3 dés identiques
            case CARRE        -> 4 * faceRepeated(count, 4); // somme des 4 dés identiques
            case FULL_HOUSE   -> 25;
            case PETITE_SUITE -> 30;
            case GRANDE_SUITE -> 40;
            case YAHTZEE      -> 50;
        };
    }

    /**
     * Affiche toutes les combinaisons avec les points qu'elles rapporteraient :
     * leur score si elles sont présentes dans les dés, 0 sinon.
     *
     * @param dice les dés à analyser
     */
    static void showCombinations(int[] dice) {
        int[][] result = analyse(dice);
        int[] count = result[0];
        int[] tally = result[1];

        System.out.println();
        System.out.println("Combinaisons :");
        for (Combination c : Combination.values()) {
            int points = 0;
            if (isPresent(c, count, tally)) {
                points = score(c, count);
            }
            // %-13s : nom aligné à gauche sur 13 caractères ; %2d : points alignés à droite sur 2 chiffres
            System.out.printf("%-13s: %2d pts%n", c.label, points);
        }
    }

    /**
     * Affiche les dés puis les combinaisons qu'ils forment.
     *
     * @param dice les dés à afficher
     */
    static void showState(int[] dice) {
        showDice(dice);
        showCombinations(dice);
    }

    /**
     * Point d'entrée : un premier lancer, puis jusqu'à {MAXREROLL} relances,
     * et enfin l'affichage du résultat final.
     *
     */
    public static void main(String[] args) {
        int[] dice = makeDice(MAXDICE);

        rollAll(dice);
        System.out.println("=== Premier lancer ===");
        showState(dice);

        for (int reroll = 1; reroll <= MAXREROLL; reroll++) {
            System.out.println();
            System.out.println("=== Relance " + reroll + "/" + MAXREROLL + " ===");

            int[] chosen = ask();
            if (chosen.length == 0) {
                // Le joueur garde ses dés : inutile de proposer les relances restantes
                System.out.println("Vous gardez vos dés.");
                break;
            }

            rerollChosen(dice, chosen);
            showState(dice);
        }

        System.out.println();
        System.out.println("=== Résultat final ===");
        showState(dice);

        reader.close();
    }
}
