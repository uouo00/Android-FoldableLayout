package com.alexvasilkov.foldablelayout;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import com.alexvasilkov.foldablelayout.shading.FoldShading;
import com.alexvasilkov.foldablelayout.shading.SimpleFoldShading;

import java.util.LinkedList;
import java.util.Queue;

/**
 * һ��������flipboard�����巭ת�ؼ�
 */
@SuppressLint("NewApi")
public class FoldableListLayout extends FrameLayout implements GestureDetector.OnGestureListener {

	/**
	 * fling��up����ʱ����ת�����ĳ�����ʱ��
	 */
    private static final long ANIMATION_DURATION_PER_ITEM = 600;

    //child view��params����
    private static final LayoutParams PARAMS = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    
    //����child view�������
    private static final int CACHED_LAYOUTS_OFFSET = 2;

    //���ڷ�ת����ʱ�ķ�ת�ǶȻص�
    private OnFoldRotationListener mFoldRotationListener;
    //������
    private BaseAdapter mAdapter;

    //��¼��ǰ�ķ�ת�Ƕ�
    private float mFoldRotation;
    //�������С�����ת�Ƕ� һ�������Сֵ��0 ���ֵ��180*(child size - 1)
    private float mMinRotation, mMaxRotation;

/*    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }*/

    //����child view �ǵ�ǰ��Ҫdraw��
    private FoldableItemLayout mFirstLayout, mSecondLayout;
    //�����ڷ�ת��view����Ч�õ�
    private FoldShading mFoldShading;

    //���浱ǰ���е�child view
    private SparseArray<FoldableItemLayout> mFoldableLayoutsMap = new SparseArray<FoldableItemLayout>();
    //���滺���child view
    private Queue<FoldableItemLayout> mFoldableLayoutsCache = new LinkedList<FoldableItemLayout>();

    //fling��up����ʱ������ת������
    private ObjectAnimator mAnimator;
    //��ʾ���� ��ʾ���MotionEvent�¼���ʱ���
    private long mLastEventTime;
    //��ʾ���� ��¼���һ���¼��Ĵ�����
    private boolean mLastEventResult;
    //���ƴ�������
    private GestureDetector mGestureDetector;

    //����Ϊ����Ч��������С����
    private float mMinDistanceBeforeScroll;
    //��ʾ���� ��true ��ʾ���ڴ�������
    private boolean mIsScrollDetected;
    //��¼�˴ι�����ʼʱ����ǰ��Rotationֵ
    private float mScrollStartRotation;
    //��¼�˴ι�����ʼʱ�������ľ���
    private float mScrollStartDistance;

    public FoldableListLayout(Context context) {
        super(context);
        init(context);
    }

