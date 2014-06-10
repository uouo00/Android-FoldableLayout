package com.alexvasilkov.foldablelayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.alexvasilkov.foldablelayout.shading.FoldShading;

/**
 * {@link FoldableListLayout}��child view
 */
@SuppressLint("NewApi")
public class FoldableItemLayout extends FrameLayout {

    private static final int CAMERA_DISTANCE = 48;
    private static final float CAMERA_DISTANCE_MAGIC_FACTOR = 8f / CAMERA_DISTANCE;

    //�Ƿ���Ҫ�����Զ�Scale
    private boolean mIsAutoScaleEnabled;

    //���Կ�����FoldableItemLayout��Ψһchild view������ȥ�������е�views,��ת�����У������ṩһ��mCacheCanvasȥ����ת����
    private BaseLayout mBaseLayout;
    //��ת��Ҫ���������view�г����¶�����������
    private PartView mTopPart, mBottomPart;

    //��ǰview �� ��
    private int mWidth, mHeight;
    //��������view���ɵ�bitmap����Ҫ������������תʱ���������в���
    private Bitmap mCacheBitmap;

    //��true ��ʾ��ǰ���ڷ�ת����
    private boolean mIsInTransformation;

    //��¼��ǰ��ת�ĽǶ� ���ֵ�Ǳ�180ģ���� ����ֵ��Χ��(-180,180)
    private float mFoldRotation;
    //��¼��ǰviewѹ���ı�ֵ
    private float mScale;
    //���mScaleһ��ʹ��
    private float mRollingDistance;

    public FoldableItemLayout(Context context) {
        super(context);
        init(context);
    }

    public FoldableItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FoldableItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
    	//��Ϸ�ת�� ��Ҫ���ṩһ�黭��
        mBaseLayout = new BaseLayout(this);

        //����ת�õĶ���view
        mTopPart = new PartView(this, Gravity.TOP);
        mBottomPart = new PartView(this, Gravity.BOTTOM);
        
        //��ʼ����ǰ״̬�ǷǷ�ת
        setInTransformation(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //mBaseLayoutҪ��ȫ���ǵ�FoldableItemLayout������mBaseLayoutҪ�ӹܳ�FoldableItemLayout�����3��child view
        mBaseLayout.moveInflatedChildren(this, 3); // skipping mBaseLayout & mTopPart & mBottomPart views
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;

        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
            mCacheBitmap = null;
        }

        //��ʼ��һ��͵�ǰviewһ����С��bitmap��ΪmBaseLayout�Ļ���
        mCacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        mBaseLayout.setCacheCanvas(new Canvas(mCacheBitmap));

