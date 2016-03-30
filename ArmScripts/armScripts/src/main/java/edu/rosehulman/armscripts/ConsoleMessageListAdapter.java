package edu.rosehulman.armscripts;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ConsoleMessageListAdapter extends ArrayAdapter<ConsoleMessage> {

  public ConsoleMessageListAdapter(Context context, int textViewResourceId, List<ConsoleMessage> objects) {
    super(context, textViewResourceId, R.id.message_sent, objects);
  }
  
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View messageView = super.getView(position, convertView, parent);
    ConsoleMessage message = getItem(position);
    TextView receivedTextView = (TextView) messageView.findViewById(R.id.message_receieved);
    TextView sentTextView = (TextView) messageView.findViewById(R.id.message_sent);
    if (message.isAndroidToAccessory()) {
      sentTextView.setText(message.getCommandString());
      receivedTextView.setText("");
    } else {
      sentTextView.setText("");
      receivedTextView.setText(message.getCommandString());
    }
    return messageView;
  }
}
