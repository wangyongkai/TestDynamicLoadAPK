package com.king.plugin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

import com.ryg.HostInterface;
import com.ryg.HostInterfaceManager;
import com.ryg.dynamicload.DLBasePluginActivity;
import com.ryg.dynamicload.internal.DLIntent;
import com.ryg.dynamicload.internal.DLPluginManager;

public class MainActivity extends DLBasePluginActivity {

    private static final String TAG = "MainActivity";


    //变成了一个普通类


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView(savedInstanceState);
    }

    private void initView(Bundle savedInstanceState) {

        //View view = LayoutInflater.from(that).inflate(R.layout.activity_main, null);
        that.setContentView(R.layout.aaaaa);

        that.findViewById(R.id.dddddd).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {


                HostInterface hostInterface = HostInterfaceManager.getHostInterface();
                hostInterface.hostMethod(that);

            }
        });


        //that.setContentView(generateContentView(that));
    }


    private View generateContentView(final Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        Button button = new Button(context);

        //button.setText("Invoke host method");
        button.setText(TestA.NAME);
        layout.addView(button, LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.d(TAG, "aaa--------->" + aaa);
                //HostInterface hostInterface = HostInterfaceManager.getHostInterface();
                //hostInterface.hostMethod(that);


                // DLPluginManager pluginManager = DLPluginManager.getInstance(that);
                //pluginManager.startPluginActivityForResultInner(that, new DLIntent("com.king.plugin"), -1);


            }
        });
        return layout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult resultCode=" + resultCode);
        if (resultCode == RESULT_FIRST_USER) {
            that.finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
