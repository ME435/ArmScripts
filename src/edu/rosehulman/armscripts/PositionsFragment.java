package edu.rosehulman.armscripts;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

/**
 * The goal of this fragment is to allow a user to create positions within a project.
 * This fragment will only be shown within the Project Activity.
 * 
 * The positions are stored into a positions table and displayed using a spinner.
 * The remainder of the space is filled with a Teach Pendant which serves as the position
 * editor.
 *
 * @author fisherds
 *
 */
public class PositionsFragment extends Fragment {
  
  public static final String TAG = "PositionsFragment";
  private static final int GRIPPER_JOINT_NUMBER = 0;
  private Button mJoint1Button, mJoint2Button, mJoint3Button, mJoint4Button, mJoint5Button,
      mGripperButton;
  private SeekBar mAbsoluteCoarseSeekBar, mRelativeFineSeekBar;
  private TextView mCurrentJointValue;
  private int[] mCurrentJointValues = new int[6];

  /** Joint that is active in the teach pendant. */
  private int mActiveJoint = 1;
  
  /** Id of the parent project. */
  private long mParentProjectId;

  /** Reference to the SQLite database adapter. */
  private PositionDbAdapter mPositionDbAdapter;

  /** Adapter to display positions in the database. */
  private SimpleCursorAdapter mExistingPositionsAdapter;
  
  /** Spinner to display positions in the database. */
  private Spinner mExistingPositionsSpinner;
  
  /** Id of the position item that is selected by the spinner. */
  private long mSelectedPositionId;
  
  /** Hacky solution to reuse the same dialog for creating AND renaming positions. */
  private boolean mRenamePosition = false;

  private DialogFragment mCreatePositionDF = new DialogFragment() {
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      final Dialog dialog = new Dialog(getActivity());
      dialog.setContentView(R.layout.position_name_dialog);

      final EditText nameText = (EditText) dialog.findViewById(R.id.edittext_project_name);
      final Button confirmButton = (Button) dialog.findViewById(R.id.confirm_project_name_button);
      final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_project_name_button);
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
      
