package edu.rosehulman.armscripts.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
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
  public static final long LARGEST_OPEN_GRIPPER_VALUE = 100;

  public static final String KEY_ID = "_id";
  public static final String KEY_PARENT_SCRIPT_ID = "parent_script_id";
  public static final String KEY_TYPE = "type";
  public static final String KEY_ORDER_INDEX = "order_index";

  // Individual value depending on the type (String or long)
  public static final String KEY_POSITION_ID = "position_id"; // long
  public static final String KEY_GRIPPER_DISTANCE = "gripper_distance"; // int
  public static final String KEY_DELAY_MS = "delay_ms"; // int
  public static final String KEY_CUSTOM_COMMAND = "custom_command"; // String
  public static final String KEY_ANOTHER_SCRIPT_ID = "another_script_id"; // long

  public static final String KEY_DISPLAY_TEXT = "display_text"; // long

  private static final String[] PROJECTION_ALL = new String[] { KEY_ID,
      KEY_PARENT_SCRIPT_ID, KEY_TYPE, KEY_ORDER_INDEX, KEY_POSITION_ID,
      KEY_DELAY_MS, KEY_GRIPPER_DISTANCE, KEY_CUSTOM_COMMAND,
      KEY_ANOTHER_SCRIPT_ID, KEY_DISPLAY_TEXT };

  private static final String[] PROJECTION_DISPLAY_TEXT = new String[] { KEY_ID, KEY_DISPLAY_TEXT };

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
   * Constructor - takes the context to allow the database to be
   * opened/created
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
   *             if the database could be neither opened or created
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
   * Create a new command using the information provided. If the command is
   * successfully created return the new id, otherwise return a -1 to indicate
   * failure.
   * 
   * @param parentScriptId
   *            the parent script for this command
   * @param type
   *            the type of the command
   * @param longValue
   *            overloaded value that might be used to fill the value of
   *            various commands
   * @param stringValue
   *            overloaded value that might be used to fill the value of the
   *            custom command
   * @return _id or -1 if failed
   */
  public long createCommand(long parentScriptId, Type type, long longValue,
      String stringValue) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_PARENT_SCRIPT_ID, parentScriptId);
    initialValues.put(KEY_TYPE, type.toString());
    switch (type) {
    case POSITION:
      initialValues.put(KEY_POSITION_ID, longValue);
      initialValues.put(KEY_DISPLAY_TEXT, "Position: " + stringValue);
      break;
    case DELAY:
      initialValues.put(KEY_DELAY_MS, longValue);
      initialValues.put(KEY_DISPLAY_TEXT, "Delay: " + longValue + " ms");
      break;
    case GRIPPER:
      if (longValue <= 0) {
        initialValues.put(KEY_GRIPPER_DISTANCE, longValue);
        initialValues.put(KEY_DISPLAY_TEXT, "Gripper: Close");
      } else if (longValue >= LARGEST_OPEN_GRIPPER_VALUE) {
        initialValues.put(KEY_GRIPPER_DISTANCE, LARGEST_OPEN_GRIPPER_VALUE);
        initialValues.put(KEY_DISPLAY_TEXT, "Gripper: Open");
      } else {
        initialValues.put(KEY_GRIPPER_DISTANCE, longValue);
        initialValues.put(KEY_DISPLAY_TEXT, "Gripper: " + longValue + "mm");
      }
      break;
    case CUSTOM:
      initialValues.put(KEY_CUSTOM_COMMAND, stringValue);
      initialValues.put(KEY_DISPLAY_TEXT, "Custom: " + stringValue);
      break;
    case SCRIPT:
      initialValues.put(KEY_ANOTHER_SCRIPT_ID, longValue);
      initialValues.put(KEY_DISPLAY_TEXT, "Script: " + stringValue);
      break;
    default:
      break;
    }
    Cursor cursor = mDb.query(TABLE_NAME, new String[] { KEY_ID },
        KEY_PARENT_SCRIPT_ID + "=" + parentScriptId, null, null, null,
        null);
    initialValues.put(KEY_ORDER_INDEX, cursor.getCount());
    return mDb.insert(TABLE_NAME, null, initialValues);
  }

  /**
   * Moves the command from one location to another.
   * 
   * @param scr
   * @param newOrderIndex
   */
  public void moveCommandFromTo(long scriptId, int fromOrderIndex,
      int toOrderIndex) {
    ArrayList<Long> commandIds = getCommandIdList(scriptId);
    if (fromOrderIndex > commandIds.size()
        || toOrderIndex > commandIds.size()) {
      Log.d(TAG, "Invalid move request.");
      return;
    }
    Long movingId = commandIds.remove(fromOrderIndex);
    commandIds.add(toOrderIndex, movingId);
    updateOrderIndicies(commandIds);
  }

  /**
   * Delete the command with the given id
   * 
   * @param commandId
   *            id of project to delete
   * @return true if deleted, false otherwise
   */
  public boolean deleteCommand(long commandId) {
    ArrayList<Long> commandIds = getCommandIdList(getParentScript(commandId));
    commandIds.remove(Long.valueOf(commandId));
    updateOrderIndicies(commandIds);
    return mDb.delete(TABLE_NAME, KEY_ID + "=" + commandId, null) > 0;
  }

  /**
   * Return a Cursor over the list of all commands in a given script.
   * 
   * @return Cursor over all commands in this script.
   */
  public Cursor fetchAllScriptCommands(long scriptId) {
    return mDb.query(TABLE_NAME, PROJECTION_ALL,
        KEY_PARENT_SCRIPT_ID + "=" + scriptId, null, null, null,
        KEY_ORDER_INDEX + " ASC");
  }

  /**
   * Return a Cursor over the list of all commands in a given script.
   * Only include the display text field.
   * 
   * @return Cursor over all commands in this script.
   */
  public Cursor fetchAllDisplayText(long scriptId) {
    return mDb.query(TABLE_NAME, PROJECTION_DISPLAY_TEXT,
        KEY_PARENT_SCRIPT_ID + "=" + scriptId, null, null, null,
        KEY_ORDER_INDEX + " ASC");
  }

  /**
   * Retrieves the parent script for the given command id.
   * 
   * @param commandId
   *            Command id to lookup
   * @return Parent script id
   */
  public long getParentScript(long commandId) {
    Cursor cursor = fetchCommand(commandId);
    int parentScriptColumn = cursor
        .getColumnIndexOrThrow(KEY_PARENT_SCRIPT_ID);
    return cursor.getLong(parentScriptColumn);
  }

  /**
   * Return a Cursor positioned at the project that matches the given
   * projectId
   * 
   * @param commandId
   *            id of command to retrieve
   * @return Cursor positioned to matching command, if found
   * @throws SQLException
   *             if note could not be found/retrieved
   */
  public Cursor fetchCommand(long commandId) throws SQLException {
    Cursor cursor = mDb.query(true, TABLE_NAME, PROJECTION_ALL,
        KEY_ID + "=" + commandId, null, null, null, null, null);
    if (cursor != null) {
      cursor.moveToFirst();
    }
    return cursor;
  }

  /**
   * Returns an ArrayList of the command ids in the script. This helper
   * function is used when deleting or moving commands. Instead of manually
   * adjusting the order index values this list is used then the index values
   * are written back. I imagine there is a slick mechanism for something like
   * this, but this solution seems simple enough (a bit inefficient).
   * 
   * @param scriptId
   *            The parent script id of all the commands in the list.
   * @return A list of all commands (in order) that share this parent script
   *         id.
   */
  private ArrayList<Long> getCommandIdList(long scriptId) {
    ArrayList<Long> result = new ArrayList<Long>();
    Cursor cursor = mDb.query(TABLE_NAME, new String[] { KEY_ID },
        KEY_PARENT_SCRIPT_ID + "=" + scriptId, null, null, null,
        KEY_ORDER_INDEX + " ASC");
    int idColumn = cursor.getColumnIndexOrThrow(KEY_ID);
    if (cursor != null && cursor.moveToFirst()) {
      do {
        result.add(cursor.getLong(idColumn));
      } while (cursor.moveToNext());
    }
    return result;
  }

  /**
   * Receives a List of command ids and updates the order of each command
   * based on the order in the list.
   * 
   * @param commandIdList
   *            List of command ids.
   */
  private void updateOrderIndicies(List<Long> commandIdList) {
    for (int i = 0; i < commandIdList.size(); i++) {
      updateCommandOrderIndex(commandIdList.get(i), i);
    }
  }

  /**
   * Update the command order only.
   * 
   * @param commandId
   *            id of command to update
   * @param newOrderIndex
   *            value for updated order index
   * @return true if the project was successfully updated, false otherwise
   */
  private boolean updateCommandOrderIndex(long commandId, int newOrderIndex) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(KEY_ORDER_INDEX, newOrderIndex);
    return mDb.update(TABLE_NAME, contentValues, KEY_ID + "=" + commandId,
        null) > 0;
  }
}