    public FoldableListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FoldableListLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    //��ʼ��
    private void init(Context context) {
        mGestureDetector = new GestureDetector(context, this);
        //�������ʼ�� �ڶ�������foldRotation��ʾ �����Ļص�������setFoldRotation(..)������õ�
        mAnimator = ObjectAnimator.ofFloat(this, "foldRotation", 0);
        mMinDistanceBeforeScroll = ViewConfiguration.get(context).getScaledPagingTouchSlop();

        mFoldShading = new SimpleFoldShading();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        //��Ҫdraw�Ķ���child view
        if (mFirstLayout != null) mFirstLayout.draw(canvas);
        if (mSecondLayout != null) mSecondLayout.draw(canvas);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return getCount() > 0;//��child �������¼� ��������ת����
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return processTouch(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return processTouch(event);
    }

    /**
     * ����ͨ����ת��������һЩֵ �ɵ��ô˺���
     * @param listener
     */
    public void setOnFoldRotationListener(OnFoldRotationListener listener) {
        mFoldRotationListener = listener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
    }

    /**
     * ���봦��ǰ��תview����Ч����ʵ�ִ˺���
     * ��Ҫ��{@link #setAdapter(android.widget.BaseAdapter)}֮ǰ����
     * @param shading
     */
    public void setFoldShading(FoldShading shading) {
        mFoldShading = shading;
    }

    /**
     * ��������������ݵ�������
     * @param adapter
     */
    public void setAdapter(BaseAdapter adapter) {
        if (mAdapter != null) mAdapter.unregisterDataSetObserver(mDataObserver);
        mAdapter = adapter;
        if (mAdapter != null) mAdapter.registerDataSetObserver(mDataObserver);
        updateAdapterData();
    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    public int getCount() {
        return mAdapter == null ? 0 : mAdapter.getCount();
    }

    /**
     * ����adapter�����³�ʼ��view��ͨ���ڳ�ʼ��adapter��adapter�����ݱ仯��Ҫ��ʼ��ʱ �����
     */
    private void updateAdapterData() {
        int size = getCount();
        mMinRotation = 0;
        mMaxRotation = size == 0 ? 0 : 180 * (size - 1);

        freeAllLayouts(); // clearing old bindings

        //���¼��� ��draw
        setFoldRotation(mFoldRotation);
    }

    /**
     * ���ݲ���rotationֵ����view
     */
    public final void setFoldRotation(float rotation) {
        setFoldRotation(rotation, false);
    }

    /**
     * ���ݲ���rotationֵ����view
     * ��������Ǵ���ת�ĺ��ĺ���
     */
    protected void setFoldRotation(float rotation, boolean isFromUser) {
        if (isFromUser) mAnimator.cancel();//ȡ����ǰ�Ķ���

        //�߽��ж� ��֤rotationֵ����Ч��Χ֮��
        rotation = Math.min(Math.max(mMinRotation, rotation), mMaxRotation);
        
        //��¼ÿ�η�תʱ�ĽǶ�ֵ
        mFoldRotation = rotation;
        
        //��ȡ��ǰҳ������
        int firstVisiblePosition = (int) (rotation / 180);
        //��ȡ�ڵ�ǰҳ���Ѿ���ת�ĽǶ�
        float localRotation = rotation % 180;

        int size = getCount();
        //�ж��Ƿ񻹴��ڵ�ǰҳ
        boolean isHasFirst = firstVisiblePosition < size;
        //�ж��Ƿ񻹴�����һҳ
        boolean isHasSecond = firstVisiblePosition + 1 < size;

        //�����ڻ�ȡchild view
        FoldableItemLayout firstLayout = isHasFirst ? getLayoutForItem(firstVisiblePosition) : null;
        FoldableItemLayout secondLayout = isHasSecond ? getLayoutForItem(firstVisiblePosition + 1) : null;

        if (isHasFirst) {
        	//����ת����
            firstLayout.setFoldRotation(localRotation);
            onFoldRotationChanged(firstLayout, firstVisiblePosition);
        }

        if (isHasSecond) {
        	//����ת����
            secondLayout.setFoldRotation(localRotation - 180);
            onFoldRotationChanged(secondLayout, firstVisiblePosition + 1);
        }

        boolean isReversedOrder = localRotation <= 90;
        //����ж�����һ���ϸ��ӵļ��������ļ��ɵó��ģ����������ϷѾ�����ϸϸƷ��
        //����˼·�ǣ��ĸ�child view Ҫ����ת���Ͱ�˭��������
        if (isReversedOrder) {
            mFirstLayout = secondLayout;
            mSecondLayout = firstLayout;
        } else {
            mFirstLayout = firstLayout;
            mSecondLayout = secondLayout;
        }

        //��ת�����еĻص�
        if (mFoldRotationListener != null) mFoldRotationListener.onFoldRotation(rotation, isFromUser);

        invalidate(); // when hardware acceleration is enabled view may not be invalidated and redrawn, but we need it
    }

    /**
     * ��������⴦��ת��״̬
     * @param layout
     * @param position
     */
    protected void onFoldRotationChanged(FoldableItemLayout layout, int position) {

    }

    public float getFoldRotation() {
        return mFoldRotation;
    }

    /**
     * �õ�һ��child view
     * @param position
     * @return
     */
    private FoldableItemLayout getLayoutForItem(int position) {
        FoldableItemLayout layout = mFoldableLayoutsMap.get(position);
        //��ǰ�Ѵ��ڴ�child view ֱ�ӷ���
        if (layout != null) return layout;

        //�����ڻ����л�ȡ
        layout = mFoldableLayoutsCache.poll();

        //���Ը���mFoldableLayoutsMap������child view
        if (layout == null) {
            int farthestItem = position;

            int size = mFoldableLayoutsMap.size();
            for (int i = 0; i < size; i++) {
                int pos = mFoldableLayoutsMap.keyAt(i);
                if (Math.abs(position - pos) > Math.abs(position - farthestItem)) {
                    farthestItem = pos;
                }
            }

            if (Math.abs(farthestItem - position) > CACHED_LAYOUTS_OFFSET) {
                layout = mFoldableLayoutsMap.get(farthestItem);
                mFoldableLayoutsMap.remove(farthestItem);
                layout.getBaseLayout().removeAllViews(); // clearing old data
            }
        }

        //����û�ҵ���Чchild view ����һ��
        if (layout == null) {
            // if still no suited layout - create it
            layout = new FoldableItemLayout(getContext());
            layout.setFoldShading(mFoldShading);
            addView(layout, PARAMS);
        }

        //������� ������Դ
        View view = mAdapter.getView(position, null, layout.getBaseLayout()); // TODO: use recycler
        layout.getBaseLayout().addView(view, PARAMS);

        //���뼯���й���
        mFoldableLayoutsMap.put(position, layout);

        return layout;
    }

    /**
     * �ͷ�����child view
     */
    private void freeAllLayouts() {
        int size = mFoldableLayoutsMap.size();
        for (int i = 0; i < size; i++) {
            FoldableItemLayout layout = mFoldableLayoutsMap.valueAt(i);
            layout.getBaseLayout().removeAllViews();
            //���Ƴ���child view�ŵ�������
            mFoldableLayoutsCache.offer(layout);
        }
        mFoldableLayoutsMap.clear();
    }

    /**
     * ��������һҳ����һҳ
     * @param index
     */
    public void scrollToPosition(int index) {
        index = Math.max(0, Math.min(index, getCount() - 1));

        //��ȡĿ��λ�õĽǶ�
        float rotation = index * 180f;
        //��ǰ����λ�õĽǶ�
        float current = getFoldRotation();
        //����ʣ����Ҫ��ת�ĽǶȼ��㶯���ĳ���ʱ��
        long duration = (long) Math.abs(ANIMATION_DURATION_PER_ITEM * (rotation - current) / 180f);

        //���ö������� ����ʼ����
        mAnimator.cancel();
        mAnimator.setFloatValues(current, rotation);
        mAnimator.setDuration(duration).start();
    }

    /**
     * ���ݵ�ǰλ�õĽǶ� �жϹ���ͣ���ڵ�ǰҳ���ǹ�������һҳ
     */
    protected void scrollToNearestPosition() {
        float current = getFoldRotation();
        //���跭ת�Ƕ�rotation%180���ֵ�ֳɶ�������(0,90)(90,180)����(0,90)ʱ������һҳ����(90,180)ʱ������һҳ
        //ͨ��rotation = (rotation+90) rotationֵ������(90,180)(180,270)����ôrotation/180��ʱ���������ֵһ�����䡢һ��ֵ+1
        //�Դ�����(rotation+90)/180��ֵҪô���� rotation/180 Ҫô����(rotation/180+1)
        scrollToPosition((int) ((current + 90f) / 180f));
    }

    /**
     * �����¼�����
     * @param event
     * @return
     */
    private boolean processTouch(MotionEvent event) {
    	//�����ж�that event�Ƿ��ѱ�ִ���ˣ��������������ԭ����onInterceptTouchEvent��onTouchEvent���п���ȥ����that event
        long eventTime = event.getEventTime();
        if (mLastEventTime == eventTime) return mLastEventResult;
        mLastEventTime = eventTime;

        //up�¼�����ʱ����Ҫ�����ڷ�ת��view����λ����,��ͣ���ڵ�ǰҳ���ǹ�������һҳ
        if (event.getActionMasked() == MotionEvent.ACTION_UP && mIsScrollDetected) {
            mIsScrollDetected = false;
            scrollToNearestPosition();
        }

        if (getCount() > 0) {
            MotionEvent eventCopy = MotionEvent.obtain(event);
            //������view Y���Ϸ���λ��ʱ����Ҫ����Yֵ
            eventCopy.offsetLocation(0, getTranslationY());
            mLastEventResult = mGestureDetector.onTouchEvent(eventCopy);
            eventCopy.recycle();
        } else {
            mLastEventResult = false;
        }

        return mLastEventResult;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        // NO-OP
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        // NO-OP
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        return super.onRequestSendAccessibilityEvent(child, event);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //�������������Ҫ����e1��ȥe2.��ô�ɵó�һ�����ۣ����ϻ���distance������ ���»���distance�Ǹ���
    	float distance = e1.getY() - e2.getY();
        //�������� ��ʼ��ʼ��һЩ����
        if (!mIsScrollDetected && Math.abs(distance) > mMinDistanceBeforeScroll) {
            mIsScrollDetected = true;
            mScrollStartRotation = getFoldRotation();
            mScrollStartDistance = distance;
        }

        if (mIsScrollDetected) {
            float rotation = (2 * (distance - mScrollStartDistance) / getHeight()) * 180f;
            //��ʼ����ת����
            setFoldRotation(mScrollStartRotation + rotation, true);
        }

        return mIsScrollDetected;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float rotation = getFoldRotation();
        if (rotation % 180 == 0) return false;

        int position = (int) (rotation / 180f);
        //Ҳ������ɻ�Ϊʲô��position������position-1�أ��������Ļ����������������ڹ��������У�rotation���ֵ�����ɲ���
        //��������������ǺܷѾ���
        scrollToPosition(velocityY > 0 ? position : position + 1);
        return true;
    }

    //adapter�õļ�������
    private DataSetObserver mDataObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            updateAdapterData();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            updateAdapterData();
        }
    };

    public interface OnFoldRotationListener {
        void onFoldRotation(float rotation, boolean isFromUser);
    }

}