      // Consider: Done action is inconsistent with how the project naming works.
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

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPositionDbAdapter = new PositionDbAdapter(getActivity());
    mPositionDbAdapter.open();
    mParentProjectId = ((ProjectActivity) getActivity()).getCurrentProjectId();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_positions, container, false);
  }

  @Override
  public void onStart() {
    super.onStart();

    // Default joint values for the home position.
    // TODO: Make this smarter when you start the fragment.
    // You should request this information from Arduino.
    mCurrentJointValues[0] = 30;
    mCurrentJointValues[1] = 0;
    mCurrentJointValues[2] = 90;
    mCurrentJointValues[3] = 0;
    mCurrentJointValues[4] = -90;
    mCurrentJointValues[5] = 0;

    // Fill the Spinner with the existing positions.
    Cursor cursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
    mExistingPositionsSpinner = (Spinner) getActivity().findViewById(R.id.position_selector);
    int viewResourceId = R.layout.existing_position_item;
    String[] fromColumns = new String[] { PositionDbAdapter.KEY_NAME };
    int[] toTextViews = new int[] { android.R.id.text1 };
    mExistingPositionsAdapter = new SimpleCursorAdapter(getActivity(), viewResourceId, cursor, fromColumns,
        toTextViews);
    mExistingPositionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mExistingPositionsSpinner.setAdapter(mExistingPositionsAdapter);
    registerForContextMenu(mExistingPositionsSpinner);
    
    mExistingPositionsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        mSelectedPositionId = id;
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });
    
    // Joint buttons
    mJoint1Button = (Button) getActivity().findViewById(R.id.current_position_joint_1_button);
    mJoint2Button = (Button) getActivity().findViewById(R.id.current_position_joint_2_button);
    mJoint3Button = (Button) getActivity().findViewById(R.id.current_position_joint_3_button);
    mJoint4Button = (Button) getActivity().findViewById(R.id.current_position_joint_4_button);
    mJoint5Button = (Button) getActivity().findViewById(R.id.current_position_joint_5_button);
    mGripperButton = (Button) getActivity().findViewById(R.id.current_position_gripper_button);
    highlightJointButton(mJoint1Button);

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
        highlightJointButton((Button) v);
        updatePendantForActiveJoint();
      }

    };
    mJoint1Button.setOnClickListener(jointSelector);
    mJoint2Button.setOnClickListener(jointSelector);
    mJoint3Button.setOnClickListener(jointSelector);
    mJoint4Button.setOnClickListener(jointSelector);
    mJoint5Button.setOnClickListener(jointSelector);
    mGripperButton.setOnClickListener(jointSelector);

    mAbsoluteCoarseSeekBar = (SeekBar) getActivity().findViewById(R.id.absolute_joint_angle);
    mRelativeFineSeekBar = (SeekBar) getActivity().findViewById(R.id.relative_joint_angle);
    mCurrentJointValue = (TextView) getActivity().findViewById(R.id.textview_current_angle);

    // Create button handler.
    Button createPositionButton = (Button) getActivity().findViewById(R.id.create_position_button);
    createPositionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mRenamePosition = false;
        mCreatePositionDF.show(getFragmentManager(), "create position");
      }
    });
  }

  /**
   * Convenience method for changing which joint is highlight in the teach pendant.
   * @param activeJointButton Button view that needs to be highlighted in the teach pendant area.
   */
  private void highlightJointButton(Button activeJointButton) {
    mJoint1Button.setBackgroundResource(R.drawable.black_button);
    mJoint2Button.setBackgroundResource(R.drawable.black_button);
    mJoint3Button.setBackgroundResource(R.drawable.black_button);
    mJoint4Button.setBackgroundResource(R.drawable.black_button);
    mJoint5Button.setBackgroundResource(R.drawable.black_button);
    mGripperButton.setBackgroundResource(R.drawable.black_button);
    activeJointButton.setBackgroundResource(R.drawable.yellow_button);
    mJoint1Button.setPadding(60, 0, 0, 0);
    mJoint2Button.setPadding(60, 0, 0, 0);
    mJoint3Button.setPadding(60, 0, 0, 0);
    mJoint4Button.setPadding(60, 0, 0, 0);
    mJoint5Button.setPadding(60, 0, 0, 0);
    mGripperButton.setPadding(50, 0, 0, 0);
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
    Cursor positionSelected = mPositionDbAdapter.fetchPosition(mSelectedPositionId);
    int nameColumn = positionSelected.getColumnIndexOrThrow(PositionDbAdapter.KEY_NAME);
    String name = positionSelected.getString(nameColumn);

    switch (item.getItemId()) {
    case R.id.menu_item_position_delete:
//      Toast.makeText(getActivity(), "Deleting " + name, Toast.LENGTH_SHORT).show();
      mPositionDbAdapter.deletePosition(mSelectedPositionId);
      Cursor cursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
      mExistingPositionsAdapter.changeCursor(cursor);
      return true;
    case R.id.menu_item_position_edit:
      Toast.makeText(getActivity(), "Editing " + name, Toast.LENGTH_SHORT).show();
      mRenamePosition = true;
      mCreatePositionDF.show(getFragmentManager(), "rename position");
      return true;
    }
    return super.onContextItemSelected(item);
  }

  
  /**
   * Updates the labels and seek bar values to match the current joint state.
   */
  private void updatePendantForActiveJoint() {
    mCurrentJointValue.setText("" + mCurrentJointValues[mActiveJoint]);
  }

  private void addPosition(String positionName) {
    mPositionDbAdapter.createPosition(mParentProjectId, positionName,
        mCurrentJointValues[1], mCurrentJointValues[2], mCurrentJointValues[3],
        mCurrentJointValues[4], mCurrentJointValues[5]);
    Cursor cursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
    mExistingPositionsAdapter.changeCursor(cursor);
  }

  private void editPositionName(String positionName) {
    mPositionDbAdapter.updatePositionName(mSelectedPositionId, positionName);
    Cursor cursor = mPositionDbAdapter.fetchAllProjectPositions(mParentProjectId);
    mExistingPositionsAdapter.changeCursor(cursor);
  }

  @Override
  public void onPause() {
    super.onPause();
  }

}
