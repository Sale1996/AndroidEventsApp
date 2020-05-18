package rs.ac.uns.ftn.eventsapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import rs.ac.uns.ftn.eventsapp.database.UserSQLiteHelper;
import rs.ac.uns.ftn.eventsapp.models.User;

public class AppDataSingleton {

    private User loggedUser;
    //TODO: ovde dodati ostale klase koje se cuvaju

    private SQLiteDatabase mDatabase;
    private UserSQLiteHelper dbUserHelper;

    private static final AppDataSingleton ourInstance = new AppDataSingleton();

    public static AppDataSingleton getInstance() {
        return ourInstance;
    }

    public void setContext(Context context) {
        dbUserHelper = new UserSQLiteHelper(context);
        mDatabase = dbUserHelper.getWritableDatabase();
    }

    private AppDataSingleton() {
    }

    public void create(User user) {
        if (user == null) {
            return;
        }

        delete();

        //edit user table
        ContentValues cv = new ContentValues();
        cv.put(UserSQLiteHelper.COLUMN_ID, user.getId());
        cv.put(UserSQLiteHelper.COLUMN_FB_ID, user.getFacebookId());
        cv.put(UserSQLiteHelper.COLUMN_NAME, user.getName());
        cv.put(UserSQLiteHelper.COLUMN_IMAGE_URI, user.getImageUri());
        cv.put(UserSQLiteHelper.COLUMN_IMAGE_HEIGHT, user.getImageHeight());
        cv.put(UserSQLiteHelper.COLUMN_IMAGE_WIDTH, user.getImageWidth());
        cv.put(UserSQLiteHelper.COLUMN_EMAIL, user.getEmail());
        cv.put(UserSQLiteHelper.COLUMN_PASSWORD, user.getPassword());
        cv.put(UserSQLiteHelper.COLUMN_ACTIVATED, user.getActivatedAccount());
        cv.put(UserSQLiteHelper.COLUMN_SYNC_FB_EVENTS, user.getSyncFacebookEvents());
        cv.put(UserSQLiteHelper.COLUMN_SYNC_FB_PROFILE, user.getSyncFacebookProfile());
        mDatabase.insert(UserSQLiteHelper.TABLE_USER, null, cv);

        //TODO: edit other tables like event, friendship, my events, going,...

        loggedUser = user;
    }

    public User read(User user) {
        if (loggedUser != null) {
            return loggedUser;
        }
        Cursor cursor = mDatabase.query(UserSQLiteHelper.TABLE_USER, null, null, null, null, null, UserSQLiteHelper.COLUMN_ID + " DESC");
        loggedUser = new User(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getInt(4),
                cursor.getInt(5),
                cursor.getString(6),
                cursor.getString(7),
                null, null, null, null, null, null, null, null, null, null,
                cursor.getInt(8) == 1,
                cursor.getInt(9) == 1,
                cursor.getInt(10) == 1);

        return loggedUser;
    }

    public void update(User user) {
        ContentValues cv = new ContentValues();
        cv.put(UserSQLiteHelper.COLUMN_ID, user.getId());
        cv.put(UserSQLiteHelper.COLUMN_FB_ID, user.getFacebookId());
        cv.put(UserSQLiteHelper.COLUMN_NAME, user.getName());
        cv.put(UserSQLiteHelper.COLUMN_IMAGE_URI, user.getImageUri());
        cv.put(UserSQLiteHelper.COLUMN_IMAGE_HEIGHT, user.getImageHeight());
        cv.put(UserSQLiteHelper.COLUMN_IMAGE_WIDTH, user.getImageWidth());
        cv.put(UserSQLiteHelper.COLUMN_EMAIL, user.getEmail());
        cv.put(UserSQLiteHelper.COLUMN_PASSWORD, user.getPassword());
        cv.put(UserSQLiteHelper.COLUMN_ACTIVATED, user.getActivatedAccount());
        cv.put(UserSQLiteHelper.COLUMN_SYNC_FB_EVENTS, user.getSyncFacebookEvents());
        cv.put(UserSQLiteHelper.COLUMN_SYNC_FB_PROFILE, user.getSyncFacebookProfile());
        mDatabase.update(UserSQLiteHelper.TABLE_USER, cv, null, null);

        this.loggedUser = user;
    }

    public void delete() {
        if (this.loggedUser == null){
            return;
        }

        mDatabase.delete(UserSQLiteHelper.TABLE_USER, UserSQLiteHelper.COLUMN_ID + "=" + this.loggedUser.getId(), null);
        //TODO: ovde dodati brisanje ostalih klasa - ovo je za loggout
        loggedUser = null;
    }

    public boolean isLoggedIn() {
        return loggedUser != null;
    }

    public User getLoggedUser(){
        return loggedUser;
    }


}