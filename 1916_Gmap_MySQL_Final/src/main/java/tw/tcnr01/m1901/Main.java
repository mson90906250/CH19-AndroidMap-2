package tw.tcnr01.m1901;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class Main extends AppCompatActivity {
    Intent it = new Intent();
    private String TAG="oldpa=>";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setupViewComponent();
    }

    private void setupViewComponent() {
    }

    //-----------------
    // Button元件的事件處理
    public void btn_start_Click(View view) {
        it.setClass(Main.this, M1901.class);
        startActivity(it);
    }
    // Button元件的事件處理
    public void btn_sql_Click(View view) {
        it.setClass(Main.this, M1421.class);
        startActivity(it);
    }
    //====================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_finish:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

