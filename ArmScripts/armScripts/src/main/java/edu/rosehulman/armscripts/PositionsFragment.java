package edu.rosehulman.armscripts;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import edu.rosehulman.armscripts.db.PositionDbAdapter;

/**
 * The goal of this fragment is to allow a user to create positions within a
 * project. This fragment will only be shown within the Project Activity.
 * 
 * The positions are stored into a positions table and displayed using a
 * spinner. The remainder of the space is filled with a Teach Pendant which
 * serves as the position editor.
 * 
 * @author fisherds
 * 
 */
public class PositionsFragment extends Fragment {

  public static final String TAG = "PositionsFragment";
  private static final int GRIPPER_JOINT_NUMBER = 0;
  private static final int ABSOLUTE_SEEK_BAR_MAX = 90;
  private static final int RELATIVE_SEEK_BAR_MAX = 5;

  // References to simple UI widgets
  private Button[] mJointButton = new Button[6];
  private SeekBar mAbsoluteCoarseSeekBar, mRelativeFineSeekBar;
  private TextView mAbsoluteAngleMaxTextView, mAbsoluteAngleMinTextView, mRelativeAngleMaxTextView, mRelativeAngleMinTextView;
  
  private TextView mCurrentJointValueTextView;
  private TextView[] mExistingPositionTextView = new TextView[6];

  /** Current robot joint state. */
  private int[] mCurrentJointValues = new int[6];

  /**
   * As the relative seek bar value changes, for example from +3 to +4, it's
   * important to know what the old contribution was so that the new appropriate
   * offset is used.
   */
  private int mRelativeSeekBarOffset = 0;

  /** Joint that is active in the teach pendant. */
  private int mActiveJoint = 1;

  /** Id of the parent project. */
  private long mParentProjectId;

  /** Reference to the SQLite database adapter. */
  private PositionDbAdapter mPositionDbAdapter;

  /** Adapter to display positions in the database. */
  private SimpleCursorAdapter mPositionsCursorAdapter;

  /** Spinner to display positions in the database. */
  private Spinner mExistingPositionsSpinner;

  /** Id of the position item that is selected by the spinner. */
  private long mSelectedPositionId = 0; // Default to 0 for non-selected.

  /**
   * Hacky solution to reuse the same dialog for creating AND renaming
   * positions.  Set this flag true before launching the dialog and it'll
   * display a dialog for renaming.  Set this flag false then show the dialog
   * and it'll display a dialog for a new position.  Hacky but easy.
   */
  private boolean mRenamePosition = false;

  /**
   * Dialog used when naming a new position OR renaming an existing position.
   */
  private DialogFragment mCreatePositionDF = new DialogFragment() {
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      final Dialog dialog = new Dialog(getActivity());
      dialog.setContentView(R.layout.position_name_dialog);

      final EditText nameText = (EditText) dialog.findViewById(R.id.edittext_position_name);
      final Button confirmButton = (Button) dialog.findViewById(R.id.confirm_position_name_button);
      final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_position_name_button);
      if (mRenamePosition) {
        dialog.setTitle("Edit the position name");
        Cursor positionSelected = mPositionDbAdapter.fetchPosition(mSelectedPositionId);
        int nameColumn = positionSelected.getColumnIndexOrThrow(PositionDbAdapter.KEY_NAME);
        String name = positionSelected.getString(nameColumn);
        nameText.setText(name);
        confirmButton.setText("Rename Position");
      } else {
        dialog.setTitle("Name the position");
        confirmButton.setText("Create Position");
      }

      confirmButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          String positionName = nameText.getText().toString();
          if (mRenamePosition) {
            editPositionName(positionName);
          } else {
            addPosition(positionName);
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
            String positionName = nameText.getText().toString();
            addPosition(positionName);
            dialog.dismiss();
          }
          return false;
        }
      });

      return dialog;
    };
  };

  /**
   * Do some of the initialization work that is UI independent.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPositionDbAdapter = new PositionDbAdapter();
    mPositionDbAdapter.open();
    mParentProjectId = ((ProjectActivity) getActivity()).getCurrentProjectId();
    
    // Default joint values for the current robot position.
    // Consider: Could make this smarter when you start the fragment.
    // Could request this information from Arduino.  Assume HOME for now.
    mCurrentJointValues[1] = 0;
    mCurrentJointValues[2] = 90;
    mCurrentJointValues[3] = 0;
    mCurrentJointValues[4] = -90;
    mCurrentJointValues[5] = 90;
    mCurrentJointValues[GRIPPER_JOINT_NUMBER] = 50;

    setHasOptionsMenu(true);
  }

  /**
   * Initialization of this fragment.  Grab all the UI widgets and setup all the listeners.
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Get the view ready.  Usually this is the only task done by onCreateView.
    View view = inflater.inflate(R.layout.fragment_positions, container, false);

    // Grab member variable references to the view widgets.
    mJointButton[1] = (Button) view.findViewById(R.id.current_position_joint_1_button);
    mJointButton[2] = (Button) view.findViewById(R.id.current_position_joint_2_button);
    mJointButton[3] = (Button) view.findViewById(R.id.current_position_joint_3_button);
    mJointButton[4] = (Button) view.findViewById(R.id.current_position_joint_4_button);
    mJointButton[5] = (Button) view.findViewById(R.id.current_position_joint_5_button);
    mJointButton[GRIPPER_JOINT_NUMBER] = (Button) view.findViewById(
        R.id.current_position_gripper_button);
    mCurrentJointValueTextView = (TextView) view.findViewById(R.id.textview_current_angle);
    mAbsoluteCoarseSeekBar = (SeekBar) view.findViewById(R.id.absolute_joint_angle);
    mRelativeFineSeekBar = (SeekBar) view.findViewById(R.id.relative_joint_angle);
    mAbsoluteAngleMaxTextView = (TextView) view.findViewById(R.id.textview_absolute_angle_max);
    mAbsoluteAngleMinTextView = (TextView) view.findViewById(R.id.textview_absolute_angle_min);
    mRelativeAngleMaxTextView = (TextView) view.findViewById(R.id.textview_relative_angle_max);
    mRelativeAngleMinTextView = (TextView) view.findViewById(R.id.textview_relative_angle_min);
    mExistingPositionsSpinner = (Spinner) view.findViewById(R.id.position_selector);
    // Note: There is no mExistingPositionTextView[0] TextView.
    mExistingPositionTextView[1] = (TextView) view.findViewById(R.id.joint_1_value);
    mExistingPositionTextView[2] = (TextView) view.findViewById(R.id.joint_2_value);
    mExistingPositionTextView[3] = (TextView) view.findViewById(R.id.joint_3_value);
    mExistingPositionTextView[4] = (TextView) view.findViewById(R.id.joint_4_value);
    mExistingPositionTextView[5] = (TextView) view.findViewById(R.id.joint_5_value);

    
    // Get the active script. Make one if none exist.
//    Cursor scriptsCursor = mScriptDbAdapter.fetchAllProjectScripts(mParentProjectId);
//    if (scriptsCursor != null && scriptsCursor.moveToFirst()) {
//      int scriptIdColumn = scriptsCursor.getColumnIndexOrThrow(ScriptDbAdapter.KEY_ID);
//      mActiveScriptId = scriptsCursor.getLong(scriptIdColumn);
//    } else {
//      // No scripts in this project. Make one.
//      mActiveScriptId = mScriptDbAdapter.createScript(mParentProjectId, DEFAULT_SCRIPT_NAME);
//    }
    
    Cursor positionsCursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
    if (positionsCursor == null || !positionsCursor.moveToFirst()) {
      // There are no positions.  Add HOME at least.
      mPositionDbAdapter.createPosition(mParentProjectId, "HOME", 0, 90, 0, -90, 90);
      mPositionDbAdapter.createPosition(mParentProjectId, "ZERO", 0, 0, 0, 0, 0);
    }
    

    // Create button handler.  Launch the dialog (in create mode) when clicked.
    Button createPositionButton = (Button) view.findViewById(R.id.create_position_button);
    if (createPositionButton != null) {
      setHasOptionsMenu(false); // Remove the menu button if we have the button here.
      createPositionButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mRenamePosition = false;
          mCreatePositionDF.show(getFragmentManager(), "create position");
        }
      });
    }


    // Joint buttons in the teach pendant.
    updateJointButtonText();
    mActiveJoint = 1;
    updatePendantActiveJointChanged();

    // Create the listener that all these buttons will use.
    OnClickListener jointSelector = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // Change the member variable for which joint is selected.
        switch (v.getId()) {
        case R.id.current_position_joint_1_button:
          mActiveJoint = 1;
          break;
        case R.id.current_position_joint_2_button:
          mActiveJoint = 2;
          break;
        case R.id.current_position_joint_3_button:
          mActiveJoint = 3;
          break;
        case R.id.current_position_joint_4_button:
          mActiveJoint = 4;
          break;
        case R.id.current_position_joint_5_button:
          mActiveJoint = 5;
          break;
        case R.id.current_position_gripper_button:
          mActiveJoint = GRIPPER_JOINT_NUMBER;
          break;
        default:
          mActiveJoint = 1;
          break;
        }
        updatePendantActiveJointChanged();
      }
    };
    mJointButton[1].setOnClickListener(jointSelector);
    mJointButton[2].setOnClickListener(jointSelector);
    mJointButton[3].setOnClickListener(jointSelector);
    mJointButton[4].setOnClickListener(jointSelector);
    mJointButton[5].setOnClickListener(jointSelector);
    mJointButton[GRIPPER_JOINT_NUMBER].setOnClickListener(jointSelector);

    // Setup listeners for the two seek bars (absolute seek bar first).
    mAbsoluteCoarseSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        
        // This needs to be an independent equation for each joint.
        switch (mActiveJoint) {
        case 1:
          mCurrentJointValues[mActiveJoint] = progress - 90;
          break;
        case 2:
          mCurrentJointValues[mActiveJoint] = progress;
          break;
        case 3:
          mCurrentJointValues[mActiveJoint] = progress - 90;
          break;
        case 4:
          mCurrentJointValues[mActiveJoint] = progress - 180;
          break;
        case 5:
          mCurrentJointValues[mActiveJoint] = progress;
          break;
        case GRIPPER_JOINT_NUMBER:
          mCurrentJointValues[mActiveJoint] = progress / 2 - 25;
          break;
        }        
        updateCurrentJointTextSeekBarChanged();
      }
    });

    // Relative seek bar is a bit more complex because it snaps back to the middle on release.
    mRelativeFineSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        mRelativeSeekBarOffset = 0;
        seekBar.setProgress(RELATIVE_SEEK_BAR_MAX);
        updateAbsoluteSeekBar(mCurrentJointValues[mActiveJoint]);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int currentRelativeOffset = progress - RELATIVE_SEEK_BAR_MAX;
        int absoluteBarValue = mCurrentJointValues[mActiveJoint] - mRelativeSeekBarOffset;
        mCurrentJointValues[mActiveJoint] = absoluteBarValue + currentRelativeOffset;
        mRelativeSeekBarOffset = currentRelativeOffset;
        updateCurrentJointTextSeekBarChanged();
      }
    });

    // Existing positions spinner setup to display the existing positions in this project.
    // Same drill: make a cursor using the DbAdapter, a SimpleCursorAdapter, and tie it to the UI.
    Cursor cursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
    String[] fromColumns = new String[] { PositionDbAdapter.KEY_NAME };
    int[] toTextViews = new int[] { android.R.id.text1 };
    mPositionsCursorAdapter = new SimpleCursorAdapter(getActivity(),
        R.layout.existing_position_item, cursor,
        fromColumns, toTextViews);
    mPositionsCursorAdapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mExistingPositionsSpinner.setAdapter(mPositionsCursorAdapter);
    registerForContextMenu(mExistingPositionsSpinner);

    // Listener for the spinner.
    // When a new position is selected update the text views to display the values.
    mExistingPositionsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        mSelectedPositionId = id;
        updateExistingPositionJointValuesSpinnerChanged();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
        mSelectedPositionId = 0;
        updateExistingPositionJointValuesSpinnerChanged();
      }
    });
    
    // Go to position button
    // Make the robot move to this position.
    // Update the current position member variables.
    // Update the pendant UI to this new position.
    Button goToPositionButton = (Button) view.findViewById(R.id.go_to_position_button);
    goToPositionButton.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View v) {
        // TODO: Make the robot move.
        Cursor currentPosition = mPositionDbAdapter.fetchPosition(mSelectedPositionId);
        int joint1ColumnIndex = currentPosition.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_1);
        int joint2ColumnIndex = currentPosition.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_2);
        int joint3ColumnIndex = currentPosition.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_3);
        int joint4ColumnIndex = currentPosition.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_4);
        int joint5ColumnIndex = currentPosition.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_5);
        mCurrentJointValues[1] = currentPosition.getInt(joint1ColumnIndex);
        mCurrentJointValues[2] = currentPosition.getInt(joint2ColumnIndex);
        mCurrentJointValues[3] = currentPosition.getInt(joint3ColumnIndex);
        mCurrentJointValues[4] = currentPosition.getInt(joint4ColumnIndex);
        mCurrentJointValues[5] = currentPosition.getInt(joint5ColumnIndex);
        
        // Actually move the robot arm.
        String commandString = "POSITION " +
            mCurrentJointValues[1] + " " +
            mCurrentJointValues[2] + " " +
            mCurrentJointValues[3] + " " +
            mCurrentJointValues[4] + " " +
            mCurrentJointValues[5];
        ((AccessoryActivity) getActivity()).sendCommand(commandString);
        
        mActiveJoint = 1;
        updateJointButtonText();
        updatePendantActiveJointChanged();
      }
    });
    
    // Update position button.
    // Don't move the robot.  Just update the database for this joint to the current values.
    // And update the text views for the new joint values.
    Button updatePositionButton = (Button) view.findViewById(R.id.set_position_button);
    updatePositionButton.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View v) {
        mPositionDbAdapter.updatePositionJointValues(mSelectedPositionId, 
            mCurrentJointValues[1],
            mCurrentJointValues[2],
            mCurrentJointValues[3],
            mCurrentJointValues[4],
            mCurrentJointValues[5]);        
        updateExistingPositionJointValuesSpinnerChanged();
      }
    });
    
    return view;
  }

  /**
   * Update the 5 TextViews for the current position in the Spinner.
   */
  private void updateExistingPositionJointValuesSpinnerChanged() {

    if (mSelectedPositionId == 0) {
      Toast.makeText(getActivity(), "No known positoin to use", Toast.LENGTH_SHORT).show();
      mExistingPositionTextView[1].setText(getString(R.string.unknown_joint_value));
      mExistingPositionTextView[2].setText(getString(R.string.unknown_joint_value));
      mExistingPositionTextView[3].setText(getString(R.string.unknown_joint_value));
      mExistingPositionTextView[4].setText(getString(R.string.unknown_joint_value));
      mExistingPositionTextView[5].setText(getString(R.string.unknown_joint_value));
      return;
    }

    Cursor positionSelected = mPositionDbAdapter.fetchPosition(mSelectedPositionId);
    int joint1Column = positionSelected.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_1);
    int joint2Column = positionSelected.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_2);
    int joint3Column = positionSelected.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_3);
    int joint4Column = positionSelected.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_4);
    int joint5Column = positionSelected.getColumnIndexOrThrow(PositionDbAdapter.KEY_JOINT_5);
    mExistingPositionTextView[1].setText(getString(R.string.degrees_format,
        positionSelected.getInt(joint1Column)));
    mExistingPositionTextView[2].setText(getString(R.string.degrees_format,
        positionSelected.getInt(joint2Column)));
    mExistingPositionTextView[3].setText(getString(R.string.degrees_format,
        positionSelected.getInt(joint3Column)));
    mExistingPositionTextView[4].setText(getString(R.string.degrees_format,
        positionSelected.getInt(joint4Column)));
    mExistingPositionTextView[5].setText(getString(R.string.degrees_format,
            positionSelected.getInt(joint5Column)));
  }

  /**
   * Update the two places where the active joint value is displayed.  This method is
   * used when the seek bars are used to change the angle (robot moves).
   */
  private void updateCurrentJointTextSeekBarChanged() {
    int jointValue = mCurrentJointValues[mActiveJoint];
    if (mActiveJoint == GRIPPER_JOINT_NUMBER) {
      mCurrentJointValueTextView.setText(getString(R.string.mm_format, jointValue));      
    } else {
      mCurrentJointValueTextView.setText(getString(R.string.degrees_format, jointValue));
    }
    if (mActiveJoint > 0) {
      // Actually make the robot arm move
      ((AccessoryActivity) getActivity()).sendCommand("JOINT " + mActiveJoint + " ANGLE " + jointValue);
      mJointButton[mActiveJoint].setText(getString(R.string.joint_label_format, mActiveJoint,
          jointValue));
    } else {
      // Actually make the robot arm move
      ((AccessoryActivity) getActivity()).sendCommand("GRIPPER " + jointValue);
    }
  }

  /**
   * Updates the labels and seek bar values to match the current joint state.  While
   * similar to the function above, this one is used when we switch joints (robot didn't move).
   */
  private void updatePendantActiveJointChanged() {
    // TODO: There is some duplicate code here and above that could be joined I'm sure.
    int jointValue = mCurrentJointValues[mActiveJoint];
    if (mActiveJoint == GRIPPER_JOINT_NUMBER) {
      mCurrentJointValueTextView.setText(getString(R.string.mm_format, jointValue));      
    } else {
      mCurrentJointValueTextView.setText(getString(R.string.degrees_format, jointValue));
    }
    updateAbsoluteSeekBar(jointValue);
    
    mRelativeAngleMaxTextView.setText(getString(R.string.relative_angle_max));
    mRelativeAngleMinTextView.setText(getString(R.string.relative_angle_min));
    switch (mActiveJoint) {
    case 1:
      mAbsoluteAngleMinTextView.setText(getString(R.string.degrees_format, -90));
      mAbsoluteAngleMaxTextView.setText(getString(R.string.degrees_format, 90));
      break;
    case 2:
      mAbsoluteAngleMinTextView.setText(getString(R.string.degrees_format, 0));
      mAbsoluteAngleMaxTextView.setText(getString(R.string.degrees_format, 180));
      break;
    case 3:
      mAbsoluteAngleMinTextView.setText(getString(R.string.degrees_format, -90));
      mAbsoluteAngleMaxTextView.setText(getString(R.string.degrees_format, 90));
      break;
    case 4:
      mAbsoluteAngleMinTextView.setText(getString(R.string.degrees_format, -180));
      mAbsoluteAngleMaxTextView.setText(getString(R.string.degrees_format, 0));
      break;
    case 5:
      mAbsoluteAngleMinTextView.setText(getString(R.string.degrees_format, 0));
      mAbsoluteAngleMaxTextView.setText(getString(R.string.degrees_format, 180));
      break;
    case GRIPPER_JOINT_NUMBER:
      mAbsoluteAngleMinTextView.setText("Closed");
      mAbsoluteAngleMaxTextView.setText("Open");
      mRelativeAngleMaxTextView.setText("+5mm");
      mRelativeAngleMinTextView.setText("-5mm");
      break;
    }
    
    mRelativeFineSeekBar.setProgress(RELATIVE_SEEK_BAR_MAX);
    mJointButton[1].setBackgroundResource(R.drawable.black_button);
    mJointButton[2].setBackgroundResource(R.drawable.black_button);
    mJointButton[3].setBackgroundResource(R.drawable.black_button);
    mJointButton[4].setBackgroundResource(R.drawable.black_button);
    mJointButton[5].setBackgroundResource(R.drawable.black_button);
    mJointButton[GRIPPER_JOINT_NUMBER].setBackgroundResource(R.drawable.black_button);
    // Make the active joint yellow.
    mJointButton[mActiveJoint].setBackgroundResource(R.drawable.yellow_button);
    // Set the padding values (not sure why but it's reset when you set the background).
    mJointButton[1].setPadding(60, 0, 0, 0);
    mJointButton[2].setPadding(60, 0, 0, 0);
    mJointButton[3].setPadding(60, 0, 0, 0);
    mJointButton[4].setPadding(60, 0, 0, 0);
    mJointButton[5].setPadding(60, 0, 0, 0);
    mJointButton[GRIPPER_JOINT_NUMBER].setPadding(50, 0, 0, 0);  // Joint "0"
  }


  private void updateAbsoluteSeekBar(int jointValue) {

    // This needs to be an independent equation for each joint.
    switch (mActiveJoint) {
    case 1:
      mAbsoluteCoarseSeekBar.setProgress(jointValue + 90);
      break;
    case 2:
      mAbsoluteCoarseSeekBar.setProgress(jointValue);
      break;
    case 3:
      mAbsoluteCoarseSeekBar.setProgress(jointValue + 90);
      break;
    case 4:
      mAbsoluteCoarseSeekBar.setProgress(jointValue + 180);
      break;
    case 5:
      mAbsoluteCoarseSeekBar.setProgress(jointValue);
      break;
    case GRIPPER_JOINT_NUMBER:
      mAbsoluteCoarseSeekBar.setProgress(jointValue * 2 + 50);
      break;
    }
  }

  // Update the text on all the joint buttons.
  private void updateJointButtonText() {
    mJointButton[1].setText(getString(R.string.joint_label_format, 1,
        mCurrentJointValues[1]));
    mJointButton[2].setText(getString(R.string.joint_label_format, 2,
        mCurrentJointValues[2]));
    mJointButton[3].setText(getString(R.string.joint_label_format, 3,
        mCurrentJointValues[3]));
    mJointButton[4].setText(getString(R.string.joint_label_format, 4,
        mCurrentJointValues[4]));
    mJointButton[5].setText(getString(R.string.joint_label_format, 5,
        mCurrentJointValues[5]));
  }
  /**
   * Create a context menu for the list view.
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflator = getActivity().getMenuInflater();
    if (v == mExistingPositionsSpinner) {
      inflator.inflate(R.menu.existing_position_context_menu, menu);
    }
  }

  /**
   * Standard listener for the context menu item selections
   */
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_item_position_delete:
      mPositionDbAdapter.deletePosition(mSelectedPositionId);
      mSelectedPositionId = 0;
      Cursor cursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
      mPositionsCursorAdapter.changeCursor(cursor);
      return true;
    case R.id.menu_item_position_edit:
      mRenamePosition = true;
      mCreatePositionDF.show(getFragmentManager(), "rename position");
      return true;
    }
    return super.onContextItemSelected(item);
  }

  private void addPosition(String positionName) {
    long positionId = mPositionDbAdapter.createPosition(mParentProjectId, positionName,
        mCurrentJointValues[1], mCurrentJointValues[2], mCurrentJointValues[3],
        mCurrentJointValues[4], mCurrentJointValues[5]);
    Cursor cursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
    mPositionsCursorAdapter.changeCursor(cursor);

    // Set the spinner to the newest position
    for (int i = 0; i < mExistingPositionsSpinner.getCount(); i++) {
      long itemIdAtPosition = mExistingPositionsSpinner.getItemIdAtPosition(i);
      if (itemIdAtPosition == positionId) {
        mExistingPositionsSpinner.setSelection(i);
        break;
      }
    }
  }

  private void editPositionName(String positionName) {
    mPositionDbAdapter.updatePositionName(mSelectedPositionId, positionName);
    Cursor cursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
    mPositionsCursorAdapter.changeCursor(cursor);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.positions_options_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.create_position_menu_item:
        mRenamePosition = false;
        mCreatePositionDF.show(getFragmentManager(), "create position");
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onPause() {
    super.onPause();
  }

}
