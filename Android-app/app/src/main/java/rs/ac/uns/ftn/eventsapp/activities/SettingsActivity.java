package rs.ac.uns.ftn.eventsapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rs.ac.uns.ftn.eventsapp.R;
import rs.ac.uns.ftn.eventsapp.apiCalls.UserAppApi;
import rs.ac.uns.ftn.eventsapp.dtos.UserFBSyncChangeDTO;
import rs.ac.uns.ftn.eventsapp.models.User;
import rs.ac.uns.ftn.eventsapp.utils.AppDataSingleton;

public class SettingsActivity extends AppCompatActivity {

    public static int MIN_DISTANCE = 1;
    public static int MAX_DISTANCE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setTitle(R.string.nav_settings);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private final String TAG = this.getClass().getName();

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            EditTextPreference pref = (EditTextPreference) findPreference("pref_default_distance2");
            pref.getEditText().setFilters(new InputFilter[]{
                    new InputFilter() {
                        @Override
                        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                            try {
                                int enteredValue = Integer.parseInt(dest.toString() + source.toString());
                                if (enteredValue >= MIN_DISTANCE && enteredValue <= MAX_DISTANCE)
                                    return null;
                            } catch (NumberFormatException nfe) {
                            }
                            Toast.makeText(getActivity(), "Value must be between " +
                                            +MIN_DISTANCE + " and " + MAX_DISTANCE + " !",
                                    Toast.LENGTH_SHORT).show();
                            return "";
                        }
                    }
            });

            SwitchPreference syncFbEvents = (SwitchPreference) findPreference("pref_facebook_events");
            syncFbEvents.setChecked(AppDataSingleton.getInstance().getLoggedUser().getSyncFacebookEvents());

            SwitchPreference syncFbProfile = (SwitchPreference) findPreference("pref_sync_user_data_facebook");
            syncFbProfile.setChecked(AppDataSingleton.getInstance().getLoggedUser().getSyncFacebookProfile());
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("pref_facebook_events")) {
                changeSyncSettings(sharedPreferences.getBoolean(key, false), AppDataSingleton.getInstance().getLoggedUser().getEmail(), 1);
            } else if (key.equals("pref_sync_user_data_facebook")) {
                changeSyncSettings(sharedPreferences.getBoolean(key, false), AppDataSingleton.getInstance().getLoggedUser().getEmail(), 2);
            }
        }

        private void changeSyncSettings(Boolean value, String email, int v) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getString(R.string.localhost_uri))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            UserAppApi api = retrofit.create(UserAppApi.class);
            Call<User> call;
            if (v == 1) {
                call = api.syncFBEvents(new UserFBSyncChangeDTO(email, value));
            } else {
                call = api.syncFBProfile(new UserFBSyncChangeDTO(email, value));
            }
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (!response.isSuccessful()) {
                        Log.d(TAG, "onResponse: FB sync change failed");
                    } else {
                        AppDataSingleton.getInstance().updateUser(response.body());
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Log.d(TAG, "onFailure: FB sync change failed");
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}
