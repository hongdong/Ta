package com.example.taupstairs.ui.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.example.taupstairs.R;
import com.example.taupstairs.adapter.HomePageFragmentPagerAdapter;
import com.example.taupstairs.bean.Task;
import com.example.taupstairs.bean.User;
import com.example.taupstairs.logic.ItaActivity;
import com.example.taupstairs.logic.ItaFragment;
import com.example.taupstairs.logic.MainService;
import com.example.taupstairs.string.HomePageString;
import com.example.taupstairs.string.IntentString;
import com.example.taupstairs.ui.fragment.InfoFragment;
import com.example.taupstairs.ui.fragment.MeFragment;
import com.example.taupstairs.ui.fragment.RankFragment;
import com.example.taupstairs.ui.fragment.TaskFragment;
import com.example.taupstairs.util.SharedPreferencesUtil;

public class HomePageActivity extends FragmentActivity implements ItaActivity {

	private User defaultUser;
	private RadioGroup radioGroup;
	private Button btn_top_right;
	private RadioButton currentButton;
	private int[] buttonIds = {R.id.btn_info, R.id.btn_task, R.id.btn_rank};
	
	private boolean noNet = true;
	private boolean goCheck = false;
	private boolean displayNoNet = false;
	private ChangeUserReceiver receiver;
	
