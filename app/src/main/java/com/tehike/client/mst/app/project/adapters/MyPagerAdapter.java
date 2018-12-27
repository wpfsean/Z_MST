package com.tehike.client.mst.app.project.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

public class MyPagerAdapter extends FragmentPagerAdapter {

	private List<Fragment> list;

	public MyPagerAdapter(FragmentManager fm, List<Fragment> list) {
		super(fm);
		this.list = list;

	}
	@Override
	public android.support.v4.app.Fragment getItem(int arg0) {
		return list.get(arg0);
	}

	@Override
	public int getCount() {
		return list == null ? 0 : list.size();
	}

}
