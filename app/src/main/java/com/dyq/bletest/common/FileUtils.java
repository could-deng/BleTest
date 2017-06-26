package com.dyq.bletest.common;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Array;

/**
 * Created by dengyuanqiang on 2017/6/21.
 */

public class FileUtils {

    public static boolean makeDirs(String filePath){
        String folderName = getFolderName(filePath);
        if(folderName == null || folderName.length() == 0){
            return false;
        }
        File folder = new File(folderName);
        return (folder.exists()&& folder.isDirectory()?true:folder.mkdirs());
    }

    public static String getFolderName(String filePath){
        if(filePath == null ||filePath.length() ==0){
            return filePath;
        }
        int filePos = filePath.lastIndexOf(File.separator);
        return (filePos == -1)?"":filePath.substring(0,filePos);
    }

    public static void writeFile(final String filePath,final String contentString){
        ThreadManager.getSubThread1Handler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fos = new FileOutputStream(filePath,true);
                    fos.write(contentString.getBytes());
                    fos.flush();
                    fos.close();
                    Logger.e(Logger.DEBUG_TAG,"writeFile(),"+filePath+",finish");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
