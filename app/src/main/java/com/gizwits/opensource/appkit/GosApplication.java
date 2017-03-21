package com.gizwits.opensource.appkit;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

public class GosApplication extends Application {

	public static int flag = 0;

	public void onCreate() {
		super.onCreate();
       SpeechUtility.createUtility(GosApplication.this, "appid=" + "58004402");


	}
}
