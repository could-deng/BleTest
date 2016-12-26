package com.dyq.bletest.view.widget;

public interface SwipeTrigger {
    void onPrepare();

    void onMove(int y, boolean isComplete, boolean automatic);

    void onRelease();

    void onComplete();

    void onReset();
}
