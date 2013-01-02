package edu.rosehulman.armscripts;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


public class ProjectActivity extends AccessoryActivity implements ActionBar.TabListener {

  private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
  private static final int POSITIONS_TAB_INDEX = 0;
  private static final int SCRIPT_EDITOR_INDEX = 1;
  private static final int RUN_INDEX = 2;
  
  private RunFragment mRunFragment = null;
  
  private long mCurrentProjectId;
  public long getCurrentProjectId() {
    return mCurrentProjectId;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_project);
    
    mCurrentProjectId = getIntent().getLongExtra(ProjectListActivity.EXTRA_PROJECT_ID, 1);

    // Set up the action bar.
    final ActionBar actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    actionBar.setHomeButtonEnabled(true);
    actionBar.setDisplayHomeAsUpEnabled(true);

    // Add tabs to the action bar.
    actionBar.addTab(actionBar.newTab().setText(R.string.tab_title_positions).setTabListener(this));
    actionBar.addTab(actionBar.newTab().setText(R.string.tab_title_scripts).setTabListener(this));
    actionBar.addTab(actionBar.newTab().setText(R.string.tab_title_run).setTabListener(this));
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
      getActionBar().setSelectedNavigationItem(
          savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_project, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case android.R.id.home:
      // app icon in action bar clicked; go home
      Intent intent = new Intent(this, ProjectListActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
  }

  @Override
  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    // When the given tab is selected, show the tab contents in the container
    Fragment fragment = null;
    mRunFragment = null;
    switch (tab.getPosition()) {
    case POSITIONS_TAB_INDEX:
      fragment = new PositionsFragment();
      break;
    case SCRIPT_EDITOR_INDEX:
      fragment = new ScriptEditorFragment();
      break;
    case RUN_INDEX:
      fragment = new RunFragment();
      mRunFragment = (RunFragment) fragment;
      break;

    }
    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  @Override
  public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
  }
  
  @Override
  protected void onCommandReceived(String receivedCommand) {
    super.onCommandReceived(receivedCommand);
    if (mRunFragment != null) {
      mRunFragment.onCommandReceived(receivedCommand);
    }
    
  }
}
