package edu.rosehulman.armscripts;

import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import edu.rosehulman.armscripts.db.CommandDbAdapter;

public class ScriptEditorFragment extends Fragment {

  private static final String TAG = ScriptEditorFragment.class.getSimpleName();

  /** Id of the parent project. */
  private long mParentProjectId;

  /** Reference to the SQLite database adapter. */
  private CommandDbAdapter mCommandDbAdapter;

  /** Adapter to display positions in the database. */
  private SimpleDragSortCursorAdapter mCommandListAdapter;

  /** Display of commands in this script. */
  private DragSortListView mCommandListView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCommandDbAdapter = new CommandDbAdapter();
    mCommandDbAdapter.open();
    mParentProjectId = ((ProjectActivity) getActivity()).getCurrentProjectId();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_scripts, container, false);
    mCommandListView = (DragSortListView) view.findViewById(R.id.listview_script_commands);

    String[] cols = { CommandDbAdapter.KEY_ORDER_INDEX, CommandDbAdapter.KEY_DISPLAY_TEXT };
    int[] ids = { R.id.temp_order, R.id.text };
    mCommandListAdapter = new SimpleDragSortCursorAdapter(getActivity(),
        R.layout.list_item_handle_left, null, cols, ids, 0);
    mCommandListView.setAdapter(mCommandListAdapter);
    registerForContextMenu(mCommandListView);

    // mCommandDbAdapter.createCommand(mParentProjectId,
    // CommandDbAdapter.Type.POSITION, 20, "");
    // mCommandDbAdapter.createCommand(mParentProjectId,
    // CommandDbAdapter.Type.POSITION, 21, "");
    // mCommandDbAdapter.createCommand(mParentProjectId,
    // CommandDbAdapter.Type.POSITION, 22, "");
    // mCommandDbAdapter.createCommand(mParentProjectId,
    // CommandDbAdapter.Type.POSITION, 23, "");
    // mCommandDbAdapter.createCommand(mParentProjectId,
    // CommandDbAdapter.Type.GRIPPER, 24, "");
    // mCommandDbAdapter.createCommand(mParentProjectId,
    // CommandDbAdapter.Type.GRIPPER, 25, "");
    // mCommandDbAdapter.createCommand(mParentProjectId,
    // CommandDbAdapter.Type.GRIPPER, 26, "");
    Cursor cursor = mCommandDbAdapter.fetchAllScriptCommands(mParentProjectId);
    mCommandListAdapter.changeCursor(cursor);

    mCommandListView.setDropListener(new DragSortListView.DropListener() {
      @Override
      public void drop(int from, int to) {
        if (from != to) {
          Log.d(TAG, "Move from " + from + " to " + to);
          mCommandDbAdapter.moveCommandFromTo(mParentProjectId, from, to);
          Cursor cursor = mCommandDbAdapter.fetchAllScriptCommands(mParentProjectId);
          mCommandListAdapter.changeCursor(cursor);
        }
      }
    });
    return view;
  }

  /**
   * Create a context menu for the list view.
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflator = getActivity().getMenuInflater();
    if (v == mCommandListView) {
      inflator.inflate(R.menu.script_command_context_menu, menu);
    }
  }

  /**
   * Standard listener for the context menu item selections
   */
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch (item.getItemId()) {
    case R.id.script_command_context_delete:
      mCommandDbAdapter.deleteCommand(info.id);
      Cursor cursor = mCommandDbAdapter.fetchAllScriptCommands(mParentProjectId);
      mCommandListAdapter.changeCursor(cursor);
      return true;
    case R.id.script_command_context_edit:
      // mSelectedId = info.id;
      // showDialog(DIALOG_ID);
      return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  public void onPause() {
    super.onPause();
  }
}
