package com.hjy.pull;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
/**
 * 
 * 下拉刷新的ListView
 * 
 * @author Jinyun Hou
 *
 */
public class PullDownListView extends ListView {

	public static interface OnPullDownScrollListener {
		public void onListViewScroll(float dist);
		public void onListViewRelease();
	}
	
	/**
	 * 记录的是每次touch事件时的rawY，即在屏幕上的坐标。
	 * 在ViewGroup进行scroll时，touch事件的y可能会有偏移，导致计算不准确
	 */
	private float mLastRawMotionY;
	
	private OnPullDownScrollListener mOnPullDownScrollListener;
	
	public void setOnPullDownScrollListener(OnPullDownScrollListener onPullDownScrollListener) {
		mOnPullDownScrollListener = onPullDownScrollListener;
	}
	
	public PullDownListView(Context context) {
		super(context);
	}
	
	public PullDownListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public PullDownListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		float y = ev.getRawY();
		if(ev.getAction() == MotionEvent.ACTION_MOVE) {
			if(getCount() > 0) {
				int firstPos = getFirstVisiblePosition();
				View firstView = getChildAt(0);
				/**
				 * 1.如果当ListView的getFirstVisiblePosition()为0时，且没有向上滑动过,
				 * 这个时候如果向下滑动，触发下拉刷新的操作
				 * 2.如果本来处于下拉刷新的操作，仍旧响应下拉刷新的操作
				 * 3.这2种情况下，都不触发ListView本身的滑动操作
				 */
				if((firstPos == 0 && Math.abs(firstView.getTop() - getListPaddingTop()) < 3 && y > mLastRawMotionY)
						|| ((ViewGroup)getParent()).getScrollY() < 0) {
					if(mOnPullDownScrollListener != null) {
						Log.e("TAG", "scroll......");
						mOnPullDownScrollListener.onListViewScroll(y - mLastRawMotionY);
						mLastRawMotionY = y;
						return true;
					}
				}
			}
		} else if(ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
			if(mOnPullDownScrollListener != null && ((ViewGroup)getParent()).getScrollY() != 0)
				mOnPullDownScrollListener.onListViewRelease();
		}
		mLastRawMotionY = y;
		return super.onTouchEvent(ev);
	}

}