package com.coolwifi.main;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import com.xiaohong.wificoolconnect.R;

public class IntroActivity extends Activity implements OnClickListener,
		OnPageChangeListener {

	private ViewPager viewPager;
	private ViewPagerAdapter vpAdapter;
	private ArrayList<View> views;
	private static final int[] pics = { R.drawable.guide1, R.drawable.guide2};
	private ImageView[] points;
	private int currentIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intro);

		ActionBar actionBar = getActionBar(); //得<span></span>到ActionBar
	    actionBar.hide(); //隐藏ActionBar
		
		initView();
		initData();
	}

	private void initView() {
		views = new ArrayList<View>();
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		vpAdapter = new ViewPagerAdapter(views);
	}

	private void initData() {
		LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);

		for (int i = 0; i < pics.length; i++) {
			ImageView iv = new ImageView(this);
			iv.setLayoutParams(mParams);
			iv.setScaleType(ScaleType.FIT_XY);
			iv.setImageResource(pics[i]);
			views.add(iv);
		}

		viewPager.setAdapter(vpAdapter);
		viewPager.setOnPageChangeListener(this);
//		initPoint();
	}

//	private void initPoint() {
//		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ll);
//
//		points = new ImageView[pics.length];
//
//		for (int i = 0; i < pics.length; i++) {
//			points[i] = (ImageView) linearLayout.getChildAt(i);
//			points[i].setEnabled(true);
//			points[i].setOnClickListener(this);
//			points[i].setTag(i);
//		}
//
//		currentIndex = 0;
//		points[currentIndex].setEnabled(false);
//	}

	@Override
	public void onPageScrollStateChanged(int arg0) {

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int arg0) {
		setCurDot(arg0);
	}

	@Override
	public void onClick(View v) {
		int position = (Integer) v.getTag();
		setCurView(position);
		setCurDot(position);
	}

	private void setCurView(int position) {
		if (position < 0 || position >= pics.length) {
			return;
		}
		viewPager.setCurrentItem(position);
	}

	private void setCurDot(int positon) {
//		if (positon < 0 || positon > pics.length - 1 || currentIndex == positon) {
//			return;
//		}
//		points[positon].setEnabled(false);
//		points[currentIndex].setEnabled(true);
//
//		currentIndex = positon;
	}
}
