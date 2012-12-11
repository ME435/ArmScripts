package edu.rosehulman.armscripts;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class PositionsFragment extends Fragment {

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

    final Button joint1Button = (Button) getActivity().findViewById(R.id.current_position_joint_1_button);
    final Button joint2Button = (Button) getActivity().findViewById(R.id.current_position_joint_2_button);
    final Button joint3Button = (Button) getActivity().findViewById(R.id.current_position_joint_3_button);
    final Button joint4Button = (Button) getActivity().findViewById(R.id.current_position_joint_4_button);
    final Button joint5Button = (Button) getActivity().findViewById(R.id.current_position_joint_5_button);
    final Button gripperButton = (Button) getActivity().findViewById(R.id.current_position_gripper_button);
    joint1Button.setBackgroundResource(R.drawable.yellow_button);
    joint1Button.setPadding(60, 0, 0, 0);
    joint2Button.setPadding(60, 0, 0, 0);
    joint3Button.setPadding(60, 0, 0, 0);
    joint4Button.setPadding(60, 0, 0, 0);
    joint5Button.setPadding(60, 0, 0, 0);
    gripperButton.setPadding(50, 0, 0, 0);
    
    OnClickListener jointSelector = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        joint1Button.setBackgroundResource(R.drawable.black_button);
        joint2Button.setBackgroundResource(R.drawable.black_button);
        joint3Button.setBackgroundResource(R.drawable.black_button);
        joint4Button.setBackgroundResource(R.drawable.black_button);
        joint5Button.setBackgroundResource(R.drawable.black_button);
        gripperButton.setBackgroundResource(R.drawable.black_button);
        v.setBackgroundResource(R.drawable.yellow_button);
        joint1Button.setPadding(60, 0, 0, 0);
        joint2Button.setPadding(60, 0, 0, 0);
        joint3Button.setPadding(60, 0, 0, 0);
        joint4Button.setPadding(60, 0, 0, 0);
        joint5Button.setPadding(60, 0, 0, 0);
        gripperButton.setPadding(50, 0, 0, 0);
      }
    };
    
    joint1Button.setOnClickListener(jointSelector);
    joint2Button.setOnClickListener(jointSelector);
    joint3Button.setOnClickListener(jointSelector);
    joint4Button.setOnClickListener(jointSelector);
    joint5Button.setOnClickListener(jointSelector);
    gripperButton.setOnClickListener(jointSelector);
  }

  @Override
  public void onPause() {
    super.onPause();
  }
}
