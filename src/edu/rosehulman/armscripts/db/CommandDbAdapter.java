package edu.rosehulman.armscripts.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Simple helper class to save positions to the SQLite db.
 *
 * @author fisherds@gmail.com (Dave Fisher)
 */
public class CommandDbAdapter {

  private static final String TAG = "CommandDbAdapter";
  public static final String TABLE_NAME = "commands";

  public static final String KEY_ID = "_id";
  public static final String KEY_PARENT_SCRIPT_ID = "parent_script_id";
  public static final String KEY_TYPE = "type";
  public static final String KEY_ORDER_INDEX = "order_index";
  
  // Individual value depending on the type (String or long)
  public static final String KEY_POSITION_ID = "position_id"; // long
  public static final String KEY_GRIPPER_DISTANCE = "gripper_distance"; // int (long)
  public static final String KEY_DELAY_MS = "delay_ms"; // int (long)
  public static final String KEY_CUSTOM_COMMAND = "custom_command"; // String
  public static final String KEY_ANOTHER_SCRIPT_ID = "another_script_id"; // long

  public static final String KEY_TYPE_POSITION = "type_position";
  public static final String KEY_TYPE_DELAY = "type_delay";
  public static final String KEY_TYPE_GRIPPER = "type_gripper";
  public static final String KEY_TYPE_CUSTOM = "type_custom";
  public static final String KEY_TYPE_SCRIPT = "type_script";
  
  private SQLiteOpenHelper mOpenHelper;
  private SQLiteDatabase mDb;

  /**
   * Possible types of commands used.
   */
  public enum Type {
    POSITION, DELAY, GRIPPER, CUSTOM, SCRIPT;
    
    public String toString() {
        String s = super.toString();
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    };
  }
  
  /**
   * Constructor - takes the context to allow the database to be opened/created
   */
  public CommandDbAdapter() {
  }

  /**
   * Open the position database. If it cannot be opened, try to create a new
   * instance of the database. If it cannot be created, throw an exception to
   * signal the failure
   * 
   * @return this (self reference, allowing this to be chained in an
   *         initialization call)
   * @throws SQLException
   *           if the database could be neither opened or created
   */
  public CommandDbAdapter open() throws SQLException {
    mOpenHelper = DbOpenHelper.getInstance();
    mDb = mOpenHelper.getWritableDatabase();
    return this;
  }

  public void close() {
    mOpenHelper.close();
  }
  
  /**
   * Create a new command using the information provided. If the command
   * is successfully created return the new id, otherwise return
   * a -1 to indicate failure.
   * 
   * @param parentScriptId the script id of this command
   * @return _id or -1 if failed
   */
  public long createPosition(long parentScriptId, Type type) {
      ContentValues initialValues = new ContentValues();
      initialValues.put(KEY_PARENT_SCRIPT_ID, parentScriptId);
      initialValues.put(KEY_TYPE, type.toString());
      
      // TODO: Add other fields.
      
      return mDb.insert(TABLE_NAME, null, initialValues);
  }
  
  /**
   * Delete the command with the given id
   * 
   * @param commandId id of project to delete
   * @return true if deleted, false otherwise
   */
  public boolean deleteCommand(long commandId) {
      return mDb.delete(TABLE_NAME, KEY_ID + "=" + commandId, null) > 0;
  }
  
  /**
   * Return a Cursor over the list of all positions in a given project.
   * 
   * @return Cursor over all positions in this project.
   */
  public Cursor fetchAllScriptCommands(long scriptId) {
    
    //TODO: update projection
    
      return mDb.query(TABLE_NAME,
          new String[] {KEY_ID, KEY_PARENT_SCRIPT_ID,},
          KEY_PARENT_SCRIPT_ID + "=" + scriptId, null, null, null, null);
  }

  /**
   * Update the position name only.
   * 
   * @param positionId id of note to update
   * @param name value for updated the project name
   * @return true if the project was successfully updated, false otherwise
   */
  public boolean updateCommandOrderIndex(long commandId, int newOrderIndex) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(KEY_ORDER_INDEX, newOrderIndex);
    return mDb.update(TABLE_NAME, contentValues, KEY_ID + "=" + commandId, null) > 0;    
  }  
}
