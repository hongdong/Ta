package com.example.taupstairs.ui.fragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import com.example.taupstairs.R;
import com.example.taupstairs.adapter.InfoAdapter;
import com.example.taupstairs.app.TaUpstairsApplication;
import com.example.taupstairs.bean.Info;
import com.example.taupstairs.bean.Person;
import com.example.taupstairs.bean.Task;
import com.example.taupstairs.logic.ItaFragment;
import com.example.taupstairs.logic.MainService;
import com.example.taupstairs.services.InfoService;
import com.example.taupstairs.string.HomePageString;
import com.example.taupstairs.ui.activity.HomePageActivity;
import com.example.taupstairs.ui.activity.InfoEndTaskActivity;
import com.example.taupstairs.ui.activity.InfoExecTaskActivity;
import com.example.taupstairs.ui.activity.InfoMessageActivity;
import com.example.taupstairs.ui.activity.InfoSignUpActivity;
import com.example.taupstairs.util.SharedPreferencesUtil;
import com.example.taupstairs.util.TimeUtil;
import com.example.taupstairs.view.XListView;
import com.example.taupstairs.view.XListView.IXListViewListener;

public class InfoFragment extends Fragment implements ItaFragment {

	private View view;
	private HomePageActivity context;
	private XListView xlist_info;
	private InfoAdapter adapter;
	private List<Info> currentInfos;
	private int clickPosition;
	
	@SuppressWarnings("rawtypes")
	private Class[] infoDetail = {InfoMessageActivity.class, InfoExecTaskActivity.class, 
		InfoSignUpActivity.class, InfoEndTaskActivity.class, };
	
	private InfoService infoService;
	private String lastestInfoId;
	private String oldestInfoId;
	private String lastestUpdata;
	
	private boolean isRefresh;
	
	public InfoFragment() {
		super();
	}
	
	public InfoFragment(HomePageActivity context) {
		super();
		this.context = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainService.addFragment(InfoFragment.this);
		initData();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fm_info, container, false);
		initView();
		return view;
	}
	
	@Override
	public void initData() {
		isRefresh = false;
		lastestInfoId = SharedPreferencesUtil.getLastestId(context, SharedPreferencesUtil.LASTEST_INFOID);
		if (null == lastestInfoId) {
			doGetInfoTask(Task.TA_GETINFO_MODE_FIRSTTIME, null);
		} else {
			doGetInfoTask(Task.TA_GETINFO_MODE_PULLREFRESH, lastestInfoId);
		}
		infoService = new InfoService(context);
		currentInfos = infoService.getInfos();
	}

	@Override
	public void initView() {
		xlist_info = (XListView) view.findViewById(R.id.xlist_fm_info);
		xlist_info.setPullLoadEnable(false);
		if (currentInfos != null) {
			adapter = new InfoAdapter(context, currentInfos);
			xlist_info.setAdapter(adapter);
		}
		xlist_info.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (currentInfos.size() >= arg2) {
					clickPosition = arg2 - 1;
					int type = Integer.parseInt(currentInfos.get(clickPosition).getInfoType());
					TaUpstairsApplication app = (TaUpstairsApplication) getActivity().getApplication();
					app.setInfo(currentInfos.get(clickPosition));
					Intent intent = new Intent(context, infoDetail[type - 1]);
					startActivity(intent);
				}
			}
		});
		xlist_info.setXListViewListener(new IXListViewListener() {
			public void onRefresh() {
				if (null == lastestInfoId) {
					doGetInfoTask(Task.TA_GETINFO_MODE_FIRSTTIME, null);
				} else {
					doGetInfoTask(Task.TA_GETINFO_MODE_PULLREFRESH, lastestInfoId);
				}
			}
			
			public void onLoadMore() {
				doGetInfoTask(Task.TA_GETINFO_MODE_LOADMORE, oldestInfoId);
			}
		});
	}
	
	/*
	 * HomePage的本地回调
	 */
	public void localRefresh(int id, Map<String, Object> params) {
		switch (id) {
		case HomePageString.UPDATA_PHOTO:
			String personId_p = (String) params.get(Person.PERSON_ID);
			String personPhotoUrl = (String) params.get(Person.PERSON_PHOTOURL);
			for (Info info : currentInfos) {
				if (personId_p.equals(info.getPersonId())) {
					info.setPersonPhotoUrl(personPhotoUrl);
				}
			}
			adapter.notifyDataSetChanged();
			break;
		case HomePageString.UPDATA_NICKNAME:
			String personId_n = (String) params.get(Person.PERSON_ID);
			String personNickname = (String) params.get(Person.PERSON_NICKNAME);
			for (Info info : currentInfos) {
				if (personId_n.equals(info.getPersonId())) {
					info.setPersonNickname(personNickname);
				}
			}
			adapter.notifyDataSetChanged();
			break;

		default:
			break;
		}
	}
	
	private void doGetInfoTask(int mode, String infoId) {
		if (!isRefresh) {
			HashMap<String, Object> taskParams = new HashMap<String, Object>(2);
			taskParams.put(Task.TA_GETINFO_MODE, mode);
			taskParams.put(Task.TA_GETINFO_INFOID, infoId);
			Task task = new Task(Task.TA_GETINFO, taskParams);
			MainService.addTask(task);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void refresh(Object... params) {
		int taskId = (Integer) params[0];
		switch (taskId) {
		case Task.TA_GETINFO:
			int mode = (Integer) params[1];
			List<Info> newInfos = (List<Info>) params[2];
			refreshInfo(mode, newInfos);
			break;

		default:
			break;
		}
	}
	
	private void refreshInfo(int mode, List<Info> newInfos) {
		if (newInfos != null) {
			switch (mode) {
			case Task.TA_GETINFO_MODE_FIRSTTIME:
				currentInfos = newInfos;
				adapter = new InfoAdapter(context, currentInfos);
				xlist_info.setAdapter(adapter);
				lastestUpdata = TimeUtil.setLastestUpdata();
				break;
				
			case Task.TA_GETINFO_MODE_PULLREFRESH:
				if (newInfos.size() < 20) {
					if (currentInfos != null) {
						currentInfos.addAll(0, newInfos);
						adapter.notifyDataSetChanged();
					} else {	
						currentInfos = newInfos;
						adapter = new InfoAdapter(context, currentInfos);
						xlist_info.setAdapter(adapter);
					}
				} else {
					currentInfos = newInfos;
					adapter.notifyDataSetInvalidated();
				}
				lastestUpdata = TimeUtil.setLastestUpdata();
				break;
				
			case Task.TA_GETINFO_MODE_LOADMORE:
				currentInfos.addAll(newInfos);
				adapter.notifyDataSetChanged();
				break;

			default:
				break;
			}
			
			changeListData();
		} else {
			xlist_info.stopRefresh();
		}
	}
	
	private void changeListData() {
		xlist_info.stopRefresh();
		xlist_info.stopLoadMore();
		xlist_info.setRefreshTime(lastestUpdata);
		if (currentInfos.size() > 0) {
			lastestInfoId = currentInfos.get(0).getInfoId();
			oldestInfoId = currentInfos.get(currentInfos.size() - 1).getInfoId();
			xlist_info.setPullLoadEnable(true);
		}
	}

	@Override
	public void exit() {
		if (infoService != null && lastestInfoId != null) {
//			SharedPreferencesUtil.savaLastestId(context, SharedPreferencesUtil.LASTEST_STATUSID, lastestInfoId);
//			infoService.emptyInfoDb();
//			infoService.insertInfos(currentInfos);
//			infoService.closeDBHelper();
		}
	}

}
