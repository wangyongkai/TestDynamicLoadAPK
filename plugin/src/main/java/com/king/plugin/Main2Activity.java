package com.king.plugin;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ryg.dynamicload.DLBasePluginActivity;
import com.ryg.dynamicload.internal.DLIntent;
import com.ryg.dynamicload.internal.DLPluginManager;

public class Main2Activity extends DLBasePluginActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main2);
        initView(savedInstanceState);
    }


    private void initView(Bundle savedInstanceState) {
        that.setContentView(generateContentView(that, that.getResources().getDrawable(R.drawable.qqq)));
    }


    private View generateContentView(final Context context, Drawable drawable) {
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        ImageView button = new ImageView(context);

        //button.setText("Invoke host method");
        button.setImageDrawable(drawable);
        layout.addView(button, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        return layout;
    }
}
