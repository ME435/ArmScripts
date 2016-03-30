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
public class PositionDbAdapter {

  private static final String TAG = "PositionDbAdapter";
  public static final String TABLE_NAME = "positions";

  public static final String KEY_ID = "_id";
  public static final String KEY_PARENT_PROJECT_ID = "parent_project_id";
  public static final String KEY_NAME = "name";
  public static final String KEY_JOINT_1 = "joint_1";
  public static final String KEY_JOINT_2 = "joint_2";
  public static final String KEY_JOINT_3 = "joint_3";
  public static final String KEY_JOINT_4 = "joint_4";
  public static final String KEY_JOINT_5 = "joint_5";

  private SQLiteOpenHelper mOpenHelper;
  private SQLiteDatabase mDb;
  
  /**
   * Constructor - takes the context to allow the database to be opened/created
   */
  public PositionDbAdapter() {
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
  public PositionDbAdapter open() throws SQLException {
//    mOpenHelper = new PositionDbOpenHelper(mContext);
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
   * @param parentProjectId the project name
   * @param name the project name
   * @param joint1 the value for joint1
   * @param joint2 the value for joint2
   * @param joint3 the value for joint3
   * @param joint4 the value for joint4
   * @param joint5 the value for joint5
   * @return _id or -1 if failed
   */
  public long createPosition(long parentProjectId, String name,
      int joint1, int joint2, int joint3, int joint4, int joint5) {
      ContentValues initialValues = new ContentValues();
      initialValues.put(KEY_PARENT_PROJECT_ID, parentProjectId);
      initialValues.put(KEY_NAME, name);
      initialValues.put(KEY_JOINT_1, joint1);
      initialValues.put(KEY_JOINT_2, joint2);
      initialValues.put(KEY_JOINT_3, joint3);
      initialValues.put(KEY_JOINT_4, joint4);
      initialValues.put(KEY_JOINT_5, joint5);
      return mDb.insert(TABLE_NAME, null, initialValues);
  }
  
  /**
   * Delete the project with the given projectId
   * 
   * @param projectId id of project to delete
   * @return true if deleted, false otherwise
   */
  public boolean deletePosition(long projectId) {
      return mDb.delete(TABLE_NAME, KEY_ID + "=" + projectId, null) > 0;
  }
  
  /**
   * Return a Cursor over the list of all positions in a given project.
   * 
   * @return Cursor over all positions in this project.
   */
  public Cursor fetchAllProjectPositions(long projectId) {
      return mDb.query(TABLE_NAME,
          new String[] {KEY_ID, KEY_PARENT_PROJECT_ID, KEY_NAME,
          KEY_JOINT_1, KEY_JOINT_2, KEY_JOINT_3, KEY_JOINT_4, KEY_JOINT_5},
          KEY_PARENT_PROJECT_ID + "=" + projectId, null, null, null, null);
  }
  
  /**
   * Return a Cursor positioned at the position that matches the given position id
   * 
   * @param positionId id of position to retrieve
   * @return Cursor positioned to matching position, if found
   * @throws SQLException if note could not be found/retrieved
   */
  public Cursor fetchPosition(long positionId) throws SQLException {
      Cursor cursor =
          mDb.query(true, TABLE_NAME, 
              new String[] {KEY_ID, KEY_PARENT_PROJECT_ID, KEY_NAME,
              KEY_JOINT_1, KEY_JOINT_2, KEY_JOINT_3, KEY_JOINT_4, KEY_JOINT_5},
              KEY_ID + "=" + positionId, null, null, null, null, null);
      if (cursor != null) {
        cursor.moveToFirst();
      }
      return cursor;
  }

  /**
   * Update the position using the details provided.
   * 
   * @param positionId id of position to update
   * @param parentProjectId the project name
   * @param name the project name
   * @param joint1 the value for joint1
   * @param joint2 the value for joint2
   * @param joint3 the value for joint3
   * @param joint4 the value for joint4
   * @param joint5 the value for joint5
   * @return true if the position was successfully updated, false otherwise
   */
  public boolean updatePosition(long positionId, long parentProjectId, String name,
      int joint1, int joint2, int joint3, int joint4, int joint5) {
      ContentValues contentValues = new ContentValues();
      contentValues.put(KEY_PARENT_PROJECT_ID, parentProjectId);
      contentValues.put(KEY_NAME, name);
      contentValues.put(KEY_JOINT_1, joint1);
      contentValues.put(KEY_JOINT_2, joint2);
      contentValues.put(KEY_JOINT_3, joint3);
      contentValues.put(KEY_JOINT_4, joint4);
      contentValues.put(KEY_JOINT_5, joint5);
      return mDb.update(TABLE_NAME, contentValues, KEY_ID + "=" + positionId, null) > 0;
  }

  /**
   * Update the position name only.
   * 
   * @param positionId id of position to update
   * @param name value for updated the project name
   * @return true if the project was successfully updated, false otherwise
   */
  public boolean updatePositionName(long positionId, String name) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(KEY_NAME, name);
    return mDb.update(TABLE_NAME, contentValues, KEY_ID + "=" + positionId, null) > 0;    
  }

  /**
   * Update the position using the details provided.
   * 
   * @param positionId id of position to update
   * @param joint1 the value for joint1
   * @param joint2 the value for joint2
   * @param joint3 the value for joint3
   * @param joint4 the value for joint4
   * @param joint5 the value for joint5
   * @return true if the position was successfully updated, false otherwise
   */
  public boolean updatePositionJointValues(long positionId,
      int joint1, int joint2, int joint3, int joint4, int joint5) {
      ContentValues contentValues = new ContentValues();
      contentValues.put(KEY_JOINT_1, joint1);
      contentValues.put(KEY_JOINT_2, joint2);
      contentValues.put(KEY_JOINT_3, joint3);
      contentValues.put(KEY_JOINT_4, joint4);
      contentValues.put(KEY_JOINT_5, joint5);
      return mDb.update(TABLE_NAME, contentValues, KEY_ID + "=" + positionId, null) > 0;
  }
  
  
}