        //mTopPart��mBottomPart�����view����mCacheBitmap������һЩ�任�Ͳü�
        mTopPart.setCacheBitmap(mCacheBitmap);
        mBottomPart.setCacheBitmap(mCacheBitmap);
    }

    /**
     * ���ݴ���ĽǶ�ֵrotation����ת����
     * @param rotation
     */
    public void setFoldRotation(float rotation) {
    	//����Ƕ�ֵ
        mFoldRotation = rotation;

        //mTopPart��mBottomPartȥ����ת
        mTopPart.applyFoldRotation(rotation);
        mBottomPart.applyFoldRotation(rotation);

        //��Ϊ0��ʾ����ǰ��ʵ�ʽǶ���-180|0|180,��ƽ�̵ģ���ô�رնԷ�ת���̵Ĵ���
        setInTransformation(rotation != 0);

        if (mIsAutoScaleEnabled) {
            float viewScale = 1.0f;
            if (mWidth > 0) {
                float dW = (float) (mHeight * Math.abs(Math.sin(Math.toRadians(rotation)))) * CAMERA_DISTANCE_MAGIC_FACTOR;
                viewScale = mWidth / (mWidth + dW);
            }
            //Scale����
            setScale(viewScale);
        }
    }

    /**
     * ��ȡ��ǰ��mFoldRotationֵ ���ֵ�Ǳ�180ģ���� ����ֵ��Χ��(-180,180)
     * @return
     */
    public float getFoldRotation() {
        return mFoldRotation;
    }

    /**
     * �Ե�ǰview������ת��mTopPart��mBottomPart��scale����
     * @param scale
     */
    public void setScale(float scale) {
        mScale = scale;
        mTopPart.applyScale(scale);
        mBottomPart.applyScale(scale);
    }

    public float getScale() {
        return mScale;
    }

    /**
     * ����λ�����¼����м��۵���
     * @param distance
     */
    public void setRollingDistance(float distance) {
        mRollingDistance = distance;
        mTopPart.applyRollingDistance(distance, mScale);
        mBottomPart.applyRollingDistance(distance, mScale);
    }

    public float getRollingDistance() {
        return mRollingDistance;
    }

    /**
     * ���õ�ǰ�Ƿ����ڷ�ת����
     * @param isInTransformation ��true ��ʾ�����ڷ�ת���̣�������mTopPart��mBottomPart
     */
    private void setInTransformation(boolean isInTransformation) {
        if (mIsInTransformation == isInTransformation) return;
        mIsInTransformation = isInTransformation;

        //����isInTransformation���ж������Ƿ��ڷ�ת���̣�������ǰ��draw
        mBaseLayout.setDrawToCache(isInTransformation);
        mTopPart.setVisibility(isInTransformation ? VISIBLE : INVISIBLE);
        mBottomPart.setVisibility(isInTransformation ? VISIBLE : INVISIBLE);
    }

    /**
     * �����Ƿ����Զ�����Scale����
     * @param isAutoScaleEnabled
     */
    public void setAutoScaleEnabled(boolean isAutoScaleEnabled) {
        mIsAutoScaleEnabled = isAutoScaleEnabled;
    }

    /**
     * һ��root view
     * ���Կ�����FoldableItemLayout��Ψһchild view������ȥ�������е�views
     */
    public FrameLayout getBaseLayout() {
        return mBaseLayout;
    }

    public void setLayoutVisibleBounds(Rect visibleBounds) {
        mTopPart.setVisibleBounds(visibleBounds);
        mBottomPart.setVisibleBounds(visibleBounds);
    }

    /**
     * ��Ҫ�����ڷ�ת��view����Ч�õ�
     * @param shading
     */
    public void setFoldShading(FoldShading shading) {
        mTopPart.setFoldShading(shading);
        mBottomPart.setFoldShading(shading);
    }


    /**
     * ���Կ�����FoldableItemLayout��Ψһchild view������ȥ�������е�child views,��ת�����У������ṩһ��mCacheCanvasȥ����ת����
     * ����Ҫ����תʱ����ָ�������draw
     *
     */
    private static class BaseLayout extends FrameLayout {

        private Canvas mCacheCanvas;
        private boolean mIsDrawToCache;

        @SuppressWarnings("deprecation")
        private BaseLayout(FoldableItemLayout layout) {
            super(layout.getContext());

            //���Լ���Ϊchild view��ӵ�FoldableItemLayout��ȥ
            int matchParent = ViewGroup.LayoutParams.MATCH_PARENT;
            LayoutParams params = new LayoutParams(matchParent, matchParent);
            layout.addView(this, params);

            //��BaseLayout��������������������Ҫ�����±���
            this.setBackgroundDrawable(layout.getBackground());
            layout.setBackgroundDrawable(null);

            setWillNotDraw(false);
        }

        //��FoldableItemLayout��child count����firstSkippedItems��child view��ӵ�BaseLayout��ȥ
        private void moveInflatedChildren(FoldableItemLayout layout, int firstSkippedItems) {
            while (layout.getChildCount() > firstSkippedItems) {
                View view = layout.getChildAt(firstSkippedItems);
                LayoutParams params = (LayoutParams) view.getLayoutParams();
                layout.removeViewAt(firstSkippedItems);
                addView(view, params);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (mIsDrawToCache) {//����תʱ�����������֧
                mCacheCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                super.draw(mCacheCanvas);
            } else {
                super.draw(canvas);
            }
        }
        
        //����һ����ת������Ҫ�õ�Canvas
        private void setCacheCanvas(Canvas cacheCanvas) {
            mCacheCanvas = cacheCanvas;
        }

        //��ת�����У�������Ϊtrue
        private void setDrawToCache(boolean drawToCache) {
            if (mIsDrawToCache == drawToCache) return;
            mIsDrawToCache = drawToCache;
            invalidate();
        }

    }

    /**
     * ����ת����ĺ����� ���������ڷ�ת��view(top part or bottom part)����cached bitmap��overlay shadows
     * @author zhuchen
     *
     */
    private static class PartView extends View {

    	//��ΪGravity.TOP��ʾ���ϲ��֣���ΪGravity.BOTTOM��ʾ���²���
        private final int mGravity;

        //�����canvas��bitmap ���еĴ���draw��������
        private Bitmap mBitmap;
        //����mGravity��Ϥ �жϵ�ǰ�������ϰ벿�ֻ����°벿��
        private final Rect mBitmapBounds = new Rect();

        //���mBitmapBounds�����õ�
        private float mClippingFactor = 0.5f;

        //һpaint ���ر�����
        private final Paint mBitmapPaint;

        //��¼��ǰview�Ŀɼ���Χ
        private Rect mVisibleBounds;

        //�����ֵ�����ж��Ƿ�ǰview����ʾ�õ�
        private int mInternalVisibility;
        private int mExtrenalVisibility;

        //��¼��ǰҪ����ת�ĽǶ�
        private float mLocalFoldRotation;
        //��תview����Ч�õ�
        private FoldShading mShading;

        public PartView(FoldableItemLayout parent, int gravity) {
            super(parent.getContext());
            mGravity = gravity;

            final int matchParent = LayoutParams.MATCH_PARENT;
            parent.addView(this, new LayoutParams(matchParent, matchParent));
            //ʹ��rotationX��rotationYʱ����ʹview�����ʱ�����ʹ���������������Ч��
            setCameraDistance(CAMERA_DISTANCE * getResources().getDisplayMetrics().densityDpi);

            mBitmapPaint = new Paint();
            mBitmapPaint.setDither(true);
            mBitmapPaint.setFilterBitmap(true);

            setWillNotDraw(false);
        }

        /**
         * ���÷�ת�����bitmap������ѡȡbitmap�ķ�Χ
         * @param bitmap
         */
        private void setCacheBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            calculateBitmapBounds();
        }

        /**
         * ���õ�ǰview�Ŀɼ���Χ ����mBitmapBounds�����ж��Ƿ��н���
         * @param visibleBounds
         */
        private void setVisibleBounds(Rect visibleBounds) {
            mVisibleBounds = visibleBounds;
            calculateBitmapBounds();
        }

        //��Ҫ�����ڷ�ת��view����Ч�õ�
        private void setFoldShading(FoldShading shading) {
            mShading = shading;
        }

        /**
         * ����mGravity����mBitmapBounds
         */
        private void calculateBitmapBounds() {
            if (mBitmap == null) {
                mBitmapBounds.set(0, 0, 0, 0);
            } else {
                int h = mBitmap.getHeight();
                int w = mBitmap.getWidth();
                
                //�������mClippingFactor����top��bottom,h * (1 - mClippingFactor)��h * mClippingFactor����ȥ��һ����
                //ʵ������һ���£����Ը��ݼ���mClippingFactorֵ�ķ����Ƶ�����
                int top = mGravity == Gravity.TOP ? 0 : (int) (h * (1 - mClippingFactor) - 0.5f);
                int bottom = mGravity == Gravity.TOP ? (int) (h * mClippingFactor + 0.5f) : h;

                mBitmapBounds.set(0, top, w, bottom);
                if (mVisibleBounds != null) {//��mVisibleBounds!=null �Һ�mVisibleBounds�޽�������ô����Ҫ������draw��
                    if (!mBitmapBounds.intersect(mVisibleBounds)) {
                        mBitmapBounds.set(0, 0, 0, 0); // no intersection
                    }
                }
            }

            invalidate();
        }

        private void applyFoldRotation(float rotation) {
            float position = rotation;
            //У���ж� ��ֵ֤�ķ�Χ��(-180; 180]
            while (position < 0) position += 360;
            position %= 360;
            if (position > 180) position -= 360; // now poistion within (-180; 180]
            
            float rotationX = 0;
            boolean isVisible = true;

            //����ж�Ҳ�Ƚϸ��ӣ����Լ�ϸϸ����
            //����˼·�ǣ�����ת��part view����setRotationX���䱳���part view���أ�����part view���治��
            if (mGravity == Gravity.TOP) {
                if (position <= -90 || position == 180) { // (-180; -90] || {180} - will not show
                    isVisible = false;
                } else if (position < 0) { // (-90; 0) - applying rotation
                    rotationX = position;
                }
                // [0; 180) - holding still
            } else {
                if (position >= 90) { // [90; 180] - will not show
                    isVisible = false;
                } else if (position > 0) { // (0; 90) - applying rotation
                    rotationX = position;
                }
                // else: (-180; 0] - holding still
            }

            //����ת�ķ���
            setRotationX(rotationX);

            mInternalVisibility = isVisible ? VISIBLE : INVISIBLE;
            applyVisibility();

            mLocalFoldRotation = position;

            invalidate(); // needed to draw shadow overlay
        }

        /**
         * scale����
         * @param scale
         */
        private void applyScale(float scale) {
            setScaleX(scale);
            setScaleY(scale);
        }

        /**
         * Ӧ����ת����ʱ��part view��λ��
         */
        private void applyRollingDistance(float distance, float scale) {
            // applying translation
            setTranslationY((int) (distance * scale + 0.5f));

            // computing clipping for top view (bottom clipping will be 1 - topClipping)
            final int h = getHeight() / 2;
            final float topClipping = h == 0 ? 0.5f : (h - distance) / h / 2;

            //Ĭ�����ֵ��0.5 ��part view������λ��ʱ��Ҳ��Ҫ������΢��
            mClippingFactor = mGravity == Gravity.TOP ? topClipping : 1f - topClipping;

            calculateBitmapBounds();
        }

        @Override
        public void setVisibility(int visibility) {
            mExtrenalVisibility = visibility;
            applyVisibility();
        }

        /**
         * ����mExtrenalVisibility��mInternalVisibility����ֵ������view���Ƿ�ɼ�
         */
        private void applyVisibility() {
            super.setVisibility(mExtrenalVisibility == VISIBLE ? mInternalVisibility : mExtrenalVisibility);
        }

        @Override
        public void draw(Canvas canvas) {
            if (mShading != null) mShading.onPreDraw(canvas, mBitmapBounds, mLocalFoldRotation, mGravity);
            if (mBitmap != null) canvas.drawBitmap(mBitmap, mBitmapBounds, mBitmapBounds, mBitmapPaint);
            //����ת��part view������Ч����
            if (mShading != null) mShading.onPostDraw(canvas, mBitmapBounds, mLocalFoldRotation, mGravity);
        }

    }

}
