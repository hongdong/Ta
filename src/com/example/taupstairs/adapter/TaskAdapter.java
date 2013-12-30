package com.example.taupstairs.adapter;

import java.util.Calendar;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.taupstairs.R;
import com.example.taupstairs.bean.Person;
import com.example.taupstairs.bean.Status;
import com.example.taupstairs.bean.Time;
import com.example.taupstairs.imageCache.SimpleImageLoader;
import com.example.taupstairs.ui.PersonDataActivity;
import com.example.taupstairs.util.HttpClientUtil;

public class TaskAdapter extends BaseAdapter {

	private Context context;
	private List<Status> listStatus;
	private Time now;

	public TaskAdapter(Context context, List<Status> listStatus) {
		super();
		this.context = context;
		this.listStatus = listStatus;
		this.now = getNow();
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return listStatus.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return listStatus.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		view = LayoutInflater.from(context).inflate(R.layout.task_item, null);	
		Status status = listStatus.get(position);
		Holder holder = new Holder();
		
		holder.img_fm_task_photo = (ImageView) view.findViewById(R.id.img_fm_task_photo);
		holder.img_fm_task_photo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, PersonDataActivity.class);
				context.startActivity(intent);
			}
		});
		
		holder.txt_fm_task_nickname = (TextView) view.findViewById(R.id.txt_fm_task_nickname);
		holder.txt_fm_task_releasetime = (TextView) view.findViewById(R.id.txt_fm_task_releasetime);
		holder.txt_fm_task_title = (TextView) view.findViewById(R.id.txt_fm_task_title);
		holder.txt_fm_task_rewards = (TextView) view.findViewById(R.id.txt_fm_task_rewards);
		holder.txt_fm_task_department = (TextView) view.findViewById(R.id.txt_fm_task_department);
		holder.txt_fm_task_grade = (TextView) view.findViewById(R.id.txt_fm_task_grade);
		holder.txt_fm_task_message = (TextView) view.findViewById(R.id.txt_fm_task_message);
		holder.txt_fm_task_signup = (TextView) view.findViewById(R.id.txt_fm_task_signup);
		
		String personSex = status.getPersonSex().trim();
		
		/*头像和昵称可能是空的。空的时候还要分男女，用上默认的*/
		if (status.getPersonPhotoUrl() != null) {
			SimpleImageLoader.showImage(holder.img_fm_task_photo, 
					HttpClientUtil.PHOTO_BASE_URL + status.getPersonPhotoUrl());
		} else {
			if (personSex.equals(Person.MALE)) {
				
			} else if (personSex.equals(Person.FEMALE)) {
				
			}
		}
		
		if (status.getPersonNickname() != null) {
			holder.txt_fm_task_nickname.setText(status.getPersonNickname());
		} else {
			if (personSex.equals(Person.MALE)) {
				holder.txt_fm_task_nickname.setText(Person.MALE_NICKNAME);
			} else if (personSex.equals(Person.FEMALE)) {
				holder.txt_fm_task_nickname.setText(Person.FEMALE_NICKNAME);
			}
		}
		
		char[] time = status.getStatusReleaseTime().toCharArray();
		int year = Integer.parseInt(String.copyValueOf(time, 0, 4));
		int month = Integer.parseInt(String.copyValueOf(time, 5, 2));
		int day = Integer.parseInt(String.copyValueOf(time, 8, 2));
		int hour = Integer.parseInt(String.copyValueOf(time, 11, 2));
		int minute = Integer.parseInt(String.copyValueOf(time, 14, 2));
		Time releaseTime = new Time(year, month, day, hour, minute);
		String displayTime = getDisplayTime(releaseTime);
		holder.txt_fm_task_releasetime.setText(displayTime);
		
		holder.txt_fm_task_title.setText(status.getStatusTitle());
		holder.txt_fm_task_rewards.setText(status.getStatusRewards());
		holder.txt_fm_task_department.setText(status.getPersonDepartment());
		holder.txt_fm_task_grade.setText(status.getPersonGrade());
		holder.txt_fm_task_message.setText(status.getStatusMessageCount());
		holder.txt_fm_task_signup.setText(status.getStatusSignUpCount());
		return view;
	}
	
	/*昵称，发布时间，标题，报酬，来自哪个院系、年级，留言数，报名数*/
	private class Holder {
		ImageView img_fm_task_photo;
		TextView txt_fm_task_nickname;
		TextView txt_fm_task_releasetime;
		TextView txt_fm_task_title;
		TextView txt_fm_task_rewards;
		TextView txt_fm_task_department;
		TextView txt_fm_task_grade;
		TextView txt_fm_task_message;
		TextView txt_fm_task_signup;
	}
	
	/*
	 * 获取当前时间的：天，时，分
	 */
	private Time getNow() {
		Time time = null;
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		time = new Time(year, month, day, hour, minute);
		
		return time;
	}
	
	/*
	 * 返回任务的发布时间。判断非常多
	 */
	private String getDisplayTime(Time releaseTime) {
		String displayTime = null;
		if (releaseTime.getYear() != now.getYear()) {
			displayTime = releaseTime.getYear() + "年" + 
						releaseTime.getMonth() + "月" +
						releaseTime.getDay() + "日    " +
						releaseTime.getHour() + ":" +
						releaseTime.getMinute();
		} else {
			if (releaseTime.getMonth() != now.getMonth()) {
				displayTime = releaseTime.getMonth() + "月" +
						releaseTime.getDay() + "日    " +
						releaseTime.getHour() + ":" +
						releaseTime.getMinute();
			} else {
				if (releaseTime.getDay() != now.getDay()) {
					displayTime = releaseTime.getDay() + "日    " +
							releaseTime.getHour() + ":" +
							releaseTime.getMinute();
				} else {
					displayTime = releaseTime.getHour() + ":" + releaseTime.getMinute();
				}
			}
		}
		return displayTime;
	}

}
