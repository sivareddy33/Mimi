package com.leagueofshadows.enc.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "SAL-1";

    static final String ID = "ID";
    private static final String TABLE_MESSAGES = "Messages";
    static final String MESSAGES_ID = "Messages_id";
    static final String MESSAGES_TO = "Messages_to";
    static final String MESSAGES_FROM = "Messages_from";
    static final String MESSAGES_CONTENT = "Messages_content";
    static final String MESSAGES_TYPE = "Messages_type";
    static final String MESSAGES_FILEPATH = "Messages_filepath";
    static final String MESSAGES_TIMESTAMP = "Messages_timestamp";
    static final String MESSAGES_SENT = "Messages_sent";
    static final String MESSAGES_RECEIVED = "Messages_received";
    static final String MESSAGES_SEEN = "Messages_seen";
    static final String MESSAGES_IS_GROUP_MESSAGE = "Messages_is_group";

    static final String TABLE_ENCRYPTED_MESSAGES = "Encrypted_Messages";
    static final String ENCRYPTED_MESSAGES_ID = "Encrypted_Messages_id";
    static final String ENCRYPTED_MESSAGES_TO = "Encrypted_Messages_to";
    static final String ENCRYPTED_MESSAGES_FROM = "Encrypted_Messages_from";
    static final String ENCRYPTED_MESSAGES_CONTENT = "Encrypted_Messages_content";
    static final String ENCRYPTED_MESSAGES_TYPE = "Encrypted_Messages_type";
    static final String ENCRYPTED_MESSAGES_FILEPATH = "Encrypted_Messages_filepath";
    static final String ENCRYPTED_MESSAGES_TIMESTAMP = "Encrypted_Messages_timestamp";
    static final String ENCRYPTED_MESSAGES_RESEND = "Encrypted_Messages_resend";
    static final String ENCRYPTED_MESSAGES_IS_GROUP_MESSAGE = "Encrypted_Messages_Is_Group";

    static final String TABLE_USERS = "Users";
    static final String USERS_ID = "Users_id";
    static final String USERS_NUMBER = "Users_number";
    static final String USERS_NAME = "Users_name";
    static final String USERS_PUBLICKEY = "Users_publickey";

    static final String TABLE_USER_DATA = "Users_data";
    static final String USER_DATA_USERS_ID = "Users_id";
    static final String USER_DATA_MESSAGES_ID = "Messages_Id";
    static final String USER_DATA_NEW_MESSAGE_COUNT = "Message_count";
    static final String USER_DATA_TIME = "USER_DATA_TIME";

    static final String TABLE_RESEND_MESSAGE = "Resend_messages";
    static final String RESEND_MESSAGE_USER_ID = "User_id";
    static final String RESEND_MESSAGE_MESSAGE_ID = "Messages_Id";

    static final String TABLE_GROUPS = "Groups";
    static final String GROUPS_ID = "Groups_id";
    static final String GROUPS_NAME = "Groups_name";
    static final String GROUPS_ADMINS = "Groups_admins";
    static final String GROUPS_ACTIVE = "Groups_active";

    static final String TABLE_GROUP_PARTICIPANTS = "Group_participants";
    static final String GROUP_PARTICIPANTS_GROUP_ID = "Group_id";
    static final String GROUP_PARTICIPANTS_USER_ID = "User_id";

    static final String TABLE_MESSAGES_RECEIVED_STATUS = "Group_received_status";
    static final String RECEIVED_STATUS_USER_ID = "User_id";
    static final String RECEIVED_STATUS_MESSAGE_ID = "Message_id";
    static final String RECEIVED_STATUS_TIMESTAMP = "Timestamp";

    static final String TABLE_MESSAGES_SEEN_STATUS = "Group_seen_status";
    static final String SEEN_STATUS_USER_ID = "User_id";
    static final String SEEN_STATUS_MESSAGE_ID = "Message_id";
    static final String SEEN_STATUS_TIMESTAMP = "Timestamp";

    static final String TABLE_MESSAGES_CIPHER_TEXT = "Cipher_text";
    static final String CIPHER_TEXT_MESSAGE_ID = "Message_id";
    static final String CIPHER_TEXT_CIPHER = "Cipher";

    static final String TABLE_MESSAGE_STATUS_SYNC = "Message_status_sync";

    static final String STATUS_SYNC_MESSAGE_ID = "Message_id";
    static final String STATUS_SYNC_USER_ID = "User_id";
    static final String STATUS_SYNC_TIMESTAMP = "Timestamp";
    static final String STATUS_SYNC_FROM_USER_ID = "From_user_id";
    static final String STATUS_SYNC_STATUS_TYPE = "Status_type";
    static final String STATUS_SYNC_MESSAGE_TYPE = "Message_type";


    public SQLHelper(Context context) {
        super(context,DATABASE_NAME,null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + MESSAGES_ID + " TEXT ,"
                + MESSAGES_TO + " TEXT ,"
                +MESSAGES_FROM+"  TEXT ,"
                + MESSAGES_CONTENT + " TEXT ,"
                +MESSAGES_TYPE+" INTEGER ,"
                +MESSAGES_FILEPATH+" TEXT ,"
                +MESSAGES_TIMESTAMP+" TEXT ,"
                +MESSAGES_SENT+" TEXT ,"
                +MESSAGES_RECEIVED+" TEXT, "
                +MESSAGES_SEEN+" TEXT ,"
                +MESSAGES_IS_GROUP_MESSAGE+" INTEGER "
                + ")";

        String CREATE_ENCRYPTED_MESSAGES_TABLE = "CREATE TABLE " + TABLE_ENCRYPTED_MESSAGES + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + ENCRYPTED_MESSAGES_ID + " TEXT ,"
                + ENCRYPTED_MESSAGES_TO + " TEXT ,"
                +ENCRYPTED_MESSAGES_FROM+"  TEXT ,"
                + ENCRYPTED_MESSAGES_CONTENT + " TEXT ,"
                +ENCRYPTED_MESSAGES_TYPE+" INTEGER ,"
                +ENCRYPTED_MESSAGES_FILEPATH+" TEXT ,"
                +ENCRYPTED_MESSAGES_TIMESTAMP+" TEXT ,"
                +ENCRYPTED_MESSAGES_RESEND+" BOOLEAN ,"
                +ENCRYPTED_MESSAGES_IS_GROUP_MESSAGE+" INTEGER "
                + ")";

        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + USERS_ID + " TEXT PRIMARY KEY  ,"
                + USERS_NAME + " TEXT ,"
                + USERS_NUMBER+"  TEXT ,"
                + USERS_PUBLICKEY + " TEXT "
                + ")";

        String CREATE_USER_DATA_TABLE = "CREATE TABLE " + TABLE_USER_DATA + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + USER_DATA_USERS_ID + " TEXT ,"
                + USER_DATA_MESSAGES_ID+"  TEXT ,"
                + USER_DATA_NEW_MESSAGE_COUNT+"  INTEGER ,"
                + USER_DATA_TIME+"  INTEGER "
                + ")";

        String CREATE_RESEND_MESSAGE_TABLE = "CREATE TABLE " + TABLE_RESEND_MESSAGE + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + RESEND_MESSAGE_MESSAGE_ID + " TEXT ,"
                + RESEND_MESSAGE_USER_ID+"  TEXT "
                + ")";

        String CREATE_GROUPS_TABLE = "CREATE TABLE "+TABLE_GROUPS + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + GROUPS_ID + " TEXT ,"
                + GROUPS_NAME+"  TEXT ,"
                + GROUPS_ADMINS+"  TEXT ,"
                + GROUPS_ACTIVE+"  INTEGER "
                + ")";

        String CREATE_GROUPS_PARTICIPANTS_TABLE = "CREATE TABLE "+TABLE_GROUP_PARTICIPANTS + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + GROUP_PARTICIPANTS_GROUP_ID + " TEXT ,"
                + GROUP_PARTICIPANTS_USER_ID+"  TEXT "
                + ")";

        String CREATE_GROUPS_RECEIVED_STATUS_TABLE = "CREATE TABLE "+TABLE_MESSAGES_RECEIVED_STATUS + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + RECEIVED_STATUS_USER_ID + " TEXT ,"
                + RECEIVED_STATUS_MESSAGE_ID+"  TEXT ,"
                + RECEIVED_STATUS_TIMESTAMP+" TEXT "
                + ")";

        String CREATE_GROUPS_SEEN_STATUS_TABLE = "CREATE TABLE "+TABLE_MESSAGES_SEEN_STATUS + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + SEEN_STATUS_USER_ID + " TEXT ,"
                + SEEN_STATUS_MESSAGE_ID+"  TEXT ,"
                + SEEN_STATUS_TIMESTAMP+" TEXT "
                + ")";

        String CREATE_CIPHER_TEXT_TABLE = "CREATE TABLE "+TABLE_MESSAGES_CIPHER_TEXT + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + CIPHER_TEXT_MESSAGE_ID + " TEXT ,"
                + CIPHER_TEXT_CIPHER+"  TEXT "
                + ")";
        String CREATE_MESSAGE_STATUS_SYNC_TABLE= "CREATE TABLE " + TABLE_MESSAGE_STATUS_SYNC + "("
                + ID + " INTEGER PRIMARY KEY  ,"
                + STATUS_SYNC_MESSAGE_ID + " TEXT ,"
                + STATUS_SYNC_FROM_USER_ID+"  TEXT ,"
                + STATUS_SYNC_USER_ID+"  TEXT ,"
                + STATUS_SYNC_TIMESTAMP+"  TEXT , "
                + STATUS_SYNC_STATUS_TYPE+"  INTEGER , "
                + STATUS_SYNC_MESSAGE_TYPE+"  INTEGER  "
                + ")";

        sqLiteDatabase.execSQL(CREATE_MESSAGES_TABLE);
        sqLiteDatabase.execSQL(CREATE_ENCRYPTED_MESSAGES_TABLE);
        sqLiteDatabase.execSQL(CREATE_USERS_TABLE);
        sqLiteDatabase.execSQL(CREATE_USER_DATA_TABLE);
        sqLiteDatabase.execSQL(CREATE_RESEND_MESSAGE_TABLE);
        sqLiteDatabase.execSQL(CREATE_GROUPS_TABLE);
        sqLiteDatabase.execSQL(CREATE_GROUPS_PARTICIPANTS_TABLE);
        sqLiteDatabase.execSQL(CREATE_GROUPS_RECEIVED_STATUS_TABLE);
        sqLiteDatabase.execSQL(CREATE_GROUPS_SEEN_STATUS_TABLE);
        sqLiteDatabase.execSQL(CREATE_CIPHER_TEXT_TABLE);
        sqLiteDatabase.execSQL(CREATE_MESSAGE_STATUS_SYNC_TABLE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_MESSAGES);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_USERS);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_RESEND_MESSAGE);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_USER_DATA);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_ENCRYPTED_MESSAGES);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_GROUP_PARTICIPANTS);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_GROUPS);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_MESSAGES_RECEIVED_STATUS);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_MESSAGES_SEEN_STATUS);
        sqLiteDatabase.execSQL("DROP TABLE  IF EXISTS "+TABLE_MESSAGE_STATUS_SYNC);
        onCreate(sqLiteDatabase);
    }
}
