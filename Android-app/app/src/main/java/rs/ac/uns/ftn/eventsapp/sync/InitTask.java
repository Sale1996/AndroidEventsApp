package rs.ac.uns.ftn.eventsapp.sync;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.facebook.login.LoginManager;

import rs.ac.uns.ftn.eventsapp.MainActivity;
import rs.ac.uns.ftn.eventsapp.activities.NoInternetActivity;
import rs.ac.uns.ftn.eventsapp.activities.SignInActivity;
import rs.ac.uns.ftn.eventsapp.models.User;
import rs.ac.uns.ftn.eventsapp.tools.InternetConnectionType;
import rs.ac.uns.ftn.eventsapp.utils.AppDataSingleton;

public class InitTask extends AsyncTask<Void, Void, Void> {

    private static final int SPLASH_TIME_OUT = 6000; // splash ce biti vidljiv minimum SPLASH_TIME_OUT milisekundi
    private Context context;

    private long startTime;

    public InitTask(@NonNull Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        startTime = System.currentTimeMillis();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        checkInternetConnection();
        //continueLogin();

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

    private void checkInternetConnection() {
        int networkStatus = InternetConnectionType.getConnectivityStatus(context);

        if (networkStatus == InternetConnectionType.TYPE_NOT_CONNECTED) {
            startNoInternetActivity();
        } else {
            checkLastDBUser();
        }
    }

    /**
     * Pull last user email and password from db if exists and sync with server
     */
    private void checkLastDBUser() {
        AppDataSingleton.getInstance().setContext(context);
        User dbUser = AppDataSingleton.getInstance().getLoggedUser();
        if (dbUser != null) {
            new SyncUserTask(context).execute();
        } else {
            startSignInActivity();
        }
    }

    private void startNoInternetActivity() {
        Intent intent = new Intent(context, NoInternetActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private void startSignInActivity() {
        LoginManager.getInstance().logOut();
        Intent intent = new Intent(context, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
