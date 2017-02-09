package app.compass;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Just putting up the about message.
 * Created by Christopher D. Harte on 06/02/2017.
 */

class About extends Activity
{
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        TextView about_text = (TextView)findViewById(R.id.about_text);

        Bundle b = getIntent().getExtras();
        String title = b.getString("title");
        String body = b.getString("body");

        setTitle(title);
        about_text.setText(body);
    }
}
