package edu.rosehulman.armscripts;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class PositionsFragment extends Fragment {

  private static final int GRIPPER_JOINT_NUMBER = 0;
  private Button mJoint1Button, mJoint2Button, mJoint3Button, mJoint4Button, mJoint5Button, mGripperButton;
  private SeekBar mAbsoluteCoarseSeekBar, mRelativeFineSeekBar;
  private TextView mCurrentJointValue;
  private int[] mCurrentJointValues = new int[6];

  /** Joint that is active in the teach pendant. */
  private int mActiveJoint = 1;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_positions, container, false);
  }

  @Override
  public void onStart() {
    super.onStart();
    Spinner spinner = (Spinner) getActivity().findViewById(R.id.position_selector);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.planets_array, R.layout.existing_position_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);

    // Default joint values for the home position.
    // TODO: Make this smarter when you start the fragment.
    mCurrentJointValues[0] = 30;
    mCurrentJointValues[1] = 0;
    mCurrentJointValues[2] = 90;
    mCurrentJointValues[3] = 0;
    mCurrentJointValues[4] = -90;
    mCurrentJointValues[5] = 0;

    // Create a new position

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
        highlightJointButton((Button) v);
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
  }

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
   * Updates the labels and seek bar values to match the current joint state.
   */
  private void updatePendantForActiveJoint() {
    mCurrentJointValue.setText("" + mCurrentJointValues[mActiveJoint]);
  }

  @Override
  public void onPause() {
    super.onPause();
  }
}
