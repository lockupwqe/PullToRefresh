package com.uibaike.neihan.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.uibaike.neihan.R;

/**
 * 自己修改的下拉刷新控件
 * @author lockup
 *
 */
public class PullToRefreshListView extends ListView implements OnScrollListener {

    private static final int PULL_TO_REFRESH = 1;
    private static final int RELEASE_TO_REFRESH = 2;
    private static final int REFRESHING = 3;

    private static final String TAG = "PullToRefreshListView";

    private OnRefreshListener mOnRefreshListener;
    private OnLoadMoreListener mOnLoadMoreListener;

    /**
     * Listener that will receive notifications every time the list scrolls.
     */
    private OnScrollListener mOnScrollListener;
    private LayoutInflater mInflater;

    private LinearLayout mRefreshView;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private ProgressBar mRefreshViewProgress;

    private int mRefreshState = PULL_TO_REFRESH;

    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    private int mRefreshViewHeight;
    private int mLastMotionY;
    
    private LinearLayout mRefreshFooter;
    private boolean mCanLoadMore = true;
    private boolean mIsLoadMore = false;

    public PullToRefreshListView(Context context) {
        super(context);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        // Load all of the animations we need in code rather than through XML
        mFlipAnimation = new RotateAnimation(0, -180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(-180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);

        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

		mRefreshView = (LinearLayout) mInflater.inflate(
				R.layout.pull_to_refresh_header, this, false);
        mRefreshViewText =
            (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        mRefreshViewImage =
            (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);
        mRefreshViewProgress =
            (ProgressBar) mRefreshView.findViewById(R.id.pull_to_refresh_progress);
        mRefreshFooter = (LinearLayout) mInflater.inflate(
        		R.layout.pull_to_refresh_footer, null);
        
        measureView(mRefreshView);
        mRefreshViewHeight = mRefreshView.getMeasuredHeight();
        mRefreshView.setPadding(0, -1*mRefreshViewHeight, 0, 0);  
        addHeaderView(mRefreshView);

        super.setOnScrollListener(this);      
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (!isVerticalScrollBarEnabled()) {
                    setVerticalScrollBarEnabled(true);
                }
                if (getFirstVisiblePosition() == 0 && mRefreshState != REFRESHING) {
                    if (mRefreshState == RELEASE_TO_REFRESH) {
                        prepareForRefresh();
                        onRefresh();
                    } else if (mRefreshState == PULL_TO_REFRESH){
                        resetHeader();
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
            	if (getFirstVisiblePosition() == 0 && mRefreshState != REFRESHING) {
            		applyHeaderPadding(event);
                    if (mRefreshView.getPaddingTop() > 0
                            && mRefreshState != RELEASE_TO_REFRESH) {
                        mRefreshViewText.setText(R.string.pull_to_refresh_release_label);
                        mRefreshViewImage.clearAnimation();
                        mRefreshViewImage.startAnimation(mFlipAnimation);
                        mRefreshState = RELEASE_TO_REFRESH;
                    } else if (mRefreshView.getPaddingTop() <= 0
                            && mRefreshState != PULL_TO_REFRESH) {
                        mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);                  
                        mRefreshViewImage.clearAnimation();
                        mRefreshViewImage.startAnimation(mReverseFlipAnimation);
                        mRefreshState = PULL_TO_REFRESH;
                    }
            	}
                break;
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
    	Log.e("firstVisibleItem", ""+firstVisibleItem);
    	Log.e("visibleItemCount", ""+visibleItemCount);
    	
        /*
         * 滚动加载更多
         * 四个条件：有数据/滚动到底部/可加载状态/没有正在加载
         */
        if (totalItemCount > 1 && (firstVisibleItem + visibleItemCount == totalItemCount)
        		&& mCanLoadMore && !mIsLoadMore) {
        	mIsLoadMore = true;
        	addFooterView(mRefreshFooter);
        	if (mOnLoadMoreListener != null) {
        		mOnLoadMoreListener.onLoadMore();
        	}      	
        }
        
        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);
        }
    }
    
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }
    
    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0,
                0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }
    
    private void applyHeaderPadding(MotionEvent ev) {      	   	
        int pointerCount = ev.getHistorySize();   
        int historicalY,topPadding;
        for (int p = 0; p < pointerCount; p++) {
            historicalY = (int) ev.getHistoricalY(p);
            topPadding = (int) ((historicalY - mLastMotionY)/3 - mRefreshViewHeight);
            mRefreshView.setPadding(
                    mRefreshView.getPaddingLeft(),
                    topPadding,
                    mRefreshView.getPaddingRight(),
                    mRefreshView.getPaddingBottom());
        }
     
        mRefreshView.setPadding(
                mRefreshView.getPaddingLeft(),
                ((int)ev.getY() - mLastMotionY)/3 - mRefreshViewHeight,
                mRefreshView.getPaddingRight(),
                mRefreshView.getPaddingBottom());
    }

    private void resetHeader() {
    	mRefreshView.setPadding(0, -1*mRefreshViewHeight, 0, 0);  
    	mRefreshViewImage.clearAnimation();
        mRefreshViewImage.setVisibility(View.VISIBLE);
        mRefreshViewProgress.setVisibility(View.GONE);  
        mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);   
        mRefreshState = PULL_TO_REFRESH;
    }

    public void prepareForRefresh() {
    	mRefreshView.setPadding(0, 0, 0, 0);
    	mRefreshViewImage.clearAnimation();
        mRefreshViewImage.setVisibility(View.GONE);
        mRefreshViewProgress.setVisibility(View.VISIBLE);
        mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);
        mRefreshState = REFRESHING;
    }

    public void onRefresh() {
        setEnabledLoadMore(false);
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }
    
    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }
    
    /**
     * 设置下拉刷新接口
     * @param onRefreshListener
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }
    
    /**
     * 设置滚动加载接口
     * @param listener
     */
    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
    	mOnLoadMoreListener = onLoadMoreListener;
    }
    
    /**
     * 下拉刷新结束调用
     */
    public void onRefreshComplete() {         
        setEnabledLoadMore(true);       
        resetHeader();
    }
    
    /**
     * 滚动加载结束调用
     */
    public void onLoadMoreComplete() {
    	mIsLoadMore = false;
    	removeFooterView(mRefreshFooter);
    }
    
    /**
     * 设置是否开启滚动加载
     * @param isLoadMore
     */
    public void setEnabledLoadMore(boolean isLoadMore) {
    	mCanLoadMore = isLoadMore;
    }
    
    /**
     * 下拉刷新监听接口
     *
     */
    public interface OnRefreshListener {
        public void onRefresh();
    }
    
    /**
     * 滚动加载更多监听接口
     */
    public interface OnLoadMoreListener {
    	public void onLoadMore();
    }
}
