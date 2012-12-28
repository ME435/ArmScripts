package edu.rosehulman.armscripts.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbOpenHelper extends SQLiteOpenHelper{
  
  private static final String TAG = "DbOpenHelper";
  private static final String DATABASE_NAME = "arm_scripts.db";
  private static final int DATABASE_VERSION = 4;

  private static DbOpenHelper sSingleton = null;
  private static final String MOCK_DATABASE_NAME = "mock_arm_scripts.db";
  private static final boolean MOCK = false;

  /**
   * Creates or just retrieves a singleton instance of DatabaseHelper. See
   * http://www.touchlab.co/blog/single-sqlite-connection/ More complicated
   * than the singleton pattern there, but keeps us from having to pass a
   * Context into the adapter classes.
   * 
   * @param context The context to use
   * @return The single instance of DatabaseHelper
   */
  public static synchronized DbOpenHelper createInstance(Context context) {
    sSingleton = new DbOpenHelper(context, MOCK ? MOCK_DATABASE_NAME
        : DATABASE_NAME);
    return sSingleton;
  }

  /**
   * Retrieves the instance of DatabaseHelper
   * 
   * @return The single instance of DatabaseHelper
   */
  public static DbOpenHelper getInstance() {
    assert (sSingleton != null);
    return sSingleton;
  }

  private DbOpenHelper(Context context, String dbName) {
    super(context, dbName, null, DATABASE_VERSION);
  }
    
  /* - - - - -  Projects  - - - - - */
  private static final String CREATE_STATEMENT_PROJECTS =
      "create table " + ProjectDbAdapter.TABLE_NAME +
      " (" + ProjectDbAdapter.KEY_ID + " integer primary key autoincrement, " +
      ProjectDbAdapter.KEY_NAME + " text not null);";
  private static String DROP_STATEMENT_PROJECTS = "DROP TABLE IF EXISTS " + 
      ProjectDbAdapter.TABLE_NAME;
  
  /* - - - - -  Positions  - - - - - */
  private static final String CREATE_STATEMENT_POSITIONS =
      "create table " + PositionDbAdapter.TABLE_NAME +
      " (" + PositionDbAdapter.KEY_ID + " integer primary key autoincrement, " +
      PositionDbAdapter.KEY_PARENT_PROJECT_ID + " integer not null, " +
      PositionDbAdapter.KEY_NAME + " text not null, " +
      PositionDbAdapter.KEY_JOINT_1 + " integer not null, " +
      PositionDbAdapter.KEY_JOINT_2 + " integer not null, " +
      PositionDbAdapter.KEY_JOINT_3 + " integer not null, " +
      PositionDbAdapter.KEY_JOINT_4 + " integer not null, " +
      PositionDbAdapter.KEY_JOINT_5 + " integer not null);";
  private static String DROP_STATEMENT_POSITIONS = "DROP TABLE IF EXISTS " +
      PositionDbAdapter.TABLE_NAME;

  /* - - - - -   Scripts   - - - - - */
  private static final String CREATE_STATEMENT_SCRIPTS =
      "create table " + ScriptDbAdapter.TABLE_NAME +
      " (" + ScriptDbAdapter.KEY_ID + " integer primary key autoincrement, " +
      ScriptDbAdapter.KEY_PARENT_PROJECT_ID + " integer not null, " +
      ScriptDbAdapter.KEY_NAME + " text not null);";
  private static String DROP_STATEMENT_SCRIPTS = "DROP TABLE IF EXISTS " + ScriptDbAdapter.TABLE_NAME;

  /* - - - - -   Commands   - - - - - */
  private static final String CREATE_STATEMENT_COMMANDS =
      "create table " + CommandDbAdapter.TABLE_NAME +
      " (" + CommandDbAdapter.KEY_ID + " integer primary key autoincrement, " +
      CommandDbAdapter.KEY_PARENT_SCRIPT_ID + " integer not null," +
      CommandDbAdapter.KEY_TYPE + " text not null," +
      CommandDbAdapter.KEY_ORDER_INDEX + " integer not null," +
      CommandDbAdapter.KEY_POSITION_ID + " integer," +
      CommandDbAdapter.KEY_DELAY_MS + " integer," +
      CommandDbAdapter.KEY_GRIPPER_DISTANCE + " integer," +
      CommandDbAdapter.KEY_CUSTOM_COMMAND + " integer," +
      CommandDbAdapter.KEY_ANOTHER_SCRIPT_ID + " integer," +
      CommandDbAdapter.KEY_DISPLAY_TEXT + " text);";
  private static String DROP_STATEMENT_COMMANDS = "DROP TABLE IF EXISTS " +
      CommandDbAdapter.TABLE_NAME;
  
  DbOpenHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_STATEMENT_PROJECTS);
    db.execSQL(CREATE_STATEMENT_POSITIONS);
    db.execSQL(CREATE_STATEMENT_SCRIPTS);
    db.execSQL(CREATE_STATEMENT_COMMANDS);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(TAG, "Upgrading from version " + oldVersion + " to " + newVersion
        + ", which will destroy all old table(s).");
    db.execSQL(DROP_STATEMENT_PROJECTS);
    db.execSQL(DROP_STATEMENT_POSITIONS);
    db.execSQL(DROP_STATEMENT_SCRIPTS);
    db.execSQL(DROP_STATEMENT_COMMANDS);
    onCreate(db);
  }
}
