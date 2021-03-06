package rs.ac.uns.ftn.eventsapp.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.threeten.bp.ZonedDateTime;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import rs.ac.uns.ftn.eventsapp.MainActivity;
import rs.ac.uns.ftn.eventsapp.R;
import rs.ac.uns.ftn.eventsapp.apiCalls.UserAppApi;
import rs.ac.uns.ftn.eventsapp.dtos.UserRegisterDTO;
import rs.ac.uns.ftn.eventsapp.firebase.FirebaseSignIn;
import rs.ac.uns.ftn.eventsapp.models.User;
import rs.ac.uns.ftn.eventsapp.sync.SyncGoingInterestedEventsTask;
import rs.ac.uns.ftn.eventsapp.sync.SyncMyEventsTask;
import rs.ac.uns.ftn.eventsapp.sync.SyncReceiverInitTask;
import rs.ac.uns.ftn.eventsapp.sync.SyncUserTask;
import rs.ac.uns.ftn.eventsapp.utils.AppDataSingleton;
import rs.ac.uns.ftn.eventsapp.utils.ZonedGsonBuilder;

public class RegisterActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST = 123;
    private final String EMAIL = "email";
    private final String EVENTS = "user_events";
    private final String TAG = this.getClass().getName();
    private CallbackManager callbackManager;
    private Button btnSelectImage;
    private CircleImageView userProfile;
    private EditText email;
    private EditText username;
    private EditText psw1;
    private EditText psw2;
    private String imgUri = null;
    private Bitmap bitmap;
    private MediaType mediaType;
    private Retrofit retrofit;
    private Uri imageData;
    private long lastSyncTime;
    private SyncReceiverInitTask syncReceiverInitTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        setupFacebookRegistration();

        Button btnRegister = findViewById(R.id.btn_register_register);
        btnSelectImage = findViewById(R.id.btn_select_photo_register);
        userProfile = findViewById(R.id.circle_image_view_register);
        email = findViewById(R.id.edittext_email_register);
        username = findViewById(R.id.edittext_username_register);
        psw1 = findViewById(R.id.edittext_password_1_register);
        psw2 = findViewById(R.id.edittext_password_2_register);
        TextView textAlreadyHaveAccount = findViewById(R.id.text_already_have_an_account_register);
        TextView textContinueAsAnonymous = findViewById(R.id.text_continue_as_anonymous_register);


        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validationSuccess()) {
                    registerUser();
                }
            }
        });

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imgUri != null) {
                    openContextMenu(v);
                } else {
                    selectImage();
                }
            }
        });

        userProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imgUri != null) {
                    openContextMenu(v);
                } else {
                    selectImage();
                }
            }
        });
        registerForContextMenu(btnSelectImage);
        registerForContextMenu(userProfile);

        textAlreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLoginActivity();
            }
        });

        textContinueAsAnonymous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToMainWindow();
            }
        });

        syncReceiverInitTask = new SyncReceiverInitTask();
        //register broadcast listener
        IntentFilter filter = new IntentFilter();
        filter.addAction(SplashScreenActivity.SYNC_MY_EVENTS);
        filter.addAction(SplashScreenActivity.SYNC_GI_EVENTS);
        registerReceiver(syncReceiverInitTask, filter);
    }

    private void setupFacebookRegistration() {
        callbackManager = CallbackManager.Factory.create();
        FacebookSdk.fullyInitialize();

        LoginButton loginButton = findViewById(R.id.login_button_login);
        loginButton.setPermissions(Arrays.asList(EMAIL, EVENTS));

        // Callback login for facebook
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                boolean isLoggedIn = accessToken != null && !accessToken.isExpired();

                Log.d(TAG, "onSuccess: token  = " + accessToken.getToken());
                Log.d(TAG, "onSuccess: logged = " + isLoggedIn);

                Log.d(TAG, "onClick:");

                // Instantiate the backend request
                retrofit = new Retrofit.Builder()
                        .baseUrl(AppDataSingleton.getInstance().SERVER_IP)
                        .addConverterFactory(ZonedGsonBuilder.getZonedGsonFactory())
                        .build();
                UserAppApi api = retrofit.create(UserAppApi.class);
                Call<User> call = api.register(accessToken.getToken());
                call.enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, retrofit2.Response<User> response) {
                        if (!response.isSuccessful()) {
                            if (response.code() == 403) {
                                email.setError(getString(R.string.facebookNotAvailable));
                            } else {
                                Log.d(TAG, "onErrorResponse: Server didn't receive FB token!");
                                Toast.makeText(getApplicationContext(), response.code() + " " + response.body(), Toast.LENGTH_LONG).show();
                                goToNoServer();
                            }
                        } else {
                            Log.d("TAG", response.body().getId().toString());
                            addUserToDB(response.body());
                            //TODO: firebase login+register

                            lastSyncTime = ZonedDateTime.now().toInstant().toEpochMilli();
                            SharedPreferences sharedPreferences = getSharedPreferences(SplashScreenActivity.SYNC_PREFERENCE, MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putLong(SyncUserTask.preferenceSyncUser, lastSyncTime);
                            editor.commit();

                            signInToFirebase(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), R.string.failed, Toast.LENGTH_LONG).show();
                        Log.d("xxs", "onFailure: registration failed");
                        goToNoServer();
                    }
                });
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d(TAG, "onError: " + exception.toString());
                Toast.makeText(getApplicationContext(), "Facebook sent exception: " + exception.getCause(), Toast.LENGTH_LONG).show();
                goToNoServer();
            }
        });
    }

    private void registerUser() {
        UserRegisterDTO registerUser = new UserRegisterDTO(email.getText().toString().trim(), psw1.getText().toString().trim(), username.getText().toString().trim());

        retrofit = new Retrofit.Builder()
                .baseUrl(AppDataSingleton.getInstance().SERVER_IP)
                .addConverterFactory(ZonedGsonBuilder.getZonedGsonFactory())
                .build();
        UserAppApi api = retrofit.create(UserAppApi.class);
        Call<User> call = api.register(registerUser);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 403) {
                        email.setError(getString(R.string.emailAlreadyExists));
                    } else {
                        Toast.makeText(getApplicationContext(), response.code() + " " + response.body(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d("TAG", response.body().getId().toString());

                    if (bitmap != null) {
                        uploadImage(response.body().getId());
                    } else {
                        loginAfterRegisterActivity();
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.failed, Toast.LENGTH_LONG).show();
                Log.d("xxs", "onFailure: registration failed");
            }
        });
    }

    private void uploadImage(Long userId) {
        retrofit = new Retrofit.Builder()
                .baseUrl(AppDataSingleton.getInstance().SERVER_IP)
                .addConverterFactory(ZonedGsonBuilder.getZonedGsonFactory())
                .build();
        UserAppApi e = retrofit.create(UserAppApi.class);
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, bos);
        builder.addFormDataPart("image", "image", RequestBody.create(mediaType, bos.toByteArray()));
        RequestBody requestBody = builder.build();
        Call<User> s = e.uploadImage(requestBody, userId);
        s.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), response.code() + " " + response.body(), Toast.LENGTH_LONG).show();
                    Log.d("xxs", "onResponse: image uploaded success");
                    goToNoServer();
                } else {
                    loginAfterRegisterActivity();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.failed, Toast.LENGTH_LONG).show();
                Log.d("xxs", "onResponse: image upload failed");
                goToNoServer();
            }
        });
    }

    private void addUserToDB(User user) {
        AppDataSingleton.getInstance().setContext(this);
        AppDataSingleton.getInstance().createUser(user);
    }

    /**
     * Check if all user inputs are valid
     *
     * @return
     */
    private boolean validationSuccess() {
        if (email.getText().toString().trim().equals("")) {
            //Toast.makeText(getApplicationContext(), R.string.registerUserValidationEmail, Toast.LENGTH_LONG).show();
            email.setError(getString(R.string.registerUserValidationEmail));
            return false;
        }
        if (!isValidEmail(email.getText())) {
            //Toast.makeText(getApplicationContext(), R.string.registerUserValidationEmail2, Toast.LENGTH_LONG).show();
            email.setError(getString(R.string.registerUserValidationEmail2));
            return false;
        }
        if (username.getText().toString().trim().equals("")) {
            //Toast.makeText(getApplicationContext(), R.string.registerUserValidationName, Toast.LENGTH_LONG).show();
            username.setError(getString(R.string.registerUserValidationName));
            return false;
        }
        if (!isValidName(username.getText())) {
            //Toast.makeText(getApplicationContext(), R.string.registerUserValidationName2, Toast.LENGTH_LONG).show();
            username.setError(getString(R.string.registerUserValidationName2));
            return false;
        }
        if (psw1.getText().toString().trim().equals("")) {
            //Toast.makeText(getApplicationContext(), R.string.registerUserValidationPsw, Toast.LENGTH_LONG).show();
            psw1.setError(getString(R.string.registerUserValidationPsw));
            return false;
        }
        if (!isValidPsw(psw1.getText())) {
            //Toast.makeText(getApplicationContext(), R.string.registerUserValidationPswLength, Toast.LENGTH_LONG).show();
            psw1.setError(getString(R.string.registerUserValidationPsw4));
            return false;
        }
        if (psw2.getText().toString().trim().equals("")) {
            //Toast.makeText(getApplicationContext(), R.string.registerUserValidationPsw2, Toast.LENGTH_LONG).show();
            psw2.setError(getString(R.string.registerUserValidationPsw2));
            return false;
        }
        if (!psw1.getText().toString().equals(psw2.getText().toString())) {
            //Toast.makeText(getApplicationContext(), R.string.registerUserValidationPsw3, Toast.LENGTH_SHORT).show();
            psw2.setError(getString(R.string.registerUserValidationPsw3));
            return false;
        }
        return true;
    }

    private boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    private boolean isValidName(CharSequence target) {
        String regex = "^\\p{L}+[\\p{L} .'-]{2,64}$";
        return (!TextUtils.isEmpty(target) && target.toString().matches(regex));
    }

    private boolean isValidPsw(CharSequence target) {
        String regex = "^(?=.*[A-Z])(?=.*[a-z])((?=.*[@#$%^&+=!])|(?=.*[0-9]))(?=\\S+$).{7,}$";
        return (!TextUtils.isEmpty(target) && target.toString().matches(regex));
    }

    private void goToLoginActivity() {
        LoginManager.getInstance().logOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select an image"), GALLERY_REQUEST);
    }

    /**
     * Rukuje se rezultatom gore poslatog intenta za odabir profilne slike
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null) {
            imageData = data.getData();
            if (imageData != null) {
                addImage(imageData);
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void addImage(Uri imageData) {
        try {
            InputStream is = getContentResolver().openInputStream(imageData);
            bitmap = BitmapFactory.decodeStream(is);
            mediaType = MediaType.parse(getContentResolver().getType(imageData));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        imgUri = imageData.getPath();
        userProfile.setImageURI(imageData);
        btnSelectImage.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.context_menu_profile_image, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.profile_image_option_delete:
                removeImage();
                return true;
            case R.id.profile_image_option_change:
                selectImage();
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void removeImage() {
        imgUri = null;
        bitmap = null;
        mediaType = null;
        userProfile.setImageURI(null);
        btnSelectImage.setVisibility(View.VISIBLE);
    }

    private void goToMainWindow() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void goToNoServer() {
        Intent intent = new Intent(this, NoServerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void loginAfterRegisterActivity() {
        Toast.makeText(getApplicationContext(), getText(R.string.confirmRegistration), Toast.LENGTH_LONG).show();
        LoginManager.getInstance().logOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void signInToFirebase(User registeredUser) {
        //nakon uspesne registracije registrujemo ga i na firebase
        FirebaseSignIn firebaseSignIn = new FirebaseSignIn(RegisterActivity.this);

        firebaseSignIn.performSignIn(registeredUser.getEmail(),
                registeredUser.getPassword(), registeredUser.getName(), registeredUser.getImageUri());
    }

    @Override
    protected void onDestroy() {
        //osloboditi resurse
        if (syncReceiverInitTask != null) {
            unregisterReceiver(syncReceiverInitTask);
        }
        super.onDestroy();
    }
}
