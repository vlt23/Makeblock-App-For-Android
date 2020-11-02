package cc.makeblock.makeblock;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setTitle(R.string.about_makeblock);
        super.setContentView(R.layout.activity_about);

        final View githubLink = findViewById(R.id.github_link);
        final Context context = getBaseContext();
        githubLink.setOnClickListener(nv ->
                Utils.openUrlInBrowser(context, context.getString(R.string.github_url)));
    }

}
