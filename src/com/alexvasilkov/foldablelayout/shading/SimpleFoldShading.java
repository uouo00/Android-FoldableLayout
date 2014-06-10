package com.alexvasilkov.foldablelayout.shading;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;

/**
 * һ���򵥵Ļ����ɲ���Ч
 *
 */
public class SimpleFoldShading implements FoldShading {

    private static final int SHADOW_COLOR = Color.BLACK;
    private static final int SHADOW_MAX_ALPHA = 192;

    private final Paint mSolidShadow;

    public SimpleFoldShading() {
        mSolidShadow = new Paint();
        mSolidShadow.setColor(SHADOW_COLOR);
    }

    @Override
    public void onPreDraw(Canvas canvas, Rect bounds, float rotation, int gravity) {
        // NO-OP
    }

    @Override
    public void onPostDraw(Canvas canvas, Rect bounds, float rotation, int gravity) {
        float intencity = getShadowIntencity(rotation, gravity);
        //����0��ʾ��Ҫ����һ���ɲ�
        if (intencity > 0) {
            int alpha = (int) (SHADOW_MAX_ALPHA * intencity);
            mSolidShadow.setAlpha(alpha);
            canvas.drawRect(bounds, mSolidShadow);
        }
    }

    private float getShadowIntencity(float rotation, int gravity) {
        float intencity = 0;
        //���ݵ�ǰ��ת�ĽǶȺ�part view��gravity���ж��Ƿ���Ҫ���������һ���ɲ�
        if (gravity == Gravity.TOP) {
        	//�����������top part view��������ת
            if (rotation > -90 && rotation < 0) { // (-90; 0) - rotation is applied
                intencity = -rotation / 90f;
            }
        } else {
        	//�����������bottom part view��������ת
            if (rotation > 0 && rotation < 90) { // (0; 90) - rotation is applied
                intencity = rotation / 90f;
            }
        }
        return intencity;
    }

}
