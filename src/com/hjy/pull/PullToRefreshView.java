package com.hjy.pull;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;

import com.hjy.pull.PullDownListView.OnPullDownScrollListener;

/**
 * 下拉刷新
 * 主要思路是，在一个LinearLayout里，添加2个View，第一个是Header View，第二个是Content View，
 * 一般情况下Content View会是ListView或者ScrollView之类的，利用Scroller来进行下拉刷新。
 * 
 * @author Jinyun Hou
 * 
 */
public class PullToRefreshView extends LinearLayout {

	private static final String TAG = "PullToRefreshView";
	
	public static interface OnRefreshListener {
		public void onStartRefresh();
	}
	
	private static final int SCROLL_DURATION = 300;
	
	public static final int STATE_INVALID = -1;
	public static final int STATE_LOADING = 0; // 加载中
	public static final int STATE_PULLDOWN_TO_REFRESH = 1; // 下拉可以刷新
	public static final int STATE_RELEASE_TO_REFRESH = 2; // 松开可以刷新
	// 记录状态
	private int mState = STATE_INVALID;

	// 记住上次触摸屏的位置
	private float mLastMotionY = 0;
	// 下拉超过这个距离，放开则进行刷新
	private int mHeaderHeight;

	private ImageView mImgArr;
	private ProgressBar mProgressBar;
	private TextView mTextViewLabel;
	private TextView mTextViewTime;
	private RotateAnimation mFlipAnimation;
	private RotateAnimation mReverseFlipAnimation;

	private String mStrLoading = "加载中";
	private String mStrPullDownToRefresh = "下拉进行刷新";
	private String mStrReleaseToRefresh = "松开可以刷新";

	private Scroller mScroller;
	private OnRefreshListener mOnRefreshListener;

	//标记，表示是否第一次调用onLayout()方法
	private boolean mFirstLayout = false;
	
	public PullToRefreshView(Context context) {
		super(context);
		init(context);
	}

	public PullToRefreshView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	/**
	 * 初始化一些默认参数
	 * 
	 * @param conteext
	 */
	private void init(Context context) {
		setOrientation(LinearLayout.VERTICAL);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.pull_to_refresh_header, null);
		ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		addView(view, params);

		mImgArr = (ImageView) findViewById(R.id.ImageView_Refresh_Icon);
		mProgressBar = (ProgressBar) findViewById(R.id.ProgressBar_Refresh);
		mTextViewLabel = (TextView) findViewById(R.id.TextView_Refresh_Label);
		mTextViewTime = (TextView) findViewById(R.id.TextView_Refresh_Time);

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
		mScroller = new Scroller(context);
		
