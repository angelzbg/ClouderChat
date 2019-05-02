package angelzani.clouderchat.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "clouder.db";

    SQLiteDatabase db;

    //Таблици имена
    private static final String TABLE_USERS = "Users";
    private static final String TABLE_FRIENDS = "Friends";
    private static final String TABLE_REQUESTS = "Requests";
    private static final String TABLE_PRIVATE_CHATS = "PrivateChats";

    //Колони таблица User
    private static final String USER_ID = "id";
    private static final String USER_UID = "user_uid";
    private static final String AVATAR = "avatar";
    private static final String AVATARCH = "avatarCh";
    private static final String AVATARSTRING = "avatarString";
    private static final String DATECR = "dateCr";
    private static final String NICK = "nick";
    private static final String NICKCH = "nickCh";
    private static final String POINTS = "points";
    private static final String ONLINE = "online";

    //Колони таблица Friends
    //private static final String USER_ID = "id";
    //private static final String USER_UID = "user_uid";
    private static final String FRIEND_USER_ID = "friend_uid";

    //Колони таблица Requests
    private static final String REQUEST_ID = "id";
    private static final String REQUEST_USER_UID = "user_uid";
    private static final String REQUEST_SENDER_UID = "sender_uid";
    private static final String REQUEST_MESSAGE = "message";
    private static final String REQUEST_DATE = "date";

    //Колони таблица PrivateChats
    private static final String PCHATS_ID = "id";
    private static final String PCHATS_TARGET_UID = "targer_uid";
    private static final String PCHATS_SENDER_UID = "sender_uid";
    private static final String PCHATS_DATE = "date";
    private static final String PCHATS_TYPE = "type";
    private static final String PCHATS_MESSAGE = "message";


    //Query за създаване на таблиците
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS +
                    " ( '" + USER_ID + "' INTEGER PRIMARY KEY AUTOINCREMENT , '" +
                    USER_UID + "' TEXT UNIQUE, '" +
                    AVATAR + "' TEXT, '" +
                    AVATARCH + "' INTEGER, '" +
                    AVATARSTRING + "' TEXT, '" +
                    DATECR + "' INTEGER, '" +
                    NICK + "' TEXT, '" +
                    NICKCH + "' INTEGER, '" +
                    POINTS + "' INTEGER, '" +
                    ONLINE + "' BOOLEAN );";

    private static final String CREATE_TABLE_FRIENDS =
            "CREATE TABLE " + TABLE_FRIENDS +
                    " ( '" + USER_ID + "' INTEGER PRIMARY KEY AUTOINCREMENT, '" +
                    USER_UID + "' TEXT, '" +
                    FRIEND_USER_ID + "' TEXT );";

    private static final String CREATE_TABLE_REQUESTS =
            "CREATE TABLE " + TABLE_REQUESTS +
                    " ( '" + REQUEST_ID + "' INTEGER PRIMARY KEY AUTOINCREMENT, '" +
                    REQUEST_USER_UID + "' TEXT, '" +
                    REQUEST_SENDER_UID + "' TEXT, '" +
                    REQUEST_MESSAGE + "' TEXT, '" +
                    REQUEST_DATE + "' INTEGER );";

    private static final String CREATE_TABLE_PRIVATE_CHATS =
            "CREATE TABLE " + TABLE_PRIVATE_CHATS +
                    " ( '" + PCHATS_ID + "' INTEGER PRIMARY KEY AUTOINCREMENT, '" +
                    PCHATS_TARGET_UID + "' TEXT, '" +
                    PCHATS_SENDER_UID + "' TEXT, '" +
                    PCHATS_DATE + "' INTEGER, '" +
                    PCHATS_TYPE + "' INTEGER, '" +
                    PCHATS_MESSAGE + "' BLOB );";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE_USERS);
        sqLiteDatabase.execSQL(CREATE_TABLE_FRIENDS);
        sqLiteDatabase.execSQL(CREATE_TABLE_REQUESTS);
        sqLiteDatabase.execSQL(CREATE_TABLE_PRIVATE_CHATS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE " + TABLE_USERS);
        sqLiteDatabase.execSQL("DROP TABLE " + TABLE_FRIENDS);
        sqLiteDatabase.execSQL("DROP TABLE " + TABLE_REQUESTS);
        sqLiteDatabase.execSQL("DROP TABLE " + TABLE_PRIVATE_CHATS);
        onCreate(sqLiteDatabase);
    }

    // -------------------------------------------------- USERS [ START ]

    public void addUser(UserModel user) {
        try {
            db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(USER_UID, user.getUid());
            cv.put(AVATAR, user.getAvatar());
            cv.put(AVATARCH, user.getAvatarCh());
            cv.put(AVATARSTRING, user.getAvatarString());
            cv.put(DATECR, user.getDateCr());
            cv.put(NICK, user.getNick());
            cv.put(NICKCH, user.getNickCh());
            cv.put(POINTS, user.getPoints());
            cv.put(ONLINE, user.isOnline());

            db.insertOrThrow(TABLE_USERS, null, cv);
        } catch (SQLException e) {
            Log.e("SQLError", e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    } // end of addUser()

    public UserModel getUser(String uid) {
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + USER_UID + " LIKE '" + uid + "';";

            c = db.rawQuery(query, null);
            if (c.moveToFirst()) {
                UserModel user = new UserModel();

                user.setUid(c.getString(c.getColumnIndex(USER_UID)));
                user.setAvatar(c.getString(c.getColumnIndex(AVATAR)));
                user.setAvatarCh(c.getLong(c.getColumnIndex(AVATARCH)));
                user.setAvatarString(c.getString(c.getColumnIndex(AVATARSTRING)));
                user.setDateCr(c.getLong(c.getColumnIndex(DATECR)));
                user.setNick(c.getString(c.getColumnIndex(NICK)));
                user.setNickCh(c.getLong(c.getColumnIndex(NICKCH)));
                user.setPoints(c.getLong(c.getColumnIndex(POINTS)));
                boolean online = c.getInt(c.getColumnIndex(ONLINE)) > 0;
                user.setOnline(online);

                return user;
            }
        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    } // end of getUser()

    public void updateUser(UserModel user) {
        try {
            db = getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(AVATAR, user.getAvatar());
            cv.put(AVATARCH, user.getAvatarCh());
            cv.put(AVATARSTRING, user.getAvatarString());
            cv.put(DATECR, user.getDateCr());
            cv.put(NICK, user.getNick());
            cv.put(NICKCH, user.getNickCh());
            cv.put(POINTS, user.getPoints());
            cv.put(ONLINE, user.isOnline());

            db.update(TABLE_USERS, cv, USER_UID + " LIKE '" + user.getUid() + "'", null);

        } catch (SQLException e) {
            Log.e("SQLError", e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    } // end of updateUser()

    public int getUserIdForNotificationId(String uid) {
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT " + USER_ID + " FROM " + TABLE_USERS + " WHERE " + USER_UID + " LIKE '" + uid + "';";

            c = db.rawQuery(query, null);
            if (c.moveToFirst()) {
                int id = c.getInt(c.getColumnIndex(USER_ID)) + 323000; // не знам кои програми какви айдита използват, но на мен ми трябва нещо конкретно, което да не се бърка с известни приложения (залагам на 323000)
                return id;
            }
        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return 0;
    } // end of getUser()

    // -------------------------------------------------- USERS [  END  ]


    // -------------------------------------------------- FRIENDS [ START ]

    public void addFriend(String userUID, String friendUID) {
        try {
            db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(USER_UID, userUID);
            cv.put(FRIEND_USER_ID, friendUID);

            db.insertOrThrow(TABLE_FRIENDS, null, cv);
        } catch (SQLException e) {
            Log.e("SQLError", e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    } // end of addFriend()

    public void removeFriend(String userUID, String friendUID) {
        try {
            db = getWritableDatabase();
            //db.delete(TABLE_FRIENDS, USER_UID + " LIKE ? AND " + FRIEND_USER_ID + " LIKE ?", new String[] { userUID, friendUID });
            String query = "DELETE FROM " + TABLE_FRIENDS + " WHERE " + USER_UID + " LIKE '" + userUID + "' AND " + FRIEND_USER_ID + " LIKE '" + friendUID + "';";
            db.execSQL(query);
        } catch (SQLException e) {
            Log.e("SQLError", e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    } // end of removeFriend()

    public ArrayList<String> getAllFriendsUIDs(String userUID) {
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT " + FRIEND_USER_ID + " FROM " + TABLE_FRIENDS + " WHERE " + USER_UID + " LIKE '" + userUID + "';";

            c = db.rawQuery(query, null);
            if (c.isBeforeFirst()) {
                ArrayList<String> friends = new ArrayList<String>();
                while (c.moveToNext()) {
                    friends.add(c.getString(c.getColumnIndex(FRIEND_USER_ID)));
                }
                return friends;
            }
        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    } // end of getFriends()

    public boolean isFriend(String userUID, String friendUID) {
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_FRIENDS + " WHERE " + USER_UID + " LIKE '" + userUID + "' AND " + FRIEND_USER_ID + " LIKE '" + friendUID + "';";

            c = db.rawQuery(query, null);
            if (c.moveToFirst()) {
                return true;
            }
        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return false;
    } // end of getFriends()

    // -------------------------------------------------- FRIENDS [  END  ]


    // -------------------------------------------------- REQUESTS [ START ]

    public void addRequest(RequestModel request) {
        try {
            db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(REQUEST_USER_UID, request.getUserUID());
            cv.put(REQUEST_SENDER_UID, request.getSenderUID());
            cv.put(REQUEST_MESSAGE, request.getMessage());
            cv.put(REQUEST_DATE, request.getDate());

            db.insertOrThrow(TABLE_REQUESTS, null, cv);
        } catch (SQLException e) {
            Log.e("SQLError", e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    } // end of addRequest()

    public void removeRequest(String userUID, String senderUID) {
        try {
            db = getWritableDatabase();
            db.delete(TABLE_REQUESTS, REQUEST_USER_UID + " LIKE ? AND " + REQUEST_SENDER_UID + " LIKE ?", new String[]{userUID, senderUID});
        } catch (SQLException e) {
            Log.e("SQLError", e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    } // end of removeFriend()

    public ArrayList<RequestModel> getAllRequests(String userUID) {
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT " + REQUEST_USER_UID + ", " + REQUEST_SENDER_UID + ", " + REQUEST_MESSAGE + ", " + REQUEST_DATE + " FROM " + TABLE_REQUESTS + " WHERE " + REQUEST_USER_UID + " LIKE '" + userUID + "' ORDER BY " + REQUEST_DATE + " DESC;";

            c = db.rawQuery(query, null);
            if (c.isBeforeFirst()) {
                ArrayList<RequestModel> allRequests = new ArrayList<>();
                while (c.moveToNext()) {
                    RequestModel request = new RequestModel();
                    request.setUserUID(userUID);
                    request.setSenderUID(c.getString(c.getColumnIndex(REQUEST_SENDER_UID)));
                    request.setMessage(c.getString(c.getColumnIndex(REQUEST_MESSAGE)));
                    request.setDate(c.getLong(c.getColumnIndex(REQUEST_DATE)));
                    allRequests.add(request);
                }
                return allRequests;
            }
        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    }

    // -------------------------------------------------- REQUESTS [  END  ]


    // -------------------------------------------------- PRIVATE CHATS [ START ]

    public void addMessage(PrivChatModel chatMessage) {
        try {
            db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(PCHATS_TARGET_UID, chatMessage.getTargetUID());
            cv.put(PCHATS_SENDER_UID, chatMessage.getSenderUID());
            cv.put(PCHATS_DATE, chatMessage.getDate());
            cv.put(PCHATS_TYPE, chatMessage.getType());
            cv.put(PCHATS_MESSAGE, chatMessage.getMessage());

            db.insertOrThrow(TABLE_PRIVATE_CHATS, null, cv);
        } catch (SQLException e) {
            Log.e("SQLError", e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    } // end of addMessage()

    public PrivChatModel getLastMessageFromUser(String targetUID, String senderUID) {
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_PRIVATE_CHATS + " WHERE " + PCHATS_TARGET_UID + " LIKE '" + targetUID + "' AND " + PCHATS_SENDER_UID + " LIKE '" + senderUID + "' ORDER BY " + PCHATS_DATE + " DESC LIMIT 1;";

            PrivChatModel privchatmessageFROM = null, privchatmessageTO = null;

            c = db.rawQuery(query, null);
            if (c.moveToFirst()) {
                privchatmessageFROM = new PrivChatModel(c.getString(c.getColumnIndex(PCHATS_TARGET_UID)), c.getString(c.getColumnIndex(PCHATS_SENDER_UID)), c.getString(c.getColumnIndex(PCHATS_MESSAGE)), c.getInt(c.getColumnIndex(PCHATS_TYPE)), c.getLong(c.getColumnIndex(PCHATS_DATE)));
            }

            query = "SELECT * FROM " + TABLE_PRIVATE_CHATS + " WHERE " + PCHATS_TARGET_UID + " LIKE '" + senderUID + "' AND " + PCHATS_SENDER_UID + " LIKE '" + targetUID + "' ORDER BY " + PCHATS_DATE + " DESC LIMIT 1;";
            c = db.rawQuery(query, null);
            if (c.moveToFirst()) {
                privchatmessageTO = new PrivChatModel(c.getString(c.getColumnIndex(PCHATS_TARGET_UID)), c.getString(c.getColumnIndex(PCHATS_SENDER_UID)), c.getString(c.getColumnIndex(PCHATS_MESSAGE)), c.getInt(c.getColumnIndex(PCHATS_TYPE)), c.getLong(c.getColumnIndex(PCHATS_DATE)));
            }

            if (privchatmessageFROM != null && privchatmessageTO != null) {

                if (privchatmessageTO.getDate() > privchatmessageFROM.getDate()) {
                    return privchatmessageTO;
                } else {
                    return privchatmessageFROM; // ако са пратени по едно и също време пак ще върне ОТ
                }

            } else if (privchatmessageFROM != null) {
                return privchatmessageFROM;
            } else if (privchatmessageTO != null) {
                return privchatmessageTO;
            }

        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    } // end of getLastMessageFromUser()

    public ArrayList<PrivChatModel> getLast20MessagesFromOrToUser(String userUID, String friendUID) {
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_PRIVATE_CHATS + " WHERE " + PCHATS_TARGET_UID + " LIKE '" + userUID + "' AND " + PCHATS_SENDER_UID + " LIKE '" + friendUID + "' OR " + PCHATS_TARGET_UID + " LIKE '" + friendUID + "' AND " + PCHATS_SENDER_UID + " LIKE '" + userUID + "' ORDER BY " + PCHATS_DATE + " DESC LIMIT 20;";

            c = db.rawQuery(query, null);
            if (c.isBeforeFirst()) {
                ArrayList<PrivChatModel> messages = new ArrayList<>();
                while(c.moveToNext()){
                    messages.add(new PrivChatModel(c.getString(c.getColumnIndex(PCHATS_TARGET_UID)), c.getString(c.getColumnIndex(PCHATS_SENDER_UID)), c.getString(c.getColumnIndex(PCHATS_MESSAGE)), c.getInt(c.getColumnIndex(PCHATS_TYPE)), c.getLong(c.getColumnIndex(PCHATS_DATE))));
                }
                return messages;
            }

        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    }// end of getLast20MessagesFromOrToUser()

    public ArrayList<PrivChatModel> getLast20MessagesFromOrToUserOlder(String userUID, String friendUID, Long date) { // въщност ще бъдат лимитирани до 5
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_PRIVATE_CHATS + " WHERE (" + PCHATS_TARGET_UID + " LIKE '" + userUID + "' AND " + PCHATS_SENDER_UID + " LIKE '" + friendUID + "' OR " + PCHATS_TARGET_UID + " LIKE '" + friendUID + "' AND " + PCHATS_SENDER_UID + " LIKE '" + userUID + "') AND " + PCHATS_DATE + " < " + date + " ORDER BY " + PCHATS_DATE + " DESC LIMIT 5;";

            c = db.rawQuery(query, null);
            if (c.isBeforeFirst()) {
                ArrayList<PrivChatModel> messages = new ArrayList<>();
                while(c.moveToNext()){
                    messages.add(new PrivChatModel(c.getString(c.getColumnIndex(PCHATS_TARGET_UID)), c.getString(c.getColumnIndex(PCHATS_SENDER_UID)), c.getString(c.getColumnIndex(PCHATS_MESSAGE)), c.getInt(c.getColumnIndex(PCHATS_TYPE)), c.getLong(c.getColumnIndex(PCHATS_DATE))));
                }
                return messages;
            }

        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    }// end of getLast20MessagesFromOrToUser()

    public String getImageFromMessage(String senderUID, String targetUID, long date) {
        Cursor c = null;
        try {
            db = getReadableDatabase();
            String query = "SELECT " + PCHATS_MESSAGE + " FROM " + TABLE_PRIVATE_CHATS + " WHERE " + PCHATS_SENDER_UID + " LIKE '" + senderUID + "' AND " + PCHATS_TARGET_UID + " LIKE '" + targetUID + "' AND " + PCHATS_DATE + " = " + date + " AND " + PCHATS_TYPE + " = 2;"; // трябва да върне само едно

            c = db.rawQuery(query, null);
            if (c.isBeforeFirst()) {
                c.moveToFirst(); // за всеки случай нека да е само един резултата
                String imageString = c.getString(c.getColumnIndex(PCHATS_MESSAGE));
                return imageString;
            }

        } catch (SQLException e) {
            Log.e("SQLException", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    }// end of getImageFromMessage()

    // -------------------------------------------------- PRIVATE CHATS [  END  ]

} // end of DatabaseHelper{}