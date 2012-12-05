package edu.rosehulman.armscripts;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class ProjectListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);
		
		Button getStartedButton = (Button) findViewById(R.id.get_started_button);
		getStartedButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent projectIntent = new Intent(ProjectListActivity.this, ProjectActivity.class);
				startActivity(projectIntent);
			}
		});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_project_list, menu);
        return true;
    }
}
