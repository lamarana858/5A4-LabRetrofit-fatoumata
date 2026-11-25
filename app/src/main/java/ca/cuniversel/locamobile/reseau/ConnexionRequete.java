package  ca.cuniversel.locamobile.reseau;

public class ConnexionRequete {
    private String courriel;
    private String motDePasse;

    public ConnexionRequete(String courriel, String motDePasse) {
        this.courriel = courriel;
        this.motDePasse = motDePasse;
    }

    public String getCourriel() {
        return courriel;
    }

    public String getMotDePasse() {
        return motDePasse;
    }
}