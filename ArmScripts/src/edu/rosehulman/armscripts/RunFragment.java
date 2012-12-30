package edu.rosehulman.armscripts;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import edu.rosehulman.armscripts.db.CommandDbAdapter;
import edu.rosehulman.armscripts.db.ScriptDbAdapter;

public class RunFragment extends Fragment {

  public static final String TAG = RunFragment.class.getSimpleName();
  
  /** 
   * Potentially there should be short delay before running the first
   * command in a script to let all commands get loaded before starting.
   * No sure.  Just set this value to 0 if it's unnecessary.
   */
  private static final int INITIAL_SCRIPT_DELAY_MS = 10;

  /** Id of the parent project. */
  private long mParentProjectId;
  private ScriptDbAdapter mScriptDbAdapter;
  private CommandDbAdapter mCommandDbAdapter;

  private ListView mConsoleMessagesListView;
  private ArrayList<ConsoleMessage> mConsoleMessages;
  private ConsoleMessageListAdapter mConsoleMessageListAdapter;

  private ListView mScriptListView;
  private SimpleCursorAdapter mScriptCursorAdapter;

  /** Scripts can call other scripts, but keep track to help users avoid circular calls. */
  private HashSet<Long> mScriptsAlreadyCalled;
  
  private Handler mCommandHandler;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mScriptDbAdapter = new ScriptDbAdapter();
    mScriptDbAdapter.open();
    mCommandDbAdapter = new CommandDbAdapter();
    mCommandDbAdapter.open();
    mConsoleMessages = new ArrayList<ConsoleMessage>();
    mConsoleMessageListAdapter = new ConsoleMessageListAdapter(getActivity(),
        R.layout.list_item_console_message, mConsoleMessages);
    mParentProjectId = ((ProjectActivity) getActivity()).getCurrentProjectId();
    
    mCommandHandler = new Handler();
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_run, container, false);

    mConsoleMessagesListView = (ListView) view.findViewById(R.id.listview_message_console);
    mConsoleMessagesListView.setAdapter(mConsoleMessageListAdapter);

    mScriptListView = (ListView) view.findViewById(R.id.listview_script_to_run);
    String[] fromColumns = new String[] { ScriptDbAdapter.KEY_NAME };
    int[] toTextView = new int[] { R.id.textview_script_to_run_name };
    Cursor scriptNames = mScriptDbAdapter.fetchAllProjectScripts(mParentProjectId);
    mScriptCursorAdapter = new SimpleCursorAdapter(getActivity(),
        R.layout.list_item_runable_script, scriptNames, fromColumns, toTextView);
    mScriptListView.setAdapter(mScriptCursorAdapter);

    mScriptListView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long scriptId) {
        mScriptsAlreadyCalled = new HashSet<Long>();
        mScriptsAlreadyCalled.add(scriptId);
        executeScriptId(scriptId, INITIAL_SCRIPT_DELAY_MS);
      }

    });

    return view;
  }

  private int executeScriptId(long scriptId, int delayUntilNextCommandExecutesInMs) {
//    Cursor scriptCursor = mScriptDbAdapter.fetchScript(scriptId);
//    int nameColumn = scriptCursor.getColumnIndexOrThrow(ScriptDbAdapter.KEY_NAME);
//    String scriptName = scriptCursor.getString(nameColumn);
//    Log.d(TAG, "Execute " + scriptName);
    
    Cursor allCommandsCursor = mCommandDbAdapter.fetchAllScriptCommands(scriptId);
    // Consider: You are loading these column ids every script, but they don't change. :)
    int typeColumn = allCommandsCursor.getColumnIndexOrThrow(CommandDbAdapter.KEY_TYPE);
    int positionIdColumn = allCommandsCursor.getColumnIndexOrThrow(CommandDbAdapter.KEY_POSITION_ID);
    int delayColumn = allCommandsCursor.getColumnIndexOrThrow(CommandDbAdapter.KEY_DELAY_MS);
    int gripperDistanceColumn = allCommandsCursor.getColumnIndexOrThrow(CommandDbAdapter.KEY_GRIPPER_DISTANCE);
    int customCommandColumn = allCommandsCursor.getColumnIndexOrThrow(CommandDbAdapter.KEY_CUSTOM_COMMAND);
    int anotherScriptIdColumn = allCommandsCursor.getColumnIndexOrThrow(CommandDbAdapter.KEY_ANOTHER_SCRIPT_ID);
    if (allCommandsCursor != null && allCommandsCursor.moveToFirst()) {
      do {
        String type = allCommandsCursor.getString(typeColumn);
        if (type.equals(CommandDbAdapter.Type.POSITION.toString())) {
          executePositionCommand(allCommandsCursor.getLong(positionIdColumn), delayUntilNextCommandExecutesInMs);
        } else if (type.equals(CommandDbAdapter.Type.DELAY.toString())) {
          delayUntilNextCommandExecutesInMs += allCommandsCursor.getInt(delayColumn);
        } else if (type.equals(CommandDbAdapter.Type.GRIPPER.toString())) {
          executeGripperCommand(allCommandsCursor.getInt(gripperDistanceColumn), delayUntilNextCommandExecutesInMs);
        } else if (type.equals(CommandDbAdapter.Type.CUSTOM.toString())) {
          executeCustomCommand(allCommandsCursor.getString(customCommandColumn), delayUntilNextCommandExecutesInMs);
        } else if (type.equals(CommandDbAdapter.Type.SCRIPT.toString())) {
          long nextScriptId = allCommandsCursor.getLong(anotherScriptIdColumn);
          if (!mScriptsAlreadyCalled.contains(nextScriptId)) {
            mScriptsAlreadyCalled.add(nextScriptId);
            delayUntilNextCommandExecutesInMs = executeScriptId(nextScriptId, delayUntilNextCommandExecutesInMs);
            mScriptsAlreadyCalled.remove(nextScriptId);
          }
        } else {
          Log.w(TAG, "Attempt to execute and unknown command type.");
        }
      } while (allCommandsCursor.moveToNext());
    }
    return delayUntilNextCommandExecutesInMs;
  }

  private void executeCustomCommand(final String customCommandString, int delayPostingForMs) {  
    mCommandHandler.postDelayed(new Runnable(){
      @Override
      public void run() {
        ConsoleMessage command = new ConsoleMessage(customCommandString, true);
        mConsoleMessages.add(command);
        mConsoleMessageListAdapter.notifyDataSetChanged();
      }
    }, delayPostingForMs);
    
  }

  private void executeGripperCommand(final int gripperDistanceMm, int delayPostingForMs) {    
    mCommandHandler.postDelayed(new Runnable(){
      @Override
      public void run() {
        ConsoleMessage command = new ConsoleMessage("GRIPPER " + gripperDistanceMm, true);
        mConsoleMessages.add(command);
        mConsoleMessageListAdapter.notifyDataSetChanged();
      }
    }, delayPostingForMs);
  }

  private void executePositionCommand(final long positionId, int delayPostingForMs) {    
    mCommandHandler.postDelayed(new Runnable(){
      @Override
      public void run() {
        ConsoleMessage command = new ConsoleMessage("POSITION " + " TBD " + positionId, true);
        mConsoleMessages.add(command);
        mConsoleMessageListAdapter.notifyDataSetChanged();
        
      }
    }, delayPostingForMs);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.run_options_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.clear_console_menu_item:
      mConsoleMessages.clear();
      mConsoleMessageListAdapter.notifyDataSetChanged();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  @Override
  public void onPause() {
    super.onPause();
  }
}
