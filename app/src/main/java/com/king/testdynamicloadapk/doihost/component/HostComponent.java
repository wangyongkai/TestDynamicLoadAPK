package com.king.testdynamicloadapk.doihost.component;

import android.content.Context;
import android.widget.Toast;

public class HostComponent {

    public void doSomething(Context context) {
        Toast.makeText(context, "I'm a component in host. I do sth.", Toast.LENGTH_SHORT).show();
    }
}
