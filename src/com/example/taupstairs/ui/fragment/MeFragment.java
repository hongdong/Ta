package com.example.taupstairs.ui.fragment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.taupstairs.R;
import com.example.taupstairs.adapter.PersonVariableDataAdapter;
import com.example.taupstairs.bean.Person;
import com.example.taupstairs.bean.Task;
import com.example.taupstairs.logic.ItaFragment;
import com.example.taupstairs.logic.MainService;
import com.example.taupstairs.services.PersonService;
import com.example.taupstairs.string.HomePageString;
import com.example.taupstairs.string.IntentString;
import com.example.taupstairs.string.JsonString;
import com.example.taupstairs.ui.activity.HomePageActivity;
import com.example.taupstairs.ui.activity.SettingActivity;
import com.example.taupstairs.ui.activity.UpdataUserdataBaseActivity;
import com.example.taupstairs.util.SdCardUtil;
import com.example.taupstairs.util.SharedPreferencesUtil;
import com.example.taupstairs.util.UploadToBCS;

public class MeFragment extends Fragment implements ItaFragment {

	private HomePageActivity context;
	private String defaultPersonId;
	private Person defaultPerson;
	private PersonService personService;
	private View view;
	private ListView list_variable, list_base;
	private PersonVariableDataAdapter variable_adapter;
	private TextView txt_setting;
	private static final String LIST_LEFT = "left";
	private static final String LIST_RIGHT = "right";
	
	private Bitmap userPhoto;
	private String nickname, signature;
	private static final String IMAGE_FILE_NAME = "userPhoto.jpg";
	private String fileName;
	private String[] items = new String[] { "选择本地图片", "拍照" };
	private static final int MSG_WHAT_OK = 0x123;
	private static final int MSG_WHAT_NO = 0x456;
	private ProgressDialog progressDialog;
	
	/*若Fragement定义有带参构造函数，则一定要定义public的默认的构造函数，
	 * 否则有可能报出android.app.Fragment$InstantiationException的运行时异常*/
	public MeFragment() {
		super();
	}

	/*传入上下文的构造方法
	 * 因为如果用getActivity()的话，有时候会为null，就会出现空指针异常*/
	public MeFragment(HomePageActivity context) {
		super();
		this.context = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainService.addFragment(MeFragment.this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fm_me, container, false);
		init();
		return view;
	}

	@Override
	public void init() {
		initData();
		initView();
	}
	
	/*初始化一些全局变量*/
	private void initData() {
		defaultPersonId = SharedPreferencesUtil.getDefaultUser(context).getUserId();
		personService = new PersonService(context);
		defaultPerson = personService.getPersonById(defaultPersonId);
	}
	
	/*初始化View组件，list列表显示，设置监听器*/
	private void initView() {
		list_variable = (ListView)view.findViewById(R.id.list_fm_me_variable);
		list_base = (ListView)view.findViewById(R.id.list_fm_me_base);
		txt_setting = (TextView)view.findViewById(R.id.txt_fm_me_setting);
		progressDialog = new ProgressDialog(context);
		
		if (defaultPerson != null) {		//在initData里面已经从数据库中读数据了
			displayPerson(defaultPerson);	//如果数据库中有数据，就直接显示出来
		} else {
			getUserData();					//没有的话，就从服务器获取
		}
		list_variable.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				switch (arg2) {				//arg2为组件的位置，这个是系统定的，从0开始
				case 0: 
					showDialog();
					break;		
					
				case 1:
					Intent intent_nickname = new Intent(context, UpdataUserdataBaseActivity.class);
					intent_nickname.putExtra(IntentString.Extra.TYPE, Person.PERSON_NICKNAME);
					intent_nickname.putExtra(IntentString.Extra.CONTENT, defaultPerson.getPersonNickname());
					startActivityForResult(intent_nickname, IntentString.RequestCode.MEFRAGMENT_UPDATAUSERDATABASE);
					break;	
					
				case 2:
					Intent intent_signature = new Intent(context, UpdataUserdataBaseActivity.class);
					intent_signature.putExtra(IntentString.Extra.TYPE, Person.PERSON_SIGNATURE);
					intent_signature.putExtra(IntentString.Extra.CONTENT, defaultPerson.getPersonSignature());
					startActivityForResult(intent_signature, IntentString.RequestCode.MEFRAGMENT_UPDATAUSERDATABASE);
					break;
					
				default:
					break;
				}
			}
		});
		txt_setting.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {		
				Intent intent = new Intent(context, SettingActivity.class);
				startActivity(intent);	
			}
		});
	}
	
	/*从服务器获取Person信息*/
	private void getUserData() {
		HashMap<String, Object> taskParams = new HashMap<String, Object>(2);
		taskParams.put(Task.TA_GETUSERDATA_ACTIVITY, Task.TA_GETUSERDATA_ACTIVITY_ME);
		taskParams.put(Task.TA_GETUSERDATA_TASKPARAMS, defaultPersonId);
		Task task = new Task(Task.TA_GETUSERDATA, taskParams);
		MainService.addTask(task);
	}
	
	/*
	 * 显示选择对话框
	 */
	private void showDialog() {
		new AlertDialog.Builder(context)
				.setTitle("设置头像")
				.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							Intent intentFromGallery = new Intent();
							intentFromGallery.setType("image/*"); // 设置文件类型
							intentFromGallery.setAction(Intent.ACTION_GET_CONTENT);
							startActivityForResult(intentFromGallery, IntentString.RequestCode.IMAGE_REQUEST_CODE);
							break;
						case 1:
							Intent intentFromCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
							// 判断存储卡是否可以用，可用进行存储
							if (SdCardUtil.hasSdcard()) {
								intentFromCapture.putExtra(MediaStore.EXTRA_OUTPUT,
										Uri.fromFile(new File(Environment.getExternalStorageDirectory(), 
												IMAGE_FILE_NAME)));
							}
							startActivityForResult(intentFromCapture, IntentString.RequestCode.CAMERA_REQUEST_CODE);
							break;
						}
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != Activity.RESULT_CANCELED) {
			switch (requestCode) {
			case IntentString.RequestCode.IMAGE_REQUEST_CODE:
				startPhotoZoom(data.getData());
				break;
			case IntentString.RequestCode.CAMERA_REQUEST_CODE:
				if (SdCardUtil.hasSdcard()) {
					File tempFile = new File(
							Environment.getExternalStorageDirectory() + "/" + IMAGE_FILE_NAME);
					startPhotoZoom(Uri.fromFile(tempFile));
				} else {
					Toast.makeText(context, "未找到存储卡，无法存储照片！",
							Toast.LENGTH_LONG).show();
				}
				break;
			case IntentString.RequestCode.PHOTO_REQUEST_CODE:
				if (data != null) {
					Bundle extras = data.getExtras();
					if (extras != null) {
						userPhoto = extras.getParcelable("data");
					}
					updataUserPhoto();	
				}
				break;
			case IntentString.RequestCode.MEFRAGMENT_UPDATAUSERDATABASE:
				if (IntentString.ResultCode.UPDATAUSERDATABASE_MEFRAGMENT_NICKNAME == resultCode) {
					nickname = data.getStringExtra(Person.PERSON_NICKNAME);
					defaultPerson.setPersonNickname(nickname);
					displayPersonVariable(defaultPerson);
					Toast.makeText(context, "更新昵称成功", Toast.LENGTH_SHORT).show();
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(Person.PERSON_ID, defaultPersonId);
					map.put(Person.PERSON_NICKNAME, nickname);
					context.localRefresh(HomePageString.UPDATA_NICKNAME, map);
					personService.updataPersonInfo(defaultPersonId, Person.PERSON_NICKNAME, nickname);
				} else if (IntentString.ResultCode.UPDATAUSERDATABASE_MEFRAGMENT_SIGNATURE == resultCode) {
					signature = data.getStringExtra(Person.PERSON_SIGNATURE);
					defaultPerson.setPersonSignature(signature);
					displayPersonVariable(defaultPerson);
					Toast.makeText(context, "更新个性签名成功", Toast.LENGTH_SHORT).show();
					personService.updataPersonInfo(defaultPersonId, Person.PERSON_SIGNATURE, signature);
				}
				break;
			}		
		}
	}
	
	/*
	 * 调用系统的裁剪图片的Intent
	 */
	public void startPhotoZoom(Uri uri) {

		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		// 设置裁剪
		intent.putExtra("crop", "true");
		// aspectX aspectY 是宽高的比例
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		// outputX outputY 是裁剪图片宽高
		intent.putExtra("outputX", 320);
		intent.putExtra("outputY", 320);
		intent.putExtra("return-data", true);
		startActivityForResult(intent, IntentString.RequestCode.PHOTO_REQUEST_CODE);
	}
	
	/*
	 * 头像上传完后通知主线程
	 */
	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (MSG_WHAT_OK == msg.what) {
				String photoUrl = (String) msg.obj;
				String url = "users_id=" + defaultPersonId + "&photo=" + photoUrl;
				doUpdataUserDataTask(url);
			} else if (MSG_WHAT_NO == msg.what) {
				progressDialog.dismiss();
				Toast.makeText(context, "网络竟然出错了", Toast.LENGTH_SHORT).show();
			}
		};
	};
	
	private void progressShow() {
		progressDialog.setCancelable(false);
		progressDialog.setMessage("    稍等片刻...");
		progressDialog.show();
	}
	
	/*
	 * 上传照片
	 */
	private void updataUserPhoto() {
		progressShow();
		try {	/*先把图片写到file里面，再读出来以流的方式上传*/
			final File file = context.getFilesDir();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			userPhoto.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			byte[] byteArray = stream.toByteArray();
			fileName = System.currentTimeMillis() + ".jpeg";
			File imageFile = new File(file, fileName);
			FileOutputStream fstream = new FileOutputStream(imageFile);
			BufferedOutputStream bStream = new BufferedOutputStream(fstream);
			bStream.write(byteArray);
			if (bStream != null) {
				bStream.close();
			}

			new Thread(new Runnable() {
				public void run() {
					UploadToBCS ub = new UploadToBCS();
					Message msg = new Message();
					try {
						File f = new File(file.getAbsolutePath() + "/" + fileName);
						ub.putObjectByInputStream(f, "/" + fileName);
						msg.what = MSG_WHAT_OK;
						msg.obj = fileName;
					} catch (Exception e) {
						e.printStackTrace();
						msg.what = MSG_WHAT_NO;
					}							
					handler.sendMessage(msg);
				}
			}).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 网络任务
	 */
	private void doUpdataUserDataTask(String url) {
		HashMap<String, Object> taskParams = new HashMap<String, Object>(2);
		taskParams.put(Task.TA_UPDATAUSERDATA_ACTIVITY, Task.TA_UPDATAUSERDATA_FRAGMENT_ME);
		taskParams.put(Task.TA_UPDATAUSERDATA_URL, url);
		Task task = new Task(Task.TA_UPDATAUSERDATA, taskParams);
		MainService.addTask(task);
	}

	@Override
	public void refresh(Object... params) {
		int taskId = (Integer) params[0];
		switch (taskId) {
		case Task.TA_GETUSERDATA:
			defaultPerson = (Person) params[1];
			if (defaultPerson != null) {
				displayPerson(defaultPerson);		
				personService.insertPerson(defaultPerson);	//更新数据库中的默认Person
			} else {
				Toast.makeText(context, "没网络啊！！！亲", Toast.LENGTH_LONG).show();
			}
			break;
			
		case Task.TA_UPDATAUSERDATA:
			progressDialog.dismiss();
			String result = (String) params[1];
			if (null == result) {
				Toast.makeText(context, "未连接网络", Toast.LENGTH_SHORT).show();
			} else {
				try {
					JSONObject jsonObject = new JSONObject(result);
					String state = jsonObject.getString(JsonString.Return.STATE).trim();
					if (state.equals(JsonString.Return.STATE_OK)) {
						defaultPerson.setPersonPhotoUrl(fileName);
						displayPersonVariable(defaultPerson);
						Toast.makeText(context, "更新头像成功", Toast.LENGTH_SHORT).show();	
						Map<String, Object> map = new HashMap<String, Object>();
						map.put(Person.PERSON_ID, defaultPersonId);
						map.put(Person.PERSON_PHOTOURL, fileName);
						context.localRefresh(HomePageString.UPDATA_PHOTO, map);
						personService.updataPersonInfo(defaultPersonId, Person.PERSON_PHOTOURL, fileName);
					} else {
						Toast.makeText(context, "网络竟然出错了", Toast.LENGTH_SHORT).show();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}	

		default:
			break;
		}
	}
	
	private void displayPerson(Person defaultPerson) {
		displayPersonVariable(defaultPerson);
		displayPersonBase(defaultPerson);
		txt_setting.setVisibility(View.VISIBLE);	//把设置那一行显示出来
	}
	
	/*显示Person资料*/
	private void displayPersonVariable(Person defaultPerson) {
		variable_adapter = new PersonVariableDataAdapter(context, defaultPerson);
		list_variable.setAdapter(variable_adapter);	//把三个可改变资料显示出来
	}
	
	private void displayPersonBase(Person defaultPerson) {
		String[] baseLeft = new String[] {"院系:", "年级:", "专业:", "姓名:", "性别:", };
		String[] baseRight = new String[] {defaultPerson.getPersonFaculty(), defaultPerson.getPersonYear(),
				defaultPerson.getPersonSpecialty(), defaultPerson.getPersonName(), defaultPerson.getPersonSex()};
		List<HashMap<String, Object>> list = new ArrayList<HashMap<String,Object>>();
		for (int i = 0; i < baseLeft.length; i++) {
			HashMap<String, Object> item = new HashMap<String, Object>();
			item.put(LIST_LEFT, baseLeft[i]);
			item.put(LIST_RIGHT, baseRight[i]);
			list.add(item);
		}
		SimpleAdapter base_adapter = new SimpleAdapter(context, list, R.layout.fm_me_base, 
				new String[] {LIST_LEFT, LIST_RIGHT, }, new int[] {R.id.txt_base_left, R.id.txt_base_right});
		list_base.setAdapter(base_adapter);			//把五个基本资料显示出来
	}

	public void exit() {
		personService.closeDBHelper();
	}

}
