

package com.example.cxy.sqservice.client;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.cxy.sqservice.ISqService;

import java.util.List;

import static com.example.cxy.sqservice.constant.DebugTag.TAG;

/**
 * @author zgx
 * //todo 调用服务
 **/
public class SqServiceProxy implements ServiceConnection {


    public static final String SERVICE_DEFAULT_CLASSNAME
            = "com.dreamlost.sharemenb.service.SqServiceNative";

    public static final String SERVICE_PACKAGE
            = "com.dreamlost.sharemenb.service";

    private static Context gContext;
    public static SqServiceProxy inst;

    private static String gPackageName;
    private static String gClassName;

    private static byte[] dataValue ;
    private static Boolean connected = false;
    /***
     *定时任务
     * **/
    private Worker worker;

    /***
     * 操作服务端的接口
     * **/
    private ISqService service = null;

    private static ClientShareMem mClientShareMem;

    private SqServiceProxy() {
        mClientShareMem = new ClientShareMem(gContext);
        //worker = new Worker();
        //worker.start();
    }

    public static int init(Context context, Looper looper, String packageName) {
        if (inst != null) {
            return 1;
        }
//        dataValue = (byte)data;
        gContext = context.getApplicationContext();

        gPackageName = (packageName == null ? context.getPackageName() : packageName);
        gClassName = SERVICE_DEFAULT_CLASSNAME;
        Log.i(TAG, "SqServiceProxy ： init gPackageName = " + gPackageName +
                ", gClassName = " + gClassName);

        inst = new SqServiceProxy();
        int result = inst.continueProcessTaskWrappers();
        return result;
    }

    public static int pushData(byte[] data){
        if (connected) {
            dataValue = data;
            mClientShareMem.dataFlow(dataValue);
            return 1;
        }
        return 0;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        Log.d(TAG, "onServiceConnected : remote mars service connected");

        try {
            service = ISqService.Stub.asInterface(binder);
            mClientShareMem.setISqService(service);
            connected = true;

        } catch (Exception e) {
            service = null;
            Log.e(TAG, "onServiceConnected err : " + e.toString());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service = null;
        connected = false;
        Log.d(TAG, "onServiceDisconnected : remote mars service disconnected");
    }

    private int continueProcessTaskWrappers() {
        try {
            if (service == null) {
                Log.d(TAG, String.format("开始绑定服务, packageName: %s, className: %s",
                        gPackageName, gClassName));
                Intent i = new Intent().setClassName("com.dreamlost.sharememb", "com.dreamlost.sqservice.service.SqServiceNative");
                Intent intent = createExplicitFromImplicitIntent(gContext, i);
                if (intent == null) {
                    Toast.makeText(gContext, "请先开启ShareMemB进程Service", Toast.LENGTH_SHORT).show();
                    return 0;
                }
                final Intent romoteIntent = new Intent(intent);
                if (!gContext.bindService(romoteIntent, inst, Service.BIND_AUTO_CREATE)) {
                    Log.e(TAG, "remote mars service bind failed");
                } else {
                    Log.i(TAG, "remote mars service bind ok");
                }
                // Waiting for service connected
                //检查服务是否启动，没有则启动并返回等待下一个50ms再继续；
                return 1;
            }
            //todo 从队列中获取一个任务，并给他分配一个cmdID,然后调用MarsService的send方法执行真正的发送事件。

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    private static class Worker extends Thread {

        @Override
        public void run() {

            while (true) {
                inst.continueProcessTaskWrappers();

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
