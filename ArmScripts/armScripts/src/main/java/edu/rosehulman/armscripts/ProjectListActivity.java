package edu.rosehulman.armscripts;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import edu.rosehulman.armscripts.db.DbOpenHelper;
import edu.rosehulman.armscripts.db.ProjectDbAdapter;

/**
 * Activity that displays a list view of projects.
 * 
 * @author Dave Fisher
 */
public class ProjectListActivity extends ListActivity {

  public static final String TAG = "ProjectList";
  public static final String EXTRA_PROJECT_ID = "extra_project_id";
  /** Dialog ID for adding and editing projects (one dialog for both tasks). */
  private static final int PROJECT_NAME_DIALOG_ID = 1;
  private static final int PROJECT_DELETE_DIALOG_ID = 2;

  /**
   * Constant to indicate that no row is selected for editing. Used when adding
   * a new project.
   */
  public static final long NO_ID_SELECTED = -1;

  /** Index of the project / row selected. */
  private long mSelectedId = NO_ID_SELECTED;

  /** Reference to the SQLite database adapter. */
  private ProjectDbAdapter mProjectDbAdapter;

  /** Adapter to display projects in the database. */
  private SimpleCursorAdapter mProjectAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_project_list);

    DbOpenHelper.createInstance(this);  // Must run before any DbAdapters are made.
    mProjectDbAdapter = new ProjectDbAdapter();
    mProjectDbAdapter.open();

    Cursor cursor = mProjectDbAdapter.fetchAllProjects();
    // No projects yet. Welcome screen is up.
    Button getStartedButton = (Button) findViewById(R.id.get_started_button);
    getStartedButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mSelectedId = NO_ID_SELECTED;
        showDialog(PROJECT_NAME_DIALOG_ID);
      }
    });
    // Make the adapter even if the cursor is empty to prepare for first item.
    int viewResourceId = R.layout.project_list_item;
    String[] fromColumns = new String[] { ProjectDbAdapter.KEY_NAME };
    int[] toTextViews = new int[] { R.id.textview_name };
    mProjectAdapter = new SimpleCursorAdapter(this, viewResourceId, cursor, fromColumns,
        toTextViews);
    setListAdapter(mProjectAdapter);
    registerForContextMenu(getListView());
  }

  /**
   * ListActivity sets up the onItemClick listener for the list view
   * automatically via this function
   */
  @Override
  protected void onListItemClick(ListView listView, View selectedView, int position, long id) {
    super.onListItemClick(listView, selectedView, position, id);
    mSelectedId = id;
    Intent projectIntent = new Intent(ProjectListActivity.this, ProjectActivity.class);
    projectIntent.putExtra(EXTRA_PROJECT_ID, id);
    startActivity(projectIntent);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_project_list, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.add_project:
      mSelectedId = NO_ID_SELECTED;
      showDialog(PROJECT_NAME_DIALOG_ID);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Create a context menu for the list view.
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflator = getMenuInflater();
    if (v == getListView()) {
      inflator.inflate(R.menu.project_list_view_context_menu, menu);
    }
  }

  /**
   * Standard listener for the context menu item selections
   */
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    mSelectedId = info.id;
    switch (item.getItemId()) {
    case R.id.menu_item_list_view_delete:
      showDialog(PROJECT_DELETE_DIALOG_ID);
      return true;
    case R.id.menu_item_list_view_edit:
      showDialog(PROJECT_NAME_DIALOG_ID);
      return true;
    }
    return super.onContextItemSelected(item);
  }

  private void addProject(String projectName) {
    long projectId = mProjectDbAdapter.createProject(projectName);
    Cursor cursor = mProjectDbAdapter.fetchAllProjects();
    mProjectAdapter.changeCursor(cursor);

    Intent projectIntent = new Intent(ProjectListActivity.this, ProjectActivity.class);
    projectIntent.putExtra(EXTRA_PROJECT_ID, projectId);
    startActivity(projectIntent);
  }

  private void editProjectName(String projectName) {
    mProjectDbAdapter.updateProject(mSelectedId, projectName);
    Cursor cursor = mProjectDbAdapter.fetchAllProjects();
    mProjectAdapter.changeCursor(cursor);
  }

  /**
   * Called when the activity is removed from memory (placeholder for later)
   */
  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  // ======================================================================
  // Dialog for adding and updating Scores
  // ======================================================================

  /**
   * Create the dialog if it has never been launched. Uses a custom dialog
   * layout
   */
  @Override
  protected Dialog onCreateDialog(int id) {
    super.onCreateDialog(id);
    final Dialog dialog = new Dialog(this);
    switch (id) {
    case PROJECT_NAME_DIALOG_ID:
      dialog.setContentView(R.layout.project_name_dialog);
      dialog.setTitle("New Project");

      final EditText nameText = (EditText) dialog.findViewById(R.id.edittext_project_name);
      final Button confirmButton = (Button) dialog.findViewById(R.id.confirm_project_name_button);
      final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_project_name_button);

      confirmButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          String projectName = nameText.getText().toString();
          if (mSelectedId == NO_ID_SELECTED) {
            addProject(projectName);
          } else {
            editProjectName(projectName);
          }
          dialog.dismiss();
        }
      });

      cancelButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          dialog.dismiss();
        }
      });
      break;
    case PROJECT_DELETE_DIALOG_ID:
      dialog.setContentView(R.layout.delete_project_dialog);
      Cursor cursorForSelectedProject = mProjectDbAdapter.fetchProject(mSelectedId);
      int nameColumn = cursorForSelectedProject.getColumnIndexOrThrow(ProjectDbAdapter.KEY_NAME);
      String selectedProjectName = cursorForSelectedProject.getString(nameColumn);
      dialog.setTitle("Delete " + selectedProjectName + "?");
      
      final Button confirmDeleteButton = (Button) dialog.findViewById(R.id.confirm_delete_project_button);
      final Button cancelDeleteButton = (Button) dialog.findViewById(R.id.cancel_delete_project_button);

      confirmDeleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mProjectDbAdapter.deleteProject(mSelectedId);
          Cursor cursor = mProjectDbAdapter.fetchAllProjects();
          mProjectAdapter.changeCursor(cursor);
          dialog.dismiss();
        }
      });

      cancelDeleteButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          dialog.dismiss();
        }
      });
      break;
    default:
      break;
    }

    return dialog;
  }

  /**
   * Update the dialog with appropriate text before presenting to the user
   */
  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    super.onPrepareDialog(id, dialog);
    switch (id) {
    case PROJECT_NAME_DIALOG_ID:
      final EditText nameText = (EditText) dialog.findViewById(R.id.edittext_project_name);
      final Button confirmButton = (Button) dialog.findViewById(R.id.confirm_project_name_button);
      if (mSelectedId == NO_ID_SELECTED) {
        dialog.setTitle("New Project");
        confirmButton.setText("Create Project");
        nameText.setText("");
      } else {
        dialog.setTitle("Rename Project");
        confirmButton.setText("Update");
        Cursor cursorForSelectedProject = mProjectDbAdapter.fetchProject(mSelectedId);
        int nameColumn = cursorForSelectedProject.getColumnIndexOrThrow(ProjectDbAdapter.KEY_NAME);
        String selectedProjectName = cursorForSelectedProject.getString(nameColumn);
        nameText.setText(selectedProjectName);
      }
      break;
    }
  }
}
