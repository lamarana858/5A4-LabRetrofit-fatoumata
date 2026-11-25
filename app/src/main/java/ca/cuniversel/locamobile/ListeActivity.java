package ca.cuniversel.locamobile;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

import ca.cuniversel.locamobile.reseau.ClientApi;
import ca.cuniversel.locamobile.reseau.ConnexionActivity;
import ca.cuniversel.locamobile.reseau.Logement;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListeActivity extends AppCompatActivity {

    private static final String PERMISSION_RESEAU_LOCAL =
            "android.permission.ACCESS_LOCAL_NETWORK";

    private LinearLayout conteneurLogements;
    private TextView messageErreur;
    private EditText champVille;
    private Button boutonChercher;
    private Button boutonConnexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liste);

        conteneurLogements = findViewById(R.id.conteneurLogements);
        messageErreur = findViewById(R.id.messageErreur);
        champVille = findViewById(R.id.champVille);
        boutonChercher = findViewById(R.id.boutonChercher);
        boutonConnexion = findViewById(R.id.boutonConnexion);

        boutonChercher.setOnClickListener(v -> lancerRechercheOuListe());
        boutonConnexion.setOnClickListener(v -> {
            Intent intent = new Intent(ListeActivity.this, ConnexionActivity.class);
            startActivity(intent);
        });

        // Android 17 : demander l'accès au réseau local
        if (!reseauLocalAutorise()) {
            requestPermissions(new String[]{PERMISSION_RESEAU_LOCAL}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        lancerRechercheOuListe();
    }

    private boolean reseauLocalAutorise() {
        return Build.VERSION.SDK_INT < 37
                || checkSelfPermission(PERMISSION_RESEAU_LOCAL)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void lancerRechercheOuListe() {
        if (!reseauLocalAutorise()) {
            afficherErreur("Autorisez l'accès aux appareils à proximité.");
            return;
        }

        String ville = champVille.getText().toString().trim();
        if (ville.isEmpty()) {
            chargerTousLesLogements();
        } else {
            rechercherParVille(ville);
        }
    }

    private void chargerTousLesLogements() {
        Log.d("LocaMobile", "1");

        ClientApi.get().listerLogements().enqueue(new Callback<List<Logement>>() {
            @Override
            public void onResponse(Call<List<Logement>> call, Response<List<Logement>> response) {
                Log.d("LocaMobile", "3");
                Log.d("LocaMobile", "code " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("LocaMobile", "logements : " + response.body().size());
                    afficher(response.body(), "");
                } else {
                    afficherErreur("Erreur HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Logement>> call, Throwable t) {
                Log.e("LocaMobile", "Erreur réseau", t);
                afficherErreur("Serveur injoignable : " + t.getMessage());
            }
        });

        Log.d("LocaMobile", "2");
    }

    private void rechercherParVille(String ville) {
        ClientApi.get().chercherParVille(ville).enqueue(new Callback<List<Logement>>() {
            @Override
            public void onResponse(Call<List<Logement>> call, Response<List<Logement>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    afficher(response.body(), ville);
                } else {
                    afficherErreur("Erreur HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Logement>> call, Throwable t) {
                Log.e("LocaMobile", "Erreur recherche", t);
                afficherErreur("Serveur injoignable : " + t.getMessage());
            }
        });
    }

    private void afficher(List<Logement> logements, String villeRecherchee) {
        conteneurLogements.removeAllViews();

        if (logements.isEmpty()) {
            if (villeRecherchee.isEmpty()) {
                messageErreur.setText("Aucun logement disponible.");
            } else {
                messageErreur.setText("Aucun logement à " + villeRecherchee + ".");
            }
            messageErreur.setVisibility(View.VISIBLE);
            return;
        }

        messageErreur.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Logement l : logements) {
            View carte = inflater.inflate(R.layout.item_logement, conteneurLogements, false);

            TextView carteTitre = carte.findViewById(R.id.carteTitre);
            TextView carteVille = carte.findViewById(R.id.carteVille);
            TextView cartePrix = carte.findViewById(R.id.cartePrix);

            carteTitre.setText(l.titre);
            carteVille.setText(l.ville);
            cartePrix.setText(String.format(Locale.CANADA_FRENCH, "%.2f $ / nuit", l.prixParNuit));

            carte.setOnClickListener(v -> {
                Intent intent = new Intent(ListeActivity.this, DetailActivity.class);
                intent.putExtra(DetailActivity.CLE_ID, l.id);
                startActivity(intent);
            });

            conteneurLogements.addView(carte);
        }
    }

    private void afficherErreur(String texte) {
        conteneurLogements.removeAllViews();
        messageErreur.setText(texte);
        messageErreur.setVisibility(View.VISIBLE);
    }
}