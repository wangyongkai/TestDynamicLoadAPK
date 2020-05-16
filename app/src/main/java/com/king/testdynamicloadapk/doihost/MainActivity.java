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

import dalvik.system.DexFile;

public class MainActivity extends Activity implements OnItemClickListener {

    public static final String FROM = "extra.from";
    public static final int FROM_INTERNAL = 0;
    public static final int FROM_EXTERNAL = 1;

    private ArrayList<PluginItem> mPluginItems = new ArrayList<PluginItem>();
    private PluginAdapter mPluginAdapter;

    private ListView mListView;
    private TextView mNoPluginTextView;
    //------------------------------------------------------------------------


    //任玉刚的博客  《开发艺术探索》作者
    //  https://blog.csdn.net/singwhatiwanna


    //dex如何加载   资源如何加载   so如何加载 ？

    //------------------------------------------------------------------------


    //问题1：so文件的加载原理
    //通过查看app的默认类加载器dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/com.king.testdynamicloadapk-2/base.apk"],nativeLibraryDirectories=[/data/app/com.king.testdynamicloadapk-2/lib/x86, /system/lib, /vendor/lib]]]
    //nativeLibraryDirectories就是加载so文件的路径(私有路径不可写操作)，可以传一个路径数组。当用到so时，System.loadlibrary就是从这些路径去找对应名字的so加载，不调用不会主动去加载so。System.load(filepath)可以加载特定路径下的so。


    //------------------------------------------------------------------------

    //问题2：插件中的so库加载过程
//加载插件的类加载器DexClassLoader，构造时传入so的加载路径librarySearchPath 这个就是指定本地so的路径 所以把所有插件的so都复制到这个路径下
    //本项目中在加载插件apk的时候 将其中的so复制到/data/user/0/com.king.testdynamicloadapk/app_pluginlib目录下
    //然后利用DexClassLoader传进这个so的path进行动态加载apk中的so文件

    //------------------------------------------------------------------------


    //问题3：如何保证插件中使用的共有的类例如dllibrary库中的类，每个插件共享一份，而不是每个插件都加载各自的一份？
    // 每一个插件apk 新建一个DexClassLoader 这样导致每个插件里的类都是单独的
    // 即使类名完全一致 也是两个不同的类（因为类加载器不同） 。本项目中将加载各个插件的DexClassLoader的父类加载器设置为PathClassLoader（即壳子的类加载器）
    // 双亲委派原则保证共享的类只是壳子的类加载器加载一次。
    //
    //
    //
    //问题：插件和壳子都依赖dllibrary库，并且都需要打包。如果两者打包时依赖dllibrary库不一致，会怎么样？
    // 例如：插件打好包后 又在壳的dllibrary修改了代码 比如增加一个变量 打包壳子后加载插件类时报错
    // java.lang.IncompatibleClassChangeError: Structural change of com.ryg.dynamicload.DLBasePluginActivity
    // is hazardous (/data/user/0/com.king.testdynamicloadapk/app_dex/plugin-debug.dex at compile time,
    // /data/app/com.king.testdynamicloadapk-2/oat/x86/base.odex at runtime): Instance field count off: 6 vs 7
    //    Lcom/ryg/dynamicload/DLBasePluginActivity; (Compile time):


    //------------------------------------------------------------------------


    //问题4：类是用到才加载的吗？  插件dex是如何被加载的
    // 插件MainActivity中用到了一个类TestA 可是项目中只是调用了loadPluginClass加载了MainActivity一个类
    // 但是在MainActivity中却可以使用TestA

    //个人理解：因为dexclassloader写了dex文件的路径 所以加载一个类 会顺便把dex中的与这个类相关的类都加载  相关都加载还是整个dex都加载？？


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


    //问题8：android中的类加载器？
    //ContextImpl中获取的类加载器是SystemClassLoader类加载器
    //而SystemClassLoader类加载器的实例其实是PathClassLoader加载器
    //结论：Android中默认获取的类加载器实际是PathClassLoader对象。而传入PathClassLoader对象的父类加载器是BootClassLoader加载器。

    //    /**
    //     * Encapsulates the set of parallel capable loader types.
    //     */
    //    private static ClassLoader createSystemClassLoader() {
    //        String classPath = System.getProperty("java.class.path", ".");
    //        String librarySearchPath = System.getProperty("java.library.path", "");
    //
    //        // String[] paths = classPath.split(":");
    //        // URL[] urls = new URL[paths.length];
    //        // for (int i = 0; i < paths.length; i++) {
    //        // try {
    //        // urls[i] = new URL("file://" + paths[i]);
    //        // }
    //        // catch (Exception ex) {
    //        // ex.printStackTrace();
    //        // }
    //        // }
    //        //
    //        // return new java.net.URLClassLoader(urls, null);
    //
    //        // TODO Make this a java.net.URLClassLoader once we have those?
    //        return new PathClassLoader(classPath, librarySearchPath, BootClassLoader.getInstance());
    //    }


    //问题：类加载原理：
//如果一个类加载器收到了类加载器的请求.它首先不会自己去尝试加载这个类.而是把这个请求委派给父加载器去完成.每个层次的类加载器都是如此.因此所有的加载请求最终都会传送到
// Bootstrap类加载器(启动类加载器)中.只有父类加载反馈自己无法加载这个请求(它的搜索范围中没有找到所需的类)时.子加载器才会尝试自己去加载。
//个人理解：类加载器先判断当前要加载的类能不能找到。如果找不到则委派给父类而不是自己去加载。


    //个人理解：一个apk安装后的dex中的自定义activity类是用PathClassLoader加载的。而继承的Activity类是用BootClassLoader加载的（dvm中所有应用公用一份Activity类 ？）。
//任何运行的Android应用至少包含有两个 ClassLoader，每个应用中的PathClassLoader拥有同一个parent即是BootClassLoader，这样就保证了系统代码共享以及应用代码隔离。


    // 问题：DexClassLoader或者PathClassLoader的区别
    //DexClassLoader可以指定优化后的dex的存储路径 而PathClassLoader没有指定的参数
    //继承关系：1.DexClassLoader-->BaseDexClassLoader-->ClassLoader.   2.PathClassLoader-->BaseDexClassLoader-->ClassLoader.


    //问题：类加载器和父类加载器是继承关系吗？
    //不是继承。  依赖 < 关联 < 聚合 < 组合


//几个方法区别：
//findLoadedClass(String) 调用这个方法，查看这个Class是否已经别加载
//findClass() 根据名称或位置加载.class字节码
//definclass()把字节码转化为Class


//loadClass源码
//    protected Class<?> loadClass(String name, boolean resolve)
//        throws ClassNotFoundException
//    {
//            // First, check if the class has already been loaded
//            Class c = findLoadedClass(name);
//            if (c == null) {
//                long t0 = System.nanoTime();
//                try {
//                    if (parent != null) {
//                        c = parent.loadClass(name, false);
//                    } else {
//                        c = findBootstrapClassOrNull(name);
//                    }
//                } catch (ClassNotFoundException e) {//!!!!!!注意此处：ClassNotFoundException
//                    // ClassNotFoundException thrown if class not found
//                    // from the non-null parent class loader
//                }
//
//                if (c == null) {
//                    // If still not found, then invoke findClass in order
//                    // to find the class.
//                    long t1 = System.nanoTime();
//                    c = findClass(name);
//
//                    // this is the defining class loader; record the stats
//                }
//            }
//            return c;
//    }


    //------------------------------------------------------------------------


    //问题9：多dex下  如何先加载主dex  后加载从dex
    //Dalivk只加载app的主dex（classes.dex），因此app需要手动加载子dex（classesN.dex）。Mutlidex.install就是干这个事的。

    //Mutlidex 源码分析  原理
    //1.获取加载主dex的pathclassloader
    //2.反射调用DexPathList的makeDexElements将剩余的dex文件加载获取Element[]
    //3.跟之前的Element[]合并

    //Mutlidex跟插件化不一样，Mutlidex是对同一个pathclassloader进行修改。（补丁包？）      插件化是用dexclassloader加载。


    //------------------------------------------------------------------------


    //问题10：加载dex loadDexFile就是加载类了吗
    //个人理解loadDexFile只是将dex文件映射到内存中。真正触发加载是在用到某个类时findClass时dex.loadClassBinaryName触发

//BaseDexClassLoader源码解析

//    public class BaseDexClassLoader extends ClassLoader {
//        // 需要加载的dex列表
//        private final DexPathList pathList;
//        // dexPath要加载的dex文件所在的路径，optimizedDirectory是odex将dexPath
//        // 处dex优化后输出到的路径，这个路径必须是手机内部路劲，libraryPath是需要
//        // 加载的C/C++库路径，parent是父类加载器对象
//        public BaseDexClassLoader(String dexPath, File optimizedDirectory,
//                                  String libraryPath, ClassLoader parent) {
//            super(parent);
//            this.pathList = new DexPathList(this, dexPath, libraryPath, optimizedDirectory);
//        }
//
//        @Override
//        protected Class<?> findClass(String name) throws ClassNotFoundException {
//            List<Throwable> suppressedExceptions = new ArrayList<Throwable>();
//            // 使用pathList对象查找name类
//            Class c = pathList.findClass(name, suppressedExceptions);
//            return c;
//        }
//    }


//    // Element类代表dex文件或资源文件的路径元素
//    /*package*/ static class Element {
//        private final File file;
//        private final boolean isDirectory;
//        private final File zip;
//        private final DexFile dexFile;
//
//        private ZipFile zipFile;
//        private boolean initialized;
//
//        // file文件，是否是目录，zip文件通常都是apk或jar文件，dexFile就是.dex文件
//        public Element(File file, boolean isDirectory, File zip, DexFile dexFile) {
//            this.file = file;
//            this.isDirectory = isDirectory;
//            this.zip = zip;
//            this.dexFile = dexFile;
//        }
//    }


//    /*package*/ final class DexPathList {
//        private static final String DEX_SUFFIX = ".dex";
//        private final ClassLoader definingContext;
//        //
//        private final Element[] dexElements;
//        // 本地库目录
//        private final File[] nativeLibraryDirectories;
//
//        public DexPathList(ClassLoader definingContext, String dexPath,
//                           String libraryPath, File optimizedDirectory) {
//            // 当前类加载器的父类加载器
//            this.definingContext = definingContext;
//            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
//            // 根据输入的dexPath创建dex元素对象
//            this.dexElements = makeDexElements(splitDexPath(dexPath), optimizedDirectory,
//                    suppressedExceptions);
//            if (suppressedExceptions.size() > 0) {
//                this.dexElementsSuppressedExceptions =
//                        suppressedExceptions.toArray(new IOException[suppressedExceptions.size()]);
//            } else {
//                dexElementsSuppressedExceptions = null;
//            }
//            this.nativeLibraryDirectories = splitLibraryPath(libraryPath);
//        }
//    }


//    private static Element[] makeDexElements(ArrayList<File> files, File optimizedDirectory,
//                                             ArrayList<IOException> suppressedExceptions) {
//        ArrayList<Element> elements = new ArrayList<Element>();
//        // 所有从dexPath找到的文件
//        for (File file : files) {
//            File zip = null;
//            DexFile dex = null;
//            String name = file.getName();
//            // 如果是文件夹，就直接将路径添加到Element中
//            if (file.isDirectory()) {
//                elements.add(new Element(file, true, null, null));
//            } else if (file.isFile()){
//                // 如果是文件且文件名以.dex结束
//                if (name.endsWith(DEX_SUFFIX)) {
//                    try {
//                        // 直接从.dex文件生成DexFile对象
//                        dex = loadDexFile(file, optimizedDirectory);//底层openDexFileNative代码中主要是对dex文件进行了优化操作，并将优将优化后得dex文件（odex文件）通过mmap映射到内存中。
//                    } catch (IOException ex) {
//                        System.logE("Unable to load dex file: " + file, ex);
//                    }
//                } else {
//                    zip = file;
//
//                    try {
//                        // 从APK/JAR文件中读取dex文件
//                        dex = loadDexFile(file, optimizedDirectory);
//                    } catch (IOException suppressed) {
//                        suppressedExceptions.add(suppressed);
//                    }
//                }
//            } else {
//                System.logW("ClassLoader referenced unknown path: " + file);
//            }
//
//            if ((zip != null) || (dex != null)) {
//                elements.add(new Element(file, false, zip, dex));
//            }
//        }
//
//        return elements.toArray(new Element[elements.size()]);
//    }


//    // BaseDexClassLoader中的方法 加载名字为name的class对象
//    public Class findClass(String name, List<Throwable> suppressed) {
//        // 遍历从dexPath查询到的dex和资源Element
//        for (Element element : dexElements) {
//            DexFile dex = element.dexFile;
//            // 如果当前的Element是dex文件元素
//            if (dex != null) {
//                // 使用DexFile.loadClassBinaryName加载类
//                Class clazz = dex.loadClassBinaryName(name, definingContext, suppressed);
//                if (clazz != null) {
//                    return clazz;
//                }
//            }
//        }
//        if (dexElementsSuppressedExceptions != null) {
//            suppressed.addAll(Arrays.asList(dexElementsSuppressedExceptions));
//        }
//        return null;
//    }


    //------------------------------------------------------------------------


    //问题：点击应用图标启动后 系统从哪儿加载app的dex
    ///data/app/com.king.testdynamicloadapk-2/base.apk


    //------------------------------------------------------------------------


    //问题1：DLBasePluginActivity为什么要继承Activity 插件activity本来就是一个普通的类 父类为啥要继承activity呢
    //继承Activity是为了插件项目能单独运行。


    //------------------------------------------------------------------------


    //问题2：插件内第一个页面跳转第二个页面 startactivity直接可行吗
    //不可行。第二个页面没有注册。


    //------------------------------------------------------------------------


    //问题3：从一个插件的界面跳转到另一个插件的界面 如何实现？？？
    //DLPluginManager跳转即可。

    //------------------------------------------------------------------------


    //问题4：如何保证插件单独运行没有问题？
    //每个插件的入口activity在清单文件中配置，相当于一个单独的app项目。插件中其他页面只能通过DLPluginManager用
    //代理的方式执行。因为没注册。


    //------------------------------------------------------------------------


    //问题5:菜单文件什么时候加载？


    //------------------------------------------------------------------------

    //DLProxyActivity DLProxyFragmentActivity启动模式不能设置为单例。因为插件中的每个界面启动都要启动一次代理界面。
    //也就是每次启动代理界面都实例一次界面。

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


//查看app的默认类加载器
        ClassLoader classLoader = getClassLoader();//dalvik.system.PathClassLoader
        if (classLoader != null) {
            Log.i("MainActivity", "[onCreate] classLoader " + i + " : " + classLoader.toString());
            //dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/com.king.testdynamicloadapk-2/base.apk"],nativeLibraryDirectories=[/data/app/com.king.testdynamicloadapk-2/lib/x86, /system/lib, /vendor/lib]]]
            while (classLoader.getParent() != null) {  //可知so可以从3个路径加载2/lib/x86, /system/lib, /vendor/lib  这3个地方都是不能动的私有空间？
                classLoader = classLoader.getParent();
                i++;
                Log.i("MainActivity", "[onCreate] classLoader " + i + " : " + classLoader.toString());
                //java.lang.BootClassLoader@8ed6ea9
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
