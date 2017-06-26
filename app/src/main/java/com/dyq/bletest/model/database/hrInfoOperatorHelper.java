package com.dyq.bletest.model.database;

import android.content.Context;

import com.dyq.bletest.MixApp;
import com.dyq.bletest.common.Logger;
import com.dyq.bletest.common.ThreadManager;

import java.util.List;

import de.greenrobot.dao.async.AsyncOperationListener;
import de.greenrobot.dao.async.AsyncSession;
import de.greenrobot.dao.query.QueryBuilder;

/**
 * Created by dengyuanqiang on 2017/6/20.
 */

public class hrInfoOperatorHelper {

    private static hrInfoOperatorHelper instance;
    public static hrInfoOperatorHelper getInstance(){
        if(instance == null){
            instance = new hrInfoOperatorHelper();
        }
        return instance;
    }

    public void asnycGetAllLogRecord(Context context, String startTime, AsyncOperationListener listener){
        HrInfoDao dao = MixApp.getDaoSession(MixApp.getContext()).getHrInfoDao();
        QueryBuilder<HrInfo> qd = dao.queryBuilder();
        qd.where(HrInfoDao.Properties.Identify_start_time.eq(startTime))
                .orderAsc(HrInfoDao.Properties.Mac_address)
                .orderAsc(HrInfoDao.Properties.Time);
        AsyncSession asyncSession = MixApp.getDaoSession(context).startAsyncSession();
        asyncSession.setListener(listener);
        asyncSession.queryList(qd.build());
    }


    /**
     * 往数据库表上插入数据操作
     */
    public void insertLogRecordList(final Context context, final List<HrInfo> infoList){
        if(context == null){
            return;
        }
        if(infoList!=null && infoList.size()>0){
            ThreadManager.getSubThread1Handler().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        MixApp.getDaoSession(MixApp.getContext()).getHrInfoDao().insertInTx(infoList);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    public void insertLogRecord(final Context context, final HrInfo info){
        if(context == null){
            return;
        }
        if(info!=null){
            ThreadManager.getSubThread1Handler().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        MixApp.getDaoSession(MixApp.getContext()).getHrInfoDao().insert(info);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
