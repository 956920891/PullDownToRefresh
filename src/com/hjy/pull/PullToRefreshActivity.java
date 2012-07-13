package com.hjy.pull;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.hjy.pull.PullToRefreshView.OnRefreshListener;

public class PullToRefreshActivity extends Activity {
	
	private PullToRefreshView mPullView;
	
	private ListView mListView;
	
	private SimpleDateFormat mSdf;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        mPullView = (PullToRefreshView) findViewById(R.id.PullView);
        
        mListView = ((ListView)findViewById(R.id.ListView));
        String[] strArr = new String[30];
        for(int i=0; i<strArr.length; i++) {
        	strArr[i] = i + "" + i +i + i +i;
        }
        mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, 
        		strArr));
        mPullView.setRefreshTime("刷新于：2012-02-04");
        mSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        mPullView.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onStartRefresh() {
				System.out.println("start to refresh...");
				loadingData();
			}
		});
        
        loadingData();
    }
    
    
    private void loadingData() {
    	new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					Thread.sleep(8000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return null;
			}
			protected void onPostExecute(Void result) {
				System.out.println("refresh end...");
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(System.currentTimeMillis());
				mPullView.onRefreshComplete("更新于:" + mSdf.format(cal.getTime()));
			};
		}.execute((Void)null);
    }
}