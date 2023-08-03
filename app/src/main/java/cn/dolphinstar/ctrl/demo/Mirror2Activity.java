package cn.dolphinstar.ctrl.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;

import cn.dolphinstar.lib.ctrlCore.MYOUController;

public class Mirror2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror2);

        Button button = findViewById(R.id.stopMirror);
        button.setOnClickListener(v->{
            MYOUController.of(Mirror2Activity.this).getDpsMirror().Stop();
            this.finish();
        });
    }

    @Override
    protected void onDestroy() {
        MYOUController.of(Mirror2Activity.this).getDpsMirror().Stop();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)
                || (keyCode == KeyEvent.KEYCODE_HOME)){
            MYOUController.of(Mirror2Activity.this).getDpsMirror().Stop();
            return  super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }
}
