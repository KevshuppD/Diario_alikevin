package calendario.kevshupp.diariokevinali;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private String theme;

    public static SettingsFragment newInstance(String theme) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString("theme", theme);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            theme = getArguments().getString("theme");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() instanceof MainActivity) {
            theme = ((MainActivity) getActivity()).getCurrentTheme();
        }

        View v = inflater.inflate(R.layout.dialog_settings, container, false);

        if ("Pixel Oscuro".equals(theme)) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvSettingsTitle)).setTextColor(Color.WHITE);
            ((TextView) v.findViewById(R.id.tvLabelTheme)).setTextColor(Color.WHITE);
            ((TextView) v.findViewById(R.id.tvLabelColor)).setTextColor(Color.WHITE);
            ((TextView) v.findViewById(R.id.tvAppVersion)).setTextColor(Color.LTGRAY);
            android.widget.RadioButton rb1 = v.findViewById(R.id.rbPixelClaro), rb2 = v.findViewById(R.id.rbPixelOscuro);
            rb1.setTextColor(Color.WHITE); rb2.setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnCheckUpdates)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnLogout)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnDismissSettings)).setTextColor(Color.WHITE);
        } else {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel);
        }

        ((TextView)v.findViewById(R.id.tvAppVersion)).setText("Versión actual: " + BuildConfig.VERSION_NAME);
        RadioGroup rg = v.findViewById(R.id.rgThemes);
        rg.check("Pixel Claro".equals(theme) ? R.id.rbPixelClaro : R.id.rbPixelOscuro);
        
        rg.setOnCheckedChangeListener((g, id) -> { 
            String t = id == R.id.rbPixelClaro ? "Pixel Claro" : "Pixel Oscuro"; 
            if (!t.equals(theme)) {
                if (getActivity() instanceof MainActivity) {
                    MainActivity main = (MainActivity) getActivity();
                    main.applyTheme(t);
                    requireActivity().getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE).edit().putString("theme", t).apply();
                    theme = t;
                    if (getArguments() != null) getArguments().putString("theme", t);
                    
                    // En lugar de detach/attach, recreamos el fragmento completamente
                    // para asegurar que todos los estilos se vuelvan a aplicar
                    getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, SettingsFragment.newInstance(t))
                        .commit();
                }
            }
        });

        setupColorButtons(v);

        v.findViewById(R.id.btnCheckUpdates).setOnClickListener(v1 -> {
             if (getActivity() instanceof MainActivity) {
                 MainActivity main = (MainActivity) getActivity();
                 main.getUpdateManager().checkForUpdates(new UpdateManager.UpdateCallback() {
                    @Override public void onUpdateAvailable(String url) { main.showUpdateDialog(url); }
                    @Override public void onNoUpdate() { Toast.makeText(getContext(), "La app está actualizada", Toast.LENGTH_SHORT).show(); }
                    @Override public void onDownloadProgress(int p) { main.runOnUiThread(() -> main.getDownloadProgressBar().setProgress(p)); }
                    @Override public void onDownloadComplete() { main.runOnUiThread(() -> { main.getDownloadProgressContainer().setVisibility(View.GONE); main.getUpdateManager().installApk(); }); }
                });
             }
        });

        v.findViewById(R.id.btnLogout).setOnClickListener(v1 -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).logout();
        });

        v.findViewById(R.id.btnDismissSettings).setOnClickListener(v1 -> requireActivity().onBackPressed());

        return v;
    }

    private void setupColorButtons(View v) {
        if ("Pixel Oscuro".equals(theme)) {
            v.findViewById(R.id.tvLabelColor).setVisibility(View.GONE);
            v.findViewById(R.id.colorSelectionLayout).setVisibility(View.GONE);
            return;
        }

        v.findViewById(R.id.colorPurple).setOnClickListener(v1 -> saveColor("#4A148C"));
        v.findViewById(R.id.colorBlue).setOnClickListener(v1 -> saveColor("#0D47A1"));
        v.findViewById(R.id.colorGreen).setOnClickListener(v1 -> saveColor("#1B5E20"));
        v.findViewById(R.id.colorPink).setOnClickListener(v1 -> saveColor("#C2185B"));
        v.findViewById(R.id.colorOrange).setOnClickListener(v1 -> saveColor("#E65100"));
        v.findViewById(R.id.colorCyan).setOnClickListener(v1 -> saveColor("#006064"));
        v.findViewById(R.id.colorBrown).setOnClickListener(v1 -> saveColor("#3E2723"));
    }

    private void saveColor(String hex) {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.applyTheme("Pixel Claro", hex);
            requireActivity().getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE).edit().putString("lightColor", hex).apply();
            Toast.makeText(getContext(), "Color aplicado", Toast.LENGTH_SHORT).show();
        }
    }
}
