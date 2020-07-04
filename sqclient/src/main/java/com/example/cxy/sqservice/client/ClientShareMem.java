package com.example.cxy.sqservice.client;

import android.content.Context;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.cxy.sqservice.ISqService;
import com.example.cxy.sqservice.constant.DebugTag;
import com.example.cxy.sqservice.util.MemoryFileHelper;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author zgx
 * <p>
 * 通过binder把,MemoryFile 的 ParcelFileDescriptor 传到Service
 **/
public class ClientShareMem {


    public static final String TAG = DebugTag.TAG;
    private Context mContext;

    /**
     * 通过binder把,MemoryFile的ParcelFileDescriptor 传到Service
     **/
    private MemoryFile mServiceShareMemory;
    private ParcelFileDescriptor mParceServiceShareFile;

    private FileDescriptor mServiceShareFile;
    private int mFD = -1;
    /**
     * 内存大小
     */
    private static final int CONTENT_SIZE = 200 * 1;
    private byte[] mContent = new byte[CONTENT_SIZE];
    private byte[] mContentCopy = new byte[CONTENT_SIZE];

    private ISqService mISqService;


    public ClientShareMem(Context context) {
        mContext = context;
        createMemFile();
    }

    public void setISqService(ISqService s) {
        this.mISqService = s;
    }

    /***
     * 获得服务端的内存
     * **/
    private void createMemFile() {
        Log.i(TAG, "-createMemFile-");

        try {
            mServiceShareMemory = new MemoryFile(SqServiceProxy.SERVICE_PACKAGE, mContent.length);

            //这种方式会有兼容性问题
            Method method = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
            FileDescriptor fd = (FileDescriptor) method.invoke(mServiceShareMemory);

            mParceServiceShareFile = ParcelFileDescriptor.dup(fd);
            if (mServiceShareMemory != null) {
                mServiceShareMemory.allowPurging(false); //不允许自动回收
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "createMemFile err : " + e.toString());
        }
    }

    public void dataFlow(byte[] value) {
        //int 转 byte，填充到 mContent
//        Arrays.fill(mContent,0,4, (byte) value);

        try {
            Log.d(TAG, "mIRemoteService  dataFlow start mContent: " + mContent[0]
                    + ", pid = " + android.os.Process.myPid());
//            mServiceShareMemory.writeBytes(new byte[]{0x02}, 0, 0, 1);
            FileInputStream fi = new FileInputStream(mParceServiceShareFile.getFileDescriptor());
            byte[] sign = new byte[1];
            MemoryFile memoryFile = MemoryFileHelper
                    .openMemoryFile(mParceServiceShareFile, value.length+2,
                            MemoryFileHelper.OPEN_READWRITE);
            int readNum = memoryFile.readBytes(sign, 0, 0, 1);
            Log.d(TAG, "匿名共享文件标识符 : " + Arrays.toString(sign));
            if ((sign[0]&0x02) == 0x00) {//校验首Byte 0000 0000 第二位 为 0 可写入
                long time = System.currentTimeMillis();
                //MemoryFile
                mServiceShareMemory.writeBytes(value, 0, 2, value.length);
                mServiceShareMemory.writeBytes(new byte[]{0x03}, 0, 0, 1);//写入成功为 0000 0011
                Log.d(TAG, "mIRemoteService  dataFlow start mContentCopy: " + mContentCopy[0]);
                Log.d(TAG, "mIRemoteService  dataFlow writeBytes : " + (System.currentTimeMillis() - time));

                //MemoryFile获取ParcelFileDescriptor，通过Binder把ParcelFileDescriptor(int类型)传递到服务端
                //IRemoteService - 传递到服务端
    //            Log.d(TAG, "客户端传递Byte数组数据 : " + Arrays.toString(mContent) +"，数组长度："+mContent.length
    //                    +"， 花费时间为" + (System.currentTimeMillis() - time)+"ms");
                Log.d(TAG, "客户端传递Byte数组数据 : " + Arrays.toString(value) + "，数组长度：" + value.length
                        + "， 花费时间为" + (System.currentTimeMillis() - time) + "ms");
                mISqService.dataFlow(mParceServiceShareFile, value.length+2);
                time = System.currentTimeMillis();
                Log.d(TAG, "mIRemoteService  dataFlow release : " + (System.currentTimeMillis() - time));

            }else {
                Log.d(TAG, "匿名共享文件禁止写入");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "dataFlow err : " + e.toString());

        }
    }

    private void releaseShareMemory() {
        try {
            mParceServiceShareFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mServiceShareMemory == null) {
            return;
        }
        mServiceShareMemory.close();
        mServiceShareMemory = null;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        releaseShareMemory();
    }
}