	private ViewPager viewPager;
	private List<Fragment> fragments;
	private InfoFragment infoFragment;
	private TaskFragment taskFragment;
	private RankFragment rankFragment;
	private MeFragment meFragment;
	private int currentIndex;
	private static final int INDEX_INFO = 0;
	private static final int INDEX_TASK = 1;
	private static final int INDEX_RANK = 2;
	private static final int INDEX_ME = 3;
	private boolean flag_clear;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.homepage);
		MainService.addActivity(HomePageActivity.this);
		init();
	}
	
	@Override
	public void init() {
		initData();
		initView();
		initCheckNetTask();
		initReceiver();
		initSetListener();
	}
	
	/*初始化全局变量*/
	private void initData() {
		defaultUser = SharedPreferencesUtil.getDefaultUser(HomePageActivity.this);
		currentIndex = 0;
		flag_clear = false;
	}
	
	/*初始化UI*/
	private void initView() {
		radioGroup = (RadioGroup)findViewById(R.id.rg_homepage);
		btn_top_right = (Button)findViewById(R.id.btn_me_write);
		btn_top_right.setBackgroundResource(R.drawable.hp_bg_btn_me);
		infoFragment = new InfoFragment();
		taskFragment = new TaskFragment(HomePageActivity.this);
		rankFragment = new RankFragment();
		meFragment = new MeFragment(HomePageActivity.this);
		fragments = new ArrayList<Fragment>();
		fragments.add(infoFragment);
		fragments.add(taskFragment);
		fragments.add(rankFragment);
		fragments.add(meFragment);
		viewPager = (ViewPager) findViewById(R.id.hp_viewpager);
		HomePageFragmentPagerAdapter adapter = new HomePageFragmentPagerAdapter(getSupportFragmentManager(), fragments);
		viewPager.setAdapter(adapter);
	}
	
	/*进入软件时检测网络*/
	private void initCheckNetTask() {
		new Thread() {
			public void run() {
				while (noNet) {
					if (!goCheck) {
						goCheck = true;
						HashMap<String, Object> taskParams = new HashMap<String, Object>(1);
						taskParams.put(Task.TA_CHECKNET_TASKPARAMS, defaultUser);
						Task task = new Task(Task.TA_CHECKNET, taskParams);
						MainService.addTask(task);
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
		}.start();
	}
	
	/*初始化广播接收*/
	private void initReceiver() {
		receiver = new ChangeUserReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.example.taupstairs.CHANGE_USER");
		registerReceiver(receiver, filter);
	}
	
	/*初始化控件的监听器*/
	private void initSetListener() {
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {	
				if (flag_clear) {
					System.out.println("check: " + checkedId);
					flag_clear = false;
				} else {
					switch (checkedId) {
					case R.id.btn_info:			
						setCurrent(INDEX_INFO);
						break;
					case R.id.btn_task:
						setCurrent(INDEX_TASK);
						break;
					case R.id.btn_rank:
						setCurrent(INDEX_RANK);
						break;

					default:
						break;
					}
				}
			}
		});
		/*初始化右上角按键的监听器*/
		btn_top_right.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (INDEX_TASK == currentIndex) {
					Intent intent = new Intent(HomePageActivity.this, WriteActivity.class);
					startActivityForResult(intent, IntentString.RequestCode.HOMEPAGE_WRITE);	
				} else {		
					currentIndex = INDEX_ME;	
					viewPager.setCurrentItem(currentIndex);
				}
			}
		});
		
		viewPager.setOnPageChangeListener(new OnPageChangeListener() {
			public void onPageSelected(int arg0) {
				currentIndex = arg0;
				if (INDEX_ME == currentIndex) {
					flag_clear = true;
					/*clearCheck函数会促发两次onCheckedChanged，这让我郁闷了半天。
					 * 第一次的checkedId为clearCheck之前被选中按钮的id
					 * 第二次的checkedId为clearCheck之后的，没有选中的，就为-1
					 * 最后灵活的设置了一个标志，问题解决了*/
					radioGroup.clearCheck();
				} else {
					currentButton = (RadioButton)HomePageActivity.this.findViewById(buttonIds[currentIndex]);
					currentButton.setChecked(true);
				}
			}
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				
			}
			public void onPageScrollStateChanged(int arg0) {
				
			}
		});
	}
	
	/*设置当前状态*/
	private void setCurrent(int index) {
		viewPager.setCurrentItem(index);
		if (INDEX_TASK == index) {
			btn_top_right.setBackgroundResource(R.drawable.hp_bg_btn_write);
		} else {
			btn_top_right.setBackgroundResource(R.drawable.hp_bg_btn_me);
		}	
	}

	@Override
	public void refresh(Object... params) {
		int taskId = (Integer) params[0];
		switch (taskId) {
		case Task.TA_CHECKNET:
			String ok = (String) params[1];
			if (ok.equals(Task.TA_NO)) {
				if (!displayNoNet) {
					displayNoNet = true;
					Toast.makeText(this, "没网络啊！！！亲", Toast.LENGTH_LONG).show();
				}
			} else {
				noNet = false;
			}
			goCheck = false;
			break;
			
		case Task.TA_USEREXIT:
			for (int i = 0; i < fragments.size(); i++) {
				ItaFragment fragment = (ItaFragment) fragments.get(i);
				fragment.exit();
			}
			System.exit(0);
			break;

		default:
			break;
		}
	}	
	
	/*
	 * 本地回调
	 */
	public void localRefresh(int id, Map<String, Object> params) {
		switch (id) {
		case HomePageString.UPDATA_PHOTO:
			taskFragment.localRefresh(id, params);
			break;
		case HomePageString.UPDATA_NICKNAME:
			taskFragment.localRefresh(id, params);
			break;

		default:
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case IntentString.RequestCode.HOMEPAGE_WRITE:
			if (IntentString.ResultCode.WRITE_HOMEPAGE == resultCode) {
				taskFragment.releaseTaskSuccess();
			}
			break;

		default:
			break;
		}
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		HashMap<String, Object> taskParams = new HashMap<String, Object>(1);
		taskParams.put(Task.TA_USEREXIT_TASKPARAMS, Task.TA_USEREXIT_ACTIVITY_HOMEPAGE);
		Task task = new Task(Task.TA_USEREXIT, taskParams);
		MainService.addTask(task);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}
	
	public class ChangeUserReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			HomePageActivity.this.finish();
		}
	}

}
