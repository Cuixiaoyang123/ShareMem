package com.example.cxy.sharemem;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cxy.sqservice.client.SqServiceProxy;
import com.example.cxy.sqservice.constant.DebugTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private int count = 0;
    private Context mContext;
    private List<byte[]> dataList = new ArrayList<byte[]>();
    private int result = 0;  //是否成功绑定远程服务
    private TextView tv,finishedTv;

    // Used to load the 'native-lib' library on application startup.


    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        initData();
        setContentView(R.layout.activity_main);
        // Example of a call to a native method
        tv = findViewById(R.id.sample_text);
        finishedTv = findViewById(R.id.finished_text);
        tv.setText("数据："+ Arrays.toString(dataList.get(0)));
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (result == 0 ) {
                    Toast.makeText(mContext, "请先开启ShareMemB进程Service", Toast.LENGTH_SHORT).show();
                    return;
                }
                int re;
                byte[] d = new byte[]{};
                d = dataList.get(1);
                re = SqServiceProxy.pushData(dataList.get(count%dataList.size()));
                count++;
                tv.setText("数据："+ Arrays.toString(dataList.get(count%dataList.size())));
                finishedTv.setText("已发送数据："+ Arrays.toString(dataList.get((count-1)%dataList.size())));
                if (re == 0) {
                    result = 0;
                    Toast.makeText(mContext, "请先开启ShareMemB进程Service", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "成功向ShareMemB进程发送数据", Toast.LENGTH_SHORT).show();
                }
            }
        });
//        tv.setText(stringFromJNI());

        Log.i(DebugTag.TAG, "SqServiceApplication onCreate pid = " +
                android.os.Process.myPid() + ", getProcessName = " + getProcessName(this,
                android.os.Process.myPid()));

        if ("com.example.cxy.sharemem".equals(getProcessName(this,
                android.os.Process.myPid()))) {
            result = SqServiceProxy.init(this, getMainLooper(), null);
            if (result == 1) {
                SqServiceProxy.pushData(new byte[]{6, 6, 6, 12, 14, 32, 1, 5, 23, 7, 7});
            }
        }
    }

    private void initData() {
        dataList.add(new byte[]{1, 1, 5, 2, 6, 12, 14, 32, 1, 5, 23, 7, 7});
        dataList.add(new byte[]{12, 17, 32, 1, 5, 23, 7, 8});
        dataList.add(new byte[]{6, 6, 6, 12, 14, 32, 1, 5, 23, 7, 7,1, 5, 23, 7, 8,2, 17, 32, 1,});
    }

    public static String getProcessName(Context cxt, int pid) {
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
