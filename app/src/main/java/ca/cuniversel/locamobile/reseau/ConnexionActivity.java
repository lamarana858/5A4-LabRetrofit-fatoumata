package ca.cuniversel.locamobile.reseau;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import ca.cuniversel.locamobile.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConnexionActivity extends AppCompatActivity {

    private EditText champCourriel;
    private EditText champMotDePasse;
    private Button boutonSeConnecter;
    private TextView texteResultat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connexion);

        champCourriel = findViewById(R.id.champCourriel);
        champMotDePasse = findViewById(R.id.champMotDePasse);
        boutonSeConnecter = findViewById(R.id.boutonSeConnecter);
        texteResultat = findViewById(R.id.texteResultat);

        boutonSeConnecter.setOnClickListener(v -> tenterConnexion());
    }

    private void tenterConnexion() {
        String courriel = champCourriel.getText().toString().trim();
        String motDePasse = champMotDePasse.getText().toString();

        if (courriel.isEmpty()) {
            champCourriel.setError("Le courriel est requis");
            champCourriel.requestFocus();
            return;
        }

        if (motDePasse.isEmpty()) {
            champMotDePasse.setError("Le mot de passe est requis");
            champMotDePasse.requestFocus();
            return;
        }

        texteResultat.setText("Connexion en cours...");
        ConnexionRequete requete = new ConnexionRequete(courriel, motDePasse);

        ClientApi.get().connecter(requete).enqueue(new Callback<TokenReponse>() {
            @Override
            public void onResponse(Call<TokenReponse> call, Response<TokenReponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    afficherErreurConnexion(response);
                    return;
                }

                TokenReponse data = response.body();
                Session.enregistrer(ConnexionActivity.this, data.getToken(), data.getNom());

                // Pour l'expérience (consigne 9), remplacez temporairement "Bearer " par "Bearer"
                String entete = "Bearer " + data.getToken();
                chargerProfil(entete);
            }

            @Override
            public void onFailure(Call<TokenReponse> call, Throwable t) {
                texteResultat.setText("Serveur injoignable : " + t.getMessage());
            }
        });
    }

    private void chargerProfil(String entete) {
        ClientApi.get().monProfil(entete).enqueue(new Callback<Profil>() {
            @Override
            public void onResponse(Call<Profil> call, Response<Profil> response) {
                int code = response.code();
                if (!response.isSuccessful() || response.body() == null) {
                    String messageErreur = extraireMessageErreur(response);
                    texteResultat.setText("Code HTTP : " + code + "\n" + messageErreur);
                    return;
                }

                Profil profil = response.body();
                String texte = "Code HTTP : " + code
                        + "\nNom : " + profil.getNom()
                        + "\nRôle : " + profil.getRole()
                        + "\nCourriel : " + profil.getCourriel();
                texteResultat.setText(texte);
            }

            @Override
            public void onFailure(Call<Profil> call, Throwable t) {
                texteResultat.setText("Serveur injoignable : " + t.getMessage());
            }
        });
    }

    private void afficherErreurConnexion(Response<?> response) {
        String message = extraireMessageErreur(response);
        texteResultat.setText("Erreur " + response.code() + " : " + message);
    }

    private String extraireMessageErreur(Response<?> response) {
        if (response.errorBody() == null) {
            return "Erreur inconnue";
        }
        try {
            String texte = response.errorBody().string();
            JsonObject json = new Gson().fromJson(texte, JsonObject.class);
            if (json != null && json.has("message") && !json.get("message").isJsonNull()) {
                return json.get("message").getAsString();
            }
            return texte;
        } catch (IOException e) {
            return "Impossible de lire la réponse d'erreur";
        }
    }
}