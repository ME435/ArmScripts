package edu.rosehulman.armscripts;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ToggleButton;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import edu.rosehulman.armscripts.db.CommandDbAdapter;
import edu.rosehulman.armscripts.db.PositionDbAdapter;
import edu.rosehulman.armscripts.db.ScriptDbAdapter;

public class ScriptEditorFragment extends Fragment {

  private static final String TAG = ScriptEditorFragment.class.getSimpleName();
  private static final String DEFAULT_SCRIPT_NAME = "untitled script";

  /** Id of the parent project. */
  private long mParentProjectId;

  /** Reference to the SQLite database adapter. */
  private CommandDbAdapter mCommandDbAdapter;
  /** Adapter to display positions in the database. */
  private SimpleDragSortCursorAdapter mCommandListAdapter;
  /** Display of commands in this script. */
  private DragSortListView mCommandListView;
  /** Drag controller to allow editing or not. */
  private DragSortController mCommandDragController;

  private ScriptDbAdapter mScriptDbAdapter;
  private SimpleCursorAdapter mScriptSelectionCursorAdapter;
  private Spinner mScriptSelectionSpinner;

  private PositionDbAdapter mPositionDbAdapter;
  private SimpleCursorAdapter mPositionsCursorAdapter;
  private ListView mPositionsListView;

  private SimpleCursorAdapter mCallScriptCursorAdapter;
  private ListView mCallScriptListView;

  /**
   * Hacky solution to reuse the same dialog for creating AND renaming script.
   * Set this flag true before launching the dialog and it'll display a dialog
   * for renaming. Set this flag false then show the dialog and it'll display a
   * dialog for a new scipt. Hacky but easy.
   */
  private boolean mRenameScript = false;

  /** Id of the position item that is selected by the spinner. */
  private long mActiveScriptId = 0; // Default to 0 for non-selected.

  /**
   * Dialog used when naming a new position OR renaming an existing position.
   */
  private DialogFragment mCreateScriptDF = new DialogFragment() {
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      final Dialog dialog = new Dialog(getActivity());
      dialog.setContentView(R.layout.script_name_dialog);

      final EditText nameText = (EditText) dialog.findViewById(R.id.edittext_script_name);
      final Button confirmButton = (Button) dialog.findViewById(R.id.confirm_script_name_button);
      final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_script_name_button);
      if (mRenameScript) {
        dialog.setTitle("Edit the script name");
        Cursor scriptSelected = mScriptDbAdapter.fetchScript(mActiveScriptId);
        int nameColumn = scriptSelected.getColumnIndexOrThrow(ScriptDbAdapter.KEY_NAME);
        String name = scriptSelected.getString(nameColumn);
        nameText.setText(name);
        confirmButton.setText("Rename Script");
      } else {
        dialog.setTitle("Name the script");
        confirmButton.setText("Create script");
      }

      confirmButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          String scriptName = nameText.getText().toString();
          if (mRenameScript) {
            editScriptName(scriptName);
          } else {
            addScript(scriptName);
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

      // Consider: Project renaming is done action is different.
      nameText.setOnEditorActionListener(new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
          if (EditorInfo.IME_ACTION_DONE == actionId) {
            String scriptName = nameText.getText().toString();
            addScript(scriptName);
            dialog.dismiss();
          }
          return false;
        }
      });

      return dialog;
    };
  };

  /**
   * Dialog used when deleting a script.
   */
  private DialogFragment mDeleteScriptDF = new DialogFragment() {
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      final Dialog dialog = new Dialog(getActivity());
      dialog.setContentView(R.layout.delete_script_dialog);
      final Button confirmButton = (Button) dialog.findViewById(R.id.confirm_delete_script_button);
      final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_delete_script_button);
      // dialog.setTitle("Edit the script name");
      Cursor scriptSelected = mScriptDbAdapter.fetchScript(mActiveScriptId);
      int nameColumn = scriptSelected.getColumnIndexOrThrow(ScriptDbAdapter.KEY_NAME);
      String name = scriptSelected.getString(nameColumn);
      dialog.setTitle("Are you sure you wish to delete " + name + "?");

      confirmButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mScriptDbAdapter.deleteScript(mActiveScriptId);
          mActiveScriptId = 0;
          refreshSelectScriptCursor();
          refreshCallScriptCursor();
          dialog.dismiss();
        }
      });

      cancelButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          dialog.dismiss();
        }
      });

      return dialog;
    };
  };

  private DragSortListView.DropListener mDslvDropListener = new DragSortListView.DropListener() {
    @Override
    public void drop(int from, int to) {
      if (from != to) {
        Log.d(TAG, "Move from " + from + " to " + to);
        mCommandDbAdapter.moveCommandFromTo(mActiveScriptId, from, to);
        refreshCommandList();
      }
    }
  };
  
  private DragSortListView.RemoveListener mDslvRemoveListener = new DragSortListView.RemoveListener() {
    
    @Override
    public void remove(int which) {
      //mCommandDbAdapter.deleteCommand(which);
      long commandIdToRemove = mCommandListView.getItemIdAtPosition(which);
      Log.d(TAG, "Remove which = " + which + "   id = " + commandIdToRemove);
      mCommandDbAdapter.deleteCommand(commandIdToRemove);
      refreshCommandList();
      
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mScriptDbAdapter = new ScriptDbAdapter();
    mScriptDbAdapter.open();
    mCommandDbAdapter = new CommandDbAdapter();
    mCommandDbAdapter.open();
    mPositionDbAdapter = new PositionDbAdapter();
    mPositionDbAdapter.open();
    mParentProjectId = ((ProjectActivity) getActivity()).getCurrentProjectId();
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_scripts, container, false);

    // Get the active script. Make one if none exist.
    Cursor scriptsCursor = mScriptDbAdapter.fetchAllProjectScripts(mParentProjectId);
    if (scriptsCursor != null && scriptsCursor.moveToFirst()) {
      int scriptIdColumn = scriptsCursor.getColumnIndexOrThrow(ScriptDbAdapter.KEY_ID);
      mActiveScriptId = scriptsCursor.getLong(scriptIdColumn);
    } else {
      // No scripts in this project. Make one.
      mActiveScriptId = mScriptDbAdapter.createScript(mParentProjectId, DEFAULT_SCRIPT_NAME);
    }

    // Spinner to select a script.
    mScriptSelectionSpinner = (Spinner) view.findViewById(R.id.select_script_spinner);
    scriptsCursor = mScriptDbAdapter.fetchAllProjectScripts(mParentProjectId);
    String[] fromColumns = new String[] { ScriptDbAdapter.KEY_NAME };
    int[] toTextViews = new int[] { R.id.select_script_textview };
    mScriptSelectionCursorAdapter = new SimpleCursorAdapter(getActivity(),
        R.layout.select_script_item, scriptsCursor, fromColumns, toTextViews);
    mScriptSelectionSpinner.setAdapter(mScriptSelectionCursorAdapter);
    registerForContextMenu(mScriptSelectionSpinner);

    // Listener for the spinner.
    // When a new position is selected update the text views to display the
    // values.
    mScriptSelectionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        mActiveScriptId = id;
        updateCommandListSpinnerChanged();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
        mActiveScriptId = 0;
        updateCommandListSpinnerChanged();
      }
    });

    // Commands list view
    mCommandListView = (DragSortListView) view.findViewById(R.id.listview_script_commands);
    String[] cols = { CommandDbAdapter.KEY_DISPLAY_TEXT };
    int[] ids = { R.id.text };
    mCommandListAdapter = new SimpleDragSortCursorAdapter(getActivity(),
        R.layout.list_item_handle_left, null, cols, ids, 0);
    mCommandListView.setAdapter(mCommandListAdapter);
    registerForContextMenu(mCommandListView);

    Cursor cursor = mCommandDbAdapter.fetchAllScriptCommands(mActiveScriptId);
    mCommandListAdapter.changeCursor(cursor);
    mCommandListView.setDropListener(mDslvDropListener);
    mCommandListView.setRemoveListener(mDslvRemoveListener);
    mCommandDragController = buildController(mCommandListView);
    mCommandListView.setFloatViewManager(mCommandDragController);
    mCommandListView.setOnTouchListener(mCommandDragController);
    mCommandListView.setDragEnabled(false);

    ToggleButton editToggle = (ToggleButton) view.findViewById(R.id.toggle_button_edit_commands);
    editToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        mCommandDragController.setRemoveEnabled(isChecked);
        mCommandDragController.setSortEnabled(isChecked);
        mCommandListView.setDragEnabled(isChecked);

        // Setup the list and adapter.
        Cursor cursor = mCommandDbAdapter.fetchAllScriptCommands(mActiveScriptId);
        String[] cols = { CommandDbAdapter.KEY_DISPLAY_TEXT };
        int[] ids = { R.id.text };

        if (isChecked) {
          Log.d(TAG, "Enter edit mode.");
          mCommandListAdapter = new SimpleDragSortCursorAdapter(getActivity(),
              R.layout.list_item_click_remove, cursor, cols, ids, 0);
        } else {
          Log.d(TAG, "No editing.");
          mCommandListAdapter = new SimpleDragSortCursorAdapter(getActivity(),
              R.layout.list_item_handle_left, cursor, cols, ids, 0);
        }
        mCommandListView.setAdapter(mCommandListAdapter);
        mCommandListView.setDropListener(mDslvDropListener);
        mCommandListView.setRemoveListener(mDslvRemoveListener);
      }
    });

    // Position list view
    mPositionsListView = (ListView) view.findViewById(R.id.listview_positions);
    Cursor positionCursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
    String[] posColumns = new String[] { PositionDbAdapter.KEY_NAME };
    int[] posTextViews = new int[] { android.R.id.text1 };
    mPositionsCursorAdapter = new SimpleCursorAdapter(getActivity(),
        android.R.layout.simple_list_item_1, positionCursor, posColumns, posTextViews);
    mPositionsCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mPositionsListView.setAdapter(mPositionsCursorAdapter);

    mPositionsListView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long positionId) {
        Cursor posCursor = mPositionDbAdapter.fetchPosition(positionId);
        int nameCol = posCursor.getColumnIndexOrThrow(PositionDbAdapter.KEY_NAME);
        String posName = posCursor.getString(nameCol);
        mCommandDbAdapter.createCommand(mActiveScriptId, CommandDbAdapter.Type.POSITION,
            positionId, posName);
        refreshCommandList();
      }

    });

    // Delay EditText
    final EditText delayEditText = (EditText) view.findViewById(R.id.edittext_delay_time);
    delayEditText.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
          String delayText = delayEditText.getText().toString();
          long delayValue = 0;
          try {
            delayValue = Long.valueOf(delayText);
            mCommandDbAdapter.createCommand(mActiveScriptId, CommandDbAdapter.Type.DELAY,
                delayValue, "");
            refreshCommandList();
          } catch (Exception e) {
            Toast.makeText(getActivity(), "Invalid delay value", Toast.LENGTH_SHORT).show();
          }
          delayEditText.setText("");
        }
        return false;
      }
    });

    // Gripper EditText
    final EditText gripperEditText = (EditText) view.findViewById(R.id.gripper_edittext);
    gripperEditText.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
          String gripperText = gripperEditText.getText().toString();
          long gripperValue = 0;
          try {
            gripperValue = Long.valueOf(gripperText);
            mCommandDbAdapter.createCommand(mActiveScriptId, CommandDbAdapter.Type.GRIPPER,
                gripperValue, "");
            refreshCommandList();
          } catch (Exception e) {
            Toast.makeText(getActivity(), "Invalid gripper value", Toast.LENGTH_SHORT).show();
          }
          gripperEditText.setText("");
        }
        return false;
      }
    });

    // Gripper Open
    Button gripperOpenButton = (Button) view.findViewById(R.id.button_gripper_open);
    gripperOpenButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mCommandDbAdapter.createCommand(mActiveScriptId, CommandDbAdapter.Type.GRIPPER,
            CommandDbAdapter.LARGEST_OPEN_GRIPPER_VALUE, "");
        refreshCommandList();
      }
    });

    // Gripper Close
    Button gripperCloseButton = (Button) view.findViewById(R.id.button_gripper_close);
    gripperCloseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mCommandDbAdapter.createCommand(mActiveScriptId, CommandDbAdapter.Type.GRIPPER,
            0, "");
        refreshCommandList();
      }
    });

    // Custom command EditText
    final EditText customCommandEditText = (EditText) view
        .findViewById(R.id.edittext_custom_message);
    customCommandEditText.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
          String customCommandText = customCommandEditText.getText().toString();
          
            mCommandDbAdapter.createCommand(mActiveScriptId, CommandDbAdapter.Type.CUSTOM,
                0, customCommandText);
            refreshCommandList();
        }
        return false;
      }
    });

    // Call another script list view.
    mCallScriptListView = (ListView) view.findViewById(R.id.listview_other_scripts);
    Cursor callScriptCursor = mScriptDbAdapter.fetchAllOtherProjectScripts(mParentProjectId,
        mActiveScriptId);
    String[] callScriptColumns = new String[] { PositionDbAdapter.KEY_NAME };
    int[] callScriptTextViews = new int[] { android.R.id.text1 };
    mCallScriptCursorAdapter = new SimpleCursorAdapter(getActivity(),
        android.R.layout.simple_list_item_1, callScriptCursor, callScriptColumns,
        callScriptTextViews);
    mCallScriptListView.setAdapter(mCallScriptCursorAdapter);
    
    mCallScriptListView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View v, int position, long scriptId) {
        Cursor scriptCursor = mScriptDbAdapter.fetchScript(scriptId);
        int nameCol = scriptCursor.getColumnIndexOrThrow(PositionDbAdapter.KEY_NAME);
        String scriptName = scriptCursor.getString(nameCol);
        mCommandDbAdapter.createCommand(mActiveScriptId, CommandDbAdapter.Type.SCRIPT,
            scriptId, scriptName);
        refreshCommandList();
      }
    });
    
    return view;
  }

  private void updateCommandListSpinnerChanged() {
    refreshCommandList();
    refreshCallScriptCursor();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.scripts_options_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.create_script_menu_item:
      mRenameScript = false;
      mCreateScriptDF.show(getFragmentManager(), "create script");
      return true;
    case R.id.delete_script_menu_item:
      mDeleteScriptDF.show(getFragmentManager(), "delete script");
      return true;
    case R.id.edit_script_name_menu_item:
      mRenameScript = true;
      mCreateScriptDF.show(getFragmentManager(), "rename script");
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void refreshSelectScriptCursor() {
    Cursor cursor = mScriptDbAdapter.fetchAllProjectScripts(mParentProjectId);
    mScriptSelectionCursorAdapter.changeCursor(cursor);
  }

  private void refreshCallScriptCursor() {
    Cursor cursor = mScriptDbAdapter.fetchAllOtherProjectScripts(mParentProjectId, mActiveScriptId);
    mCallScriptCursorAdapter.changeCursor(cursor);
  }

  private void refreshCommandList() {
    Cursor cursor = mCommandDbAdapter.fetchAllDisplayText(mActiveScriptId);
    mCommandListAdapter.changeCursor(cursor);
  }

  /**
   * Create a context menu for the list view.
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflater = getActivity().getMenuInflater();
    if (v == mCommandListView) {
      inflater.inflate(R.menu.script_command_context_menu, menu);
    } else if (v == mScriptSelectionSpinner) {
      inflater.inflate(R.menu.select_script_context_menu, menu);
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
      // Note we delete the individual command without an are you sure box.
      mCommandDbAdapter.deleteCommand(info.id);
      refreshCommandList();
      return true;
    case R.id.script_command_context_edit:
      Log.d(TAG, "You wish to edit this command.  Good for you.");
      return true;
    case R.id.select_script_context_delete:
      // To delete the script we use an Are you sure box.
      mDeleteScriptDF.show(getFragmentManager(), "delete script");
      return true;
    case R.id.select_script_context_edit:
      mRenameScript = true;
      mCreateScriptDF.show(getFragmentManager(), "rename script");
      return true;
    }
    return super.onContextItemSelected(item);
  }

  /**
   * Called in onCreateView. Override this to provide a custom
   * DragSortController.
   */
  public DragSortController buildController(DragSortListView dslv) {
    DragSortController controller = new DragSortController(dslv);
    controller.setDragHandleId(R.id.drag_handle);
    controller.setClickRemoveId(R.id.click_remove);
    controller.setRemoveEnabled(false);
    controller.setSortEnabled(false);
    controller.setDragInitMode(DragSortController.ON_DOWN);
    controller.setRemoveMode(DragSortController.CLICK_REMOVE);
    return controller;
  }

  private void addScript(String scriptName) {
    long scriptId = mScriptDbAdapter.createScript(mParentProjectId, scriptName);
    Cursor cursor = mScriptDbAdapter.fetchAllProjectScripts(mParentProjectId);
    mScriptSelectionCursorAdapter.changeCursor(cursor);

    // Set the spinner to the newest position
    for (int i = 0; i < mScriptSelectionSpinner.getCount(); i++) {
      long itemIdAtScript = mScriptSelectionSpinner.getItemIdAtPosition(i);
      if (itemIdAtScript == scriptId) {
        mScriptSelectionSpinner.setSelection(i);
        break;
      }
    }
  }

  private void editScriptName(String scriptName) {
    mScriptDbAdapter.updateScriptName(mActiveScriptId, scriptName);
    Cursor cursor = mScriptDbAdapter.fetchAllProjectScripts(mParentProjectId);
    mScriptSelectionCursorAdapter.changeCursor(cursor);
  }

  @Override
  public void onPause() {
    super.onPause();
  }
}
