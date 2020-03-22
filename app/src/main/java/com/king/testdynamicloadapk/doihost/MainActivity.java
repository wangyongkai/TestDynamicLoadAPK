package com.king.testdynamicloadapk.doihost;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.king.testdynamicloadapk.R;
import com.king.testdynamicloadapk.doihost.interfacee.HostInterfaceImp;
import com.ryg.HostInterfaceManager;
import com.ryg.dynamicload.internal.DLIntent;
import com.ryg.dynamicload.internal.DLPluginManager;
import com.ryg.utils.DLUtils;

public class MainActivity extends Activity implements OnItemClickListener {

    public static final String FROM = "extra.from";
    public static final int FROM_INTERNAL = 0;
    public static final int FROM_EXTERNAL = 1;

    private ArrayList<PluginItem> mPluginItems = new ArrayList<PluginItem>();
    private PluginAdapter mPluginAdapter;

    private ListView mListView;
    private TextView mNoPluginTextView;


    //------------------------------------------------------------------------

    //问题1：一个类 继承的父类中 和 实现的接口中 含有相同的方法
    //接口优先级高于父类。这个类没实现方法，就把父类中的方法当做接口方法的实现。
    // 这个类实现了方法，就把这个类的方法当接口的实现方法。

    //------------------------------------------------------------------------

    //问题2：so库如何加载

    //so文件安装后放在 /data/data/包名/lib/ 目录下 该目录是程序不能操作的，
    // 不能在程序运行时，向该目录拷贝.so文件 。所以要想动态加载so 只能用 System.load(filepath);
    //举例：如何在不安装app的情况下更新线上的so库？思路：第一次安装带上so用System.loadlibrary加载so
    // 以后启动app都请求接口查看是否需要更新so 如果需要则则先下载so 同时修改加载so的地方为System.load(filepath)


    //本项目中在加载插件apk的时候 将其中的so复制到/data/user/0/com.king.testdynamicloadapk/app_pluginlib目录下
    //然后利用DexClassLoader传进这个so的path进行动态加载apk中的so文件

    //------------------------------------------------------------------------


    //问题3：每一个插件apk 新建一个DexClassLoader 这样导致每个插件里的类都是单独的
    // 即使类名完全一致 也是两个不同的类   那么插件中继承的dllibrary中的类呢
    // 跟壳中的dllibrary中的应该也是不同的类  这样插件中如果有更新dllibrary那不是跟壳中的dllibrary不对齐了？

    //到底有没有加载两份dllibrar？？ 或者怎么保证壳子中加载的类 插件不再重复加载？
    //解决方案：将DexClassLoader的父加载器设置为PathClassLoader


    //app类加载原理：
    //任何运行的Android应用至少包含有两个 ClassLoader，每个应用中的PathClassLoader拥有同一个
    // parent即是BootClassLoader，这样就保证了系统代码共享以及应用代码隔离，如下图。

    //classloader加载时先查找此当前类有没有被加载过 如果没有则查有父类加载器则用父类加载器加载
    //DexClassLoader构造时传的的父类加载器竟然是PathClassLoader
    // 例如插件中的DLBasePluginActivity  应用启动时PathClassLoader肯定已经加
    // 载过DLBasePluginActivity 所以DexClassLoader不会再加载


    //结论：
    // 1.DexClassLoader构造时可以指定PathClassLoader 注意不是继承关系 是组合
    // 2.壳中共有的代码 插件中不会再次加载 因为插件的父类加载器是PathClassLoader已经加载过


    //插件打好包后 又在壳的dllibrary修改了代码 比如增加一个变量  则加载插件类时报错
    // java.lang.IncompatibleClassChangeError: Structural change of com.ryg.dynamicload.DLBasePluginActivity
    // is hazardous (/data/user/0/com.king.testdynamicloadapk/app_dex/plugin-debug.dex at compile time,
    // /data/app/com.king.testdynamicloadapk-2/oat/x86/base.odex at runtime): Instance field count off: 6 vs 7
    //    Lcom/ryg/dynamicload/DLBasePluginActivity; (Compile time):


    //------------------------------------------------------------------------


    //问题4：插件dex是如何被加载的
    // 插件MainActivity中用到了一个类TestA 可是项目中只是调用了loadPluginClass加载了MainActivity一个类
    // 但是在MainActivity中却可以使用TestA

    //个人理解：因为dexclassloader写了dex文件的路径 所以加载一个类 会顺便把dex中的与这个类相关的类都加载


    //------------------------------------------------------------------------


    //问题5：插件内的多个activity的跳转？
    //可以在dllibrary中搞一个方法，遍历插件中的activity跳转哪个就调用代理activity去加载哪个。


    //------------------------------------------------------------------------


    //问题6：如何访问插件中的资源
    //个人理解：以activity为维度区分资源。壳子中的界面的activity没有反射反射调用addAssetPath 所以是单独的ContextImpl里面的Resources实例
    // ProxyActivity中反射创建实例 调用addAssetPath增加apk的路径后  是另一个Resources实例 并不是资源的合并

    // 疑问：如果多个插件之间有共同的资源 怎么共享 ？ 插件怎么访问壳子中的资源？
    //


    //------------------------------------------------------------------------


    //问题7：插件界面的生命周期如何控制
    //利用接口DLPlugin 在代理activity中的各个生命周期方法内调用DLPlugin的各个生命周期方法（插件界面实现了此接口）
    //也就是说接口走各个方法 并不是走的activity的生命周期方法 而是接口中的方法  不是继承activity的方法


    //------------------------------------------------------------------------

    //------------------------------------------------------------------------

    //------------------------------------------------------------------------

    //------------------------------------------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();


        findViewById(R.id.ttototo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(MainActivity.this, MainActivity1.class));

            }
        });


        int i = 1;
        ClassLoader classLoader = getClassLoader();//dalvik.system.PathClassLoader
        if (classLoader != null) {
            Log.i("MainActivity", "[onCreate] classLoader " + i + " : " + classLoader.toString());
            while (classLoader.getParent() != null) {
                classLoader = classLoader.getParent();
                i++;
                Log.i("MainActivity", "[onCreate] classLoader " + i + " : " + classLoader.toString());
            }
        }


        HostInterfaceManager.setHostInterface(new HostInterfaceImp());
    }

    private void initView() {
        mPluginAdapter = new PluginAdapter();
        mListView = (ListView) findViewById(R.id.plugin_list);
        mNoPluginTextView = (TextView) findViewById(R.id.no_plugin);
    }

    private void initData() {
        ///storage/emulated/0/DynamicLoadHost
        String pluginFolder = Environment.getExternalStorageDirectory() + "/DynamicLoadHost";
        File file = new File(pluginFolder);
        File[] plugins = file.listFiles();
        if (plugins == null || plugins.length == 0) {
            mNoPluginTextView.setVisibility(View.VISIBLE);
            return;
        }

        for (File plugin : plugins) {
            PluginItem item = new PluginItem();
            item.pluginPath = plugin.getAbsolutePath();
            item.packageInfo = DLUtils.getPackageInfo(this, item.pluginPath);
            if (MyContant.list.contains(item.packageInfo.packageName)) {
                continue;
            }
            MyContant.list.add(item.packageInfo.packageName);
            mPluginItems.add(item);
            DLPluginManager.getInstance(this).loadApk(item.pluginPath);//只加载一次 其实只是解析了apk包 构建了apk解析后的对象DLPluginPackage包含属性：DexClassLoader AssetManager Resources
        }

        mListView.setAdapter(mPluginAdapter);
        mListView.setOnItemClickListener(this);
        mPluginAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                DLUtils.showDialog(this, getString(R.string.action_about), getString(R.string.introducation));
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class PluginAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public PluginAdapter() {
            mInflater = MainActivity.this.getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mPluginItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mPluginItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.plugin_item, parent, false);
                holder = new ViewHolder();
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.apkName = (TextView) convertView.findViewById(R.id.apk_name);
                holder.packageName = (TextView) convertView.findViewById(R.id.package_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            PluginItem item = mPluginItems.get(position);
            PackageInfo packageInfo = item.packageInfo;
            holder.appIcon.setImageDrawable(DLUtils.getAppIcon(MainActivity.this, item.pluginPath));
            holder.appName.setText(DLUtils.getAppLabel(MainActivity.this, item.pluginPath));
            holder.apkName.setText(item.pluginPath.substring(item.pluginPath.lastIndexOf(File.separatorChar) + 1));
            holder.packageName.setText(packageInfo.applicationInfo.packageName);
            return convertView;
        }
    }

    private static class ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView apkName;
        public TextView packageName;
    }

    public static class PluginItem {//插件item
        public PackageInfo packageInfo;
        public String pluginPath;

        public PluginItem() {
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PluginItem item = mPluginItems.get(position);
        DLPluginManager pluginManager = DLPluginManager.getInstance(this);
        pluginManager.startPluginActivity(this, new DLIntent(item.packageInfo.packageName));

    }

}
