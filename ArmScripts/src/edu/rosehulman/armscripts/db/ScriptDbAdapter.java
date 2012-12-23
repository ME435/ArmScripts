package edu.rosehulman.armscripts.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Simple helper class to save positions to the SQLite db.
 *
 * @author fisherds@gmail.com (Dave Fisher)
 */
public class ScriptDbAdapter {

  private static final String TAG = "ScriptDbAdapter";
  public static final String TABLE_NAME = "scripts";

  public static final String KEY_ID = "_id";
  public static final String KEY_PARENT_PROJECT_ID = "parent_project_id";
  public static final String KEY_NAME = "name";

  private SQLiteOpenHelper mOpenHelper;
  private SQLiteDatabase mDb;

  /**
   * Constructor - takes the context to allow the database to be opened/created
   */
  public ScriptDbAdapter() {
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
  public ScriptDbAdapter open() throws SQLException {
//    mOpenHelper = new ScriptDbOpenHelper(mContext);
    mOpenHelper = DbOpenHelper.getInstance();
    mDb = mOpenHelper.getWritableDatabase();
    return this;
  }

  public void close() {
    mOpenHelper.close();
  }
  
  /**
   * Create a new position using the information provided. If the position
   * is successfully created return the new id, otherwise return
   * a -1 to indicate failure.
   * 
   * @param parentProjectId the parent project id
   * @param name the script name
   * @return _id or -1 if failed
   */
  public long createScript(long parentProjectId, String name) {
      ContentValues initialValues = new ContentValues();
      initialValues.put(KEY_PARENT_PROJECT_ID, parentProjectId);
      initialValues.put(KEY_NAME, name);
      return mDb.insert(TABLE_NAME, null, initialValues);
  }
  
  /**
   * Delete the script with the given id
   * 
   * @param id id of script to delete
   * @return true if deleted, false otherwise
   */
  public boolean deleteScript(long projectId) {
      return mDb.delete(TABLE_NAME, KEY_ID + "=" + projectId, null) > 0;
  }
  
  /**
   * Return a Cursor over the list of all positions in a given project.
   * 
   * @return Cursor over all positions in this project.
   */
  public Cursor fetchAllProjectScripts(long projectId) {
      return mDb.query(TABLE_NAME,
          new String[] {KEY_ID, KEY_PARENT_PROJECT_ID, KEY_NAME},
          KEY_PARENT_PROJECT_ID + "=" + projectId, null, null, null, null);
  }
  
  /**
   * Return a Cursor over the list of all script in a given project omitting
   * the active script.
   * 
   * @return Cursor over all positions in this project.
   */
  public Cursor fetchAllOtherProjectScripts(long projectId, long activeScriptId) {
      return mDb.query(TABLE_NAME,
          new String[] {KEY_ID, KEY_PARENT_PROJECT_ID, KEY_NAME},
          KEY_PARENT_PROJECT_ID + "=" + projectId + " AND " +
          KEY_ID + "!=" + activeScriptId, null, null, null, null);
  }
  
  /**
   * Return a Cursor positioned at the position that matches the given position id
   * 
   * @param positionId id of position to retrieve
   * @return Cursor positioned to matching position, if found
   * @throws SQLException if note could not be found/retrieved
   */
  public Cursor fetchScript(long positionId) throws SQLException {
      Cursor cursor =
          mDb.query(true, TABLE_NAME, 
              new String[] {KEY_ID, KEY_PARENT_PROJECT_ID, KEY_NAME},
              KEY_ID + "=" + positionId, null, null, null, null, null);
      if (cursor != null) {
        cursor.moveToFirst();
      }
      return cursor;
  }

  /**
   * Update the position using the details provided.
   * 
   * @param parentProjectId the project name
   * @param name the script name
   * @return true if the position was successfully updated, false otherwise
   */
  public boolean updateScript(long positionId, long parentProjectId, String name,
      int joint1, int joint2, int joint3, int joint4, int joint5) {
      ContentValues contentValues = new ContentValues();
      contentValues.put(KEY_PARENT_PROJECT_ID, parentProjectId);
      contentValues.put(KEY_NAME, name);
      return mDb.update(TABLE_NAME, contentValues, KEY_ID + "=" + positionId, null) > 0;
  }

  /**
   * Update the position name only.
   * 
   * @param positionId id of note to update
   * @param name value for updated the project name
   * @return true if the project was successfully updated, false otherwise
   */
  public boolean updateScriptName(long positionId, String name) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(KEY_NAME, name);
    return mDb.update(TABLE_NAME, contentValues, KEY_ID + "=" + positionId, null) > 0;    
  }
  
  
}
