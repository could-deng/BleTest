package com.dyq.bletest;
        import de.greenrobot.daogenerator.DaoGenerator;
        import de.greenrobot.daogenerator.Entity;
        import de.greenrobot.daogenerator.Schema;

public class MyClass {

    public static void main(String[] args) throws Exception{
        Schema schema = new Schema(1,"com.dyq.bletest.model.database");
        addInfoDB(schema);
        new DaoGenerator().generateAll(schema,"/Users/dengyuanqiang/Documents/github/BleTest/app/src/main/java");
    }

    private static void addInfoDB(Schema schema){
        Entity info = schema.addEntity("HrInfo");
        info.addIdProperty().autoincrement();
        info.addStringProperty("mac_address").javaDocGetterAndSetter("蓝牙设备mac地址");
        info.addStringProperty("identify_start_time").index().javaDocGetterAndSetter("记录开始时间,记录唯一标示");
        info.addStringProperty("time").javaDocGetterAndSetter("记录下来的时间");
        info.addStringProperty("matter").javaDocGetterAndSetter("事件关键字");//事件包括：开始、正常、断开、链接、
        info.addStringProperty("value").javaDocGetterAndSetter("值");//不同事件存的值不同
    }
}

