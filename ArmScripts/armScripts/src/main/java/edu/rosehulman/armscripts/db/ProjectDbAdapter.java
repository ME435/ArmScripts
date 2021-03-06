package edu.rosehulman.armscripts.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Simple helper class to save projects to the SQLite db.
 *
 * @author fisherds@gmail.com (Dave Fisher)
 */
public class ProjectDbAdapter {

  private static final String TAG = "ProjectDbAdapter";
  public static final String TABLE_NAME = "projects";

  public static final String KEY_ID = "_id";
  public static final String KEY_NAME = "name";

  private SQLiteOpenHelper mOpenHelper;
  private SQLiteDatabase mDb;

  /**
   * Constructor - takes the context to allow the database to be opened/created
   */
  public ProjectDbAdapter() {
  }

  /**
   * Open the projects database. If it cannot be opened, try to create a new
   * instance of the database. If it cannot be created, throw an exception to
   * signal the failure
   * 
   * @return this (self reference, allowing this to be chained in an
   *         initialization call)
   * @throws SQLException
   *           if the database could be neither opened or created
   */
  public ProjectDbAdapter open() throws SQLException {
    mOpenHelper = DbOpenHelper.getInstance();
    mDb = mOpenHelper.getWritableDatabase();
    return this;
  }

  public void close() {
    mOpenHelper.close();
  }
  
  /**
   * Create a new project using the name provided. If the project is
   * successfully created return the new projectId, otherwise return
   * a -1 to indicate failure.
   * 
   * @param name the project name
   * @return projectId or -1 if failed
   */
  public long createProject(String name) {
      ContentValues initialValues = new ContentValues();
      initialValues.put(KEY_NAME, name);
      return mDb.insert(TABLE_NAME, null, initialValues);
  }
  
  /**
   * Delete the project with the given projectId
   * 
   * @param projectId id of project to delete
   * @return true if deleted, false otherwise
   */
  public boolean deleteProject(long projectId) {
      return mDb.delete(TABLE_NAME, KEY_ID + "=" + projectId, null) > 0;
  }
  
  /**
   * Return a Cursor over the list of all projects in the database
   * 
   * @return Cursor over all projects
   */
  public Cursor fetchAllProjects() {
      return mDb.query(TABLE_NAME, new String[] {KEY_ID, KEY_NAME},
          null, null, null, null, null);
  }
  
  /**
   * Return a Cursor positioned at the project that matches the given projectId
   * 
   * @param projectId id of project to retrieve
   * @return Cursor positioned to matching project, if found
   * @throws SQLException if note could not be found/retrieved
   */
  public Cursor fetchProject(long projectId) throws SQLException {
      Cursor cursor =
          mDb.query(true, TABLE_NAME, new String[] {KEY_ID, KEY_NAME},
              KEY_ID + "=" + projectId, null, null, null, null, null);
      if (cursor != null) {
        cursor.moveToFirst();
      }
      return cursor;
  }

  /**
   * Update the project using the details provided. The note to be updated is
   * specified using the projectId, and it is altered to use the name
   * value passed in
   * 
   * @param projectId id of note to update
   * @param name value for updated the project name
   * @return true if the project was successfully updated, false otherwise
   */
  public boolean updateProject(long projectId, String name) {
      ContentValues args = new ContentValues();
      args.put(KEY_NAME, name);
      return mDb.update(TABLE_NAME, args, KEY_ID + "=" + projectId, null) > 0;
  }
  
  
}
