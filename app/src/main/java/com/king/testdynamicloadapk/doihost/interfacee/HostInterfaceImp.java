package com.king.testdynamicloadapk.doihost.interfacee;

import android.content.Context;

import com.king.testdynamicloadapk.doihost.component.HostComponent;
import com.ryg.HostInterface;


public class HostInterfaceImp implements HostInterface {

    @Override
    public void hostMethod(Context context) {
        new HostComponent().doSomething(context);
    }
}