		measureHeadView(view);
	}

	private View getHeadView() {
		View headView = getChildAt(0);
		if (headView == null)
			throw new IllegalArgumentException("You should set a head view.");
		return headView;
	}

	private View getContentView() {
		View contentView = getChildAt(1);
		if (contentView == null)
			throw new IllegalArgumentException("You should set a content view.");
		return contentView;
	}

	/**
	 * 主要是为了计算header的高度
	 * 
	 * @param child
	 */
	private void measureHeadView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if(p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		int childWidthSpec = getChildMeasureSpec(0, 0 + 0, p.width);
		int childHeightSpec = 0;
		if(p.height > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(p.height, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
		mHeaderHeight = child.getMeasuredHeight();
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		View view = getContentView();
		//如果是ListView的下拉刷新
		if(view instanceof PullDownListView) {
			((PullDownListView)view).setOnPullDownScrollListener(onPullDownScrollListener);
		}
	}
	
	@Override
	public void computeScroll() {
		super.computeScroll();
		if(mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		View headView = getHeadView();
		headView.layout(0, -mHeaderHeight, r - l, 0);
		View contentView = getContentView();
		contentView.layout(0, 0, r - l, b - t);
		if(!mFirstLayout) {
			changeState(STATE_LOADING);
			scrollTo(0, -mHeaderHeight);
			mFirstLayout = true;
		}
	}
	
	@Override 
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		float y = ev.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "onInterceptTouchEvent down");
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			Log.d(TAG, "onInterceptTouchEvent move");
			float yOffset = y - mLastMotionY;
			mLastMotionY = y;
			Log.e(TAG, "scaled touch " + ViewConfiguration.getTouchSlop());
			if (mState != STATE_LOADING && yOffset > ViewConfiguration.getTouchSlop() && canScroll())
				return true;
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			Log.d(TAG, "onInterceptTouchEvent up or cancel");
			break;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float y = event.getY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "onTouchEvent down");
			// 记住开始落下的屏幕点
			mLastMotionY = y;
			if(!mScroller.isFinished())
				mScroller.abortAnimation();
			break;
		case MotionEvent.ACTION_MOVE:
			Log.d(TAG, "onTouchEvent move");
			if(mState != STATE_LOADING)
				doScrollView(y);
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_UP:
			Log.d(TAG, "onTouchEvent up");
			doRelease();
			break;
		case MotionEvent.ACTION_CANCEL:
			Log.d(TAG, "onTouchEvent cancel");
			break;
		}
		return true;
	}

	/**
	 * 判断是否能够进行下拉刷新操作，而不是触发ListView或者ScrollView本身的滚动操作。
	 * 这里考虑了ListView跟ScrollView两种情况。
	 * 
	 * @return
	 */
	private boolean canScroll() {
		if (getChildCount() > 1) {
			View childView = getChildAt(1);
			if (childView instanceof ListView) {
				ListView list = (ListView) childView;
				if (list.getChildCount() == 0)
					return true;
				int padTop = list.getListPaddingTop();
				int top = list.getChildAt(0).getTop();
				if (list.getFirstVisiblePosition() == 0
						&& Math.abs(top - padTop) < 3)
					return true;
				else
					return false;
			} else if (childView instanceof ScrollView) {
				ScrollView scrollView = (ScrollView) childView;
				if (scrollView.getScrollY() == 0)
					return true;
				else
					return false;
			}
		}
		return false;
	}

	/**
	 * 进行下拉操作，如果正在刷新的过程中，则不作滑动操作
	 * 
	 * @param y
	 */
	private void doScrollView(float y) {
		if (getScrollY() <= 0) {
			int deltaY = (int) ((mLastMotionY - y) * 0.5f);
			if (getScrollY() + deltaY > 0)
				deltaY = -getScrollY();
			scrollBy(0, deltaY);
			if(mState >= STATE_PULLDOWN_TO_REFRESH) {
				// 下拉超过一定距离，则改变状态
				if (getScrollY() <= -mHeaderHeight) {
					changeState(STATE_RELEASE_TO_REFRESH);
				} else {
					changeState(STATE_PULLDOWN_TO_REFRESH);
				}				
			}
		}
	}

	/**
	 * 松开手后
	 * 
	 * @param y
	 */
	private void doRelease() {
		if(mState == STATE_RELEASE_TO_REFRESH) {
			changeState(STATE_LOADING);
			mScroller.startScroll(0, getScrollY(), 0, -mHeaderHeight - getScrollY(), SCROLL_DURATION);
			if(null != mOnRefreshListener)
				mOnRefreshListener.onStartRefresh();
		} else if(mState == STATE_PULLDOWN_TO_REFRESH){
			mScroller.startScroll(0, getScrollY(), 0, -getScrollY(), SCROLL_DURATION);
		} else if(mState == STATE_LOADING) {
			mScroller.startScroll(0, getScrollY(), 0, -mHeaderHeight - getScrollY(), SCROLL_DURATION);
		}
		postInvalidate();
	}
	
	/**
	 * 改变状态
	 * 
	 * @param state
	 */
	private void changeState(int state) {
		if(mState == state)
			return;
		switch (state) {
		case STATE_LOADING: {
			mImgArr.setVisibility(View.INVISIBLE);
			mImgArr.clearAnimation();
			mProgressBar.setVisibility(View.VISIBLE);
			mTextViewLabel.setText(mStrLoading);
			break;
		}
		case STATE_PULLDOWN_TO_REFRESH: {
			mImgArr.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.INVISIBLE);
			mTextViewLabel.setText(mStrPullDownToRefresh);
			if(mState == STATE_RELEASE_TO_REFRESH) {
				mImgArr.startAnimation(mFlipAnimation);
			} else {
				mImgArr.clearAnimation();
			}
			break;
		}
		case STATE_RELEASE_TO_REFRESH: {
			mImgArr.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.INVISIBLE);
			mTextViewLabel.setText(mStrReleaseToRefresh);
			if(mState == STATE_PULLDOWN_TO_REFRESH) {
				mImgArr.startAnimation(mReverseFlipAnimation);
			} else {
				mImgArr.clearAnimation();
			}
			break;
		}
		}
		mState = state;
	}
	
	public int getState() {
		return mState;
	}
	
	public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
		mOnRefreshListener = onRefreshListener;
	}
	
	/**
	 * 设置刷新时间
	 * 
	 * @param charSequence
	 */
	public void setRefreshTime(CharSequence charSequence) {
		mTextViewTime.setText(charSequence);
	}
	
	/**
	 * 刷新完成
	 * 
	 * @param charSequence 刷新完成时间
	 */
	public void onRefreshComplete(CharSequence charSequence) {
		setRefreshTime(charSequence);
		changeState(STATE_PULLDOWN_TO_REFRESH);
		mScroller.startScroll(0, getScrollY(), 0, -getScrollY(), SCROLL_DURATION);
	}
	
	/**
	 * 改变View的状态为正在加载中
	 * 
	 * @return false表示正在刷新，不需要再重新刷新
	 */
	public boolean startRefreshData() {
		if(mState == STATE_LOADING)
			return false;
		changeState(STATE_LOADING);
		scrollTo(0, -mHeaderHeight);
		invalidate();
		return true;
	}

	private OnPullDownScrollListener onPullDownScrollListener = new OnPullDownScrollListener() {
		
		@Override
		public void onListViewScroll(float dist) {
			Log.d("TAG", "dist ... " + dist);
			
			if(getScrollY() - (int)(dist/2) > 0)
				scrollTo(0, 0);
			else {
				scrollBy(0, (int) -dist/2);
				if(mState >= STATE_PULLDOWN_TO_REFRESH) {
					// 下拉超过一定距离，则改变状态
					if (getScrollY() <= -mHeaderHeight) {
						changeState(STATE_RELEASE_TO_REFRESH);
					} else {
						changeState(STATE_PULLDOWN_TO_REFRESH);
					}				
				}
			}
		}
		
		public void onListViewRelease() {
			doRelease();
		};
	};
	
}