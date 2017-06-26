package com.dyq.bletest;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dyq.bletest.common.FileUtils;
import com.dyq.bletest.model.database.DaoMaster;
import com.dyq.bletest.model.database.DaoSession;

/**
 * Created by dengyuanqiang on 2017/6/20.
 */

public class MixApp extends Application {

    private static Context mContext;
    private static DaoSession daoSession;
    private static DaoMaster daoMaster;
    private static SQLiteDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        FileUtils.makeDirs(Config.PATH_APP_STORAGE);
        FileUtils.makeDirs(Config.PATH_HR_STORAGE);
    }

    public static Context getContext(){
        return mContext;
    }

    public static DaoSession getDaoSession(Context context){
        if(daoSession == null){
            if(daoMaster == null){
                daoMaster = getDaoMaster(context);
            }
            daoSession = daoMaster.newSession();
        }
        return daoSession;
    }

    public static DaoMaster getDaoMaster(Context context){
        if(daoMaster == null){
            DaoMaster.OpenHelper helper = new DaoMaster.DevOpenHelper(context,"realm.db",null);
            daoMaster = new DaoMaster(helper.getWritableDatabase());
        }
        return daoMaster;
    }

    public static SQLiteDatabase getSqlDatabase(Context context){
        if(daoSession == null){
            if(daoMaster == null){
                daoMaster = getDaoMaster(context);
            }
            db = daoMaster.getDatabase();
        }
        return db;
    }
    @Override
    public void onTerminate() {
        super.onTerminate();
        getSqlDatabase(this).close();//程序推出时，关闭数据库
    }
}
