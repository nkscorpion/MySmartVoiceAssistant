package com.gizwits.opensource.appkit.ControlModule;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.gizwifisdk.listener.GizWifiDeviceListener;
import com.gizwits.opensource.appkit.CommonModule.GosBaseActivity;
import com.gizwits.opensource.appkit.CustomContent.DialogManager;
import com.gizwits.opensource.appkit.CustomContent.JsonParser;
import com.gizwits.opensource.appkit.CustomContent.Result_Adapter;
import com.gizwits.opensource.appkit.R;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.UnderstanderResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static com.gizwits.opensource.appkit.utils.MyUtil.check;
import static com.gizwits.opensource.appkit.utils.MyUtil.readFile;


public class GosDeviceControlActivity extends GosBaseActivity {
    
    private GizWifiDevice device;
    private Button btn_begin;
    private ListView result_ListView;
    private LinearLayout ProgressBar_Layout;
    
    private SpeechRecognizer tSpeechRecognizer;//语音识别对象，构建语法后可以识别语法文件中定义的特定语式
    //语意理解对象，识别非命令词的语音输入，返回网络中定义好的回答（包括语意提取和智能问答）
    private SpeechUnderstander tSpeechUnderstander;
    //语音合成对象，让APP开口说话
    private SpeechSynthesizer mTts;
    //初始化在线命令词识别语法的时候生成的ID，在命令词识别的过程中需要用到
    String grammarID = "";
    //读取到的本地命令词文件（.abnf后缀）
    String mCloudGrammar = null;
    //在说话的时候显示的Dialog
    private DialogManager mdialogManager;
    
    public String OriginalText = null; // 语音听写得到的内容
    public String UnderstanderText = null; //语意理解得到的内容
    public String MLC = null;          //命令词识别得到的内容
    
    ArrayList<String> Result_Texts = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gos_device_control);
        SpeechUtility.createUtility(this, "appid=" + "58004402");
        initDevice();
        initView();
        mCloudGrammar = readFile(this, "grammar_sample.abnf", "utf-8");
        showLog(mCloudGrammar);
        tSpeechRecognizer = SpeechRecognizer.createRecognizer(this, mInitListener);
        tSpeechUnderstander = SpeechUnderstander.createUnderstander(this, mInitListener);
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        buildCloudGrammar();
    }
    
    final int COMMAND_LED_OPEN = 1;
    final int COMMAND_MOTOR_OPEN = 2;
    final int COMMAND_MOTOR_CLOSE = 3;
    final int COMMAND_WSD = 4;
    final int COMMAND_INFRARED = 5;
    final int COMMAND_LED_CLOSE = 6;
    final int NO_MATCH = 0;
    final String NOMATCH_OUT_OF_COCA = "nomatch:out-of-voca";
    final String NOMATCH_NOISY = "nomatch:noisy";
    
    
    public int checkMLC(String mlc) {//todo
        String[] ledOpen = this.getResources().getStringArray(R.array.ledOpen);
        String[] motorOpen = this.getResources().getStringArray(R.array.motorOpen);
        String[] motorClose = this.getResources().getStringArray(R.array.motorClose);
        String[] wsd = this.getResources().getStringArray(R.array.wsd);
        String[] infrared = this.getResources().getStringArray(R.array.infrared);
        
        if (mlc.contains(NOMATCH_OUT_OF_COCA)) {
            DoSpeechUnderstand();
        } else if (mlc.contains(NOMATCH_NOISY)) {
            mTts.startSpeaking("听不懂，换种说法吧", mSynthesizerListener);
            showTip("噪音，无法识别");
            ProgressBar_Layout.setVisibility(View.GONE);
        } else {
            if (mlc.contains("关灯")) {
                return COMMAND_LED_CLOSE;
            }
            if (check(ledOpen, mlc)) {
                sendCommand("LED_OnOff", true);
                return COMMAND_LED_OPEN;
            } else if (check(motorOpen, mlc)) {
                return COMMAND_MOTOR_OPEN;
            } else if (check(motorClose, mlc)) {
                return COMMAND_MOTOR_CLOSE;
            } else if (check(wsd, mlc)) {
                return COMMAND_WSD;
            } else if (check(infrared, mlc)) {
                return COMMAND_INFRARED;
            }
        }
        
        return NO_MATCH;
    }
    
    
    private void initView() {
        btn_begin = (Button) findViewById(R.id.Begin_button);
        btn_begin.setEnabled(false);
        ProgressBar_Layout = (LinearLayout) findViewById(R.id.progressbar_LinearLayout);
        result_ListView = (ListView) findViewById(R.id.result_ListView);
        mdialogManager = new DialogManager(this);
        
        result_ListView.setAdapter(new Result_Adapter(GosDeviceControlActivity.this, Result_Texts));
        
        btn_begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("onClick");
                setCloudParam();
                tSpeechRecognizer.startListening(recognizerListener);
            }
        });
    }
    
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code);
            } else {
                showLog("语音合成对象初始化成功 code= " + code);
            }
        }
    };
    
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int i) {
            if (i != ErrorCode.SUCCESS) {
                showTip("初始化失败，errorCode =" + i);
            }
        }
    };
    
    private void buildCloudGrammar() {
        tSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置文本编码格式
        tSpeechRecognizer.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        int ret = tSpeechRecognizer.buildGrammar("abnf", mCloudGrammar,
                            mGrammarListener);
        if (ret != ErrorCode.SUCCESS)
            showTip("语法构建失败,错误码：" + ret);
    }
    
    public void setCloudParam() {
        tSpeechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        // 设置识别引擎
        tSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        tSpeechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置云端识别使用的语法id
        tSpeechRecognizer.setParameter(SpeechConstant.CLOUD_GRAMMAR, grammarID);
        tSpeechRecognizer.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        tSpeechRecognizer.setParameter(SpeechConstant.ASR_AUDIO_PATH,
                            Environment.getExternalStorageDirectory() + "/tanjie/input.wav");
    }
    
    private GrammarListener mGrammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String s, SpeechError speechError) {
            if (speechError == null) {
                grammarID = s;
                showTip("语法构建成功：" + s);
                btn_begin.setEnabled(true);
            } else {
                showTip("语法构建失败,错误码：" + speechError.getErrorCode());
            }
        }
    };
    
    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            int v = 7 * i / 30 + 1;
            mdialogManager.updateVoiceLevel(v);
        }
        
        @Override
        public void onBeginOfSpeech() {
            mdialogManager.showRecordingDialog();
        }
        
        @Override
        public void onEndOfSpeech() {
            mdialogManager.dimissDialog();
            ProgressBar_Layout.setVisibility(View.VISIBLE);
        }
        
        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            showLog("onResult1");
            DoNext(recognizerResult);
        }
        
        @Override
        public void onError(SpeechError speechError) {
            showLog(speechError.toString());
            if (speechError.getErrorCode() == 10703) {
                showTip("网络连接发生异常（可能.abnf文件中有语法错误）");
                ProgressBar_Layout.setVisibility(View.GONE);
            }
            if (speechError.getErrorCode() == 10118 || speechError.getErrorCode() == 10119) {
                showTip("你没有说话");
                ProgressBar_Layout.setVisibility(View.GONE);
            }
            
        }
        
        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            
        }
    };
    
    
    private HashMap<String, String> IatResults = new LinkedHashMap<>();
    
    private void DoNext(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        
        showLog("RecognizerResult: " + text);
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        IatResults.put(sn, text);
        StringBuilder resultBuffer = new StringBuilder();
        for (String key : IatResults.keySet()) {
            resultBuffer.append(IatResults.get(key));
        }
        MLC = resultBuffer.toString();
        String speckStr = "执行命令：" + MLC + "，成功！";
        showLog("命令词是:" + MLC);
        //// TODO: 2017/3/19 `````````````````````````````````````````````````````````````
        switch (checkMLC(MLC)) {
            case NO_MATCH:
                ProgressBar_Layout.setVisibility(View.GONE);
                break;
            case COMMAND_LED_OPEN:
                if (MLC.contains("黄")) {
                    sendCommand("LED_Color", 1);
                    //保存返回指令
                } else if (MLC.contains("紫")) {
                    sendCommand("LED_Color", 2);
                } else if (MLC.contains("粉")) {
                    sendCommand("LED_Color", 3);
                } else {
                    sendCommand("LED_OnOff", true);
                }
                Result_Texts.add("执行命令：" + MLC + "，成功！");
                result_ListView.setAdapter(new Result_Adapter(GosDeviceControlActivity.this, Result_Texts));
                ProgressBar_Layout.setVisibility(View.GONE);
                mTts.startSpeaking(speckStr, mSynthesizerListener);
                break;
            case COMMAND_MOTOR_OPEN:
                sendCommand("Motor_Speed", 1);
                Result_Texts.add("执行命令：" + MLC + "，成功！");
                timer.schedule(new RemindTask(), 2 * 1000);
                result_ListView.setAdapter(new Result_Adapter(GosDeviceControlActivity.this, Result_Texts));
                ProgressBar_Layout.setVisibility(View.GONE);
                mTts.startSpeaking(speckStr, mSynthesizerListener);
                break;
            case COMMAND_MOTOR_CLOSE:
                sendCommand("Motor_Speed", -1);
                Result_Texts.add("执行命令：" + MLC + "，成功！");
                timer.schedule(new RemindTask(), 2 * 1000);
                result_ListView.setAdapter(new Result_Adapter(GosDeviceControlActivity.this, Result_Texts));
                ProgressBar_Layout.setVisibility(View.GONE);
                mTts.startSpeaking(speckStr, mSynthesizerListener);
                break;
            case COMMAND_WSD:
                device.setListener(mListener);
                device.getDeviceStatus();
                time2.schedule(new WaitwsdTask(), 2 * 1000);
                break;
            case COMMAND_INFRARED:
                device.setListener(mListener2);
                device.getDeviceStatus();
                time3.schedule(new WatiHwTask(), 2 * 1000);
                break;
            case COMMAND_LED_CLOSE:
                sendCommand("LED_Color", 0);
                sendCommand("LED_OnOff", false);
                Result_Texts.add("执行命令：" + MLC + "，成功！");
                result_ListView.setAdapter(new Result_Adapter(GosDeviceControlActivity.this, Result_Texts));
                ProgressBar_Layout.setVisibility(View.GONE);
                mTts.startSpeaking(speckStr, mSynthesizerListener);
                break;
        }
    }
    
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x001:
                    Result_Texts.add("门窗状态为:" + doorstate);
                    result_ListView.setAdapter(new Result_Adapter(GosDeviceControlActivity.this, Result_Texts));
                    ProgressBar_Layout.setVisibility(View.GONE);
                    mTts.startSpeaking("门窗状态为" + doorstate, mSynthesizerListener);
                    break;
                case 0x002:
                    Result_Texts.add("温度为：" + Temperature + "    湿度为：" + Humidity);
                    result_ListView.setAdapter(new Result_Adapter(GosDeviceControlActivity.this, Result_Texts));
                    ProgressBar_Layout.setVisibility(View.GONE);
                    mTts.startSpeaking("当前温度为：" + deviceStatu.get("Temperature") + "摄氏度"
                                                           + "。当前湿度为：" + "百分之" + deviceStatu.get("Humidity"), mSynthesizerListener);
                    break;
                case 0x003:
                    Result_Texts.add("网络状态不佳，请求超时");
                    result_ListView.setAdapter(new Result_Adapter(GosDeviceControlActivity.this, Result_Texts));
                    ProgressBar_Layout.setVisibility(View.GONE);
                    break;
            }
        }
    };
    
    Timer timer = new Timer();
    Timer time2 = new Timer();
    Timer time3 = new Timer();
    
    private class WatiHwTask extends TimerTask {
        @Override
        public void run() {
            if (doorstate != null) {
                Message msg = new Message();
                msg.what = 0x001;
                handler.sendMessage(msg);
            } else {
                Message msg = new Message();
                msg.what = 0x003;
                handler.sendMessage(msg);
            }
        }
        
    }
    
    private class WaitwsdTask extends TimerTask {
        @Override
        public void run() {
            if (Temperature != -1 && Humidity != -1) {
                Message msg = new Message();
                msg.what = 0x002;
                handler.sendMessage(msg);
            } else {
                Message msg = new Message();
                msg.what = 0x003;
                handler.sendMessage(msg);
            }
        }
    }
    
    private class RemindTask extends TimerTask {
        public void run() {
            sendCommand("Motor_Speed", 0);
        }
    }
    
    
    private void DoSpeechUnderstand() {
        if (tSpeechUnderstander.isUnderstanding()) {
            tSpeechUnderstander.stopUnderstanding();
            showTip("停止录音");
        } else {
            tSpeechUnderstander.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
            tSpeechUnderstander.setParameter(SpeechConstant.ASR_SOURCE_PATH,
                                Environment.getExternalStorageDirectory() + "/tanjie/input.wav");
            int ret = tSpeechUnderstander.startUnderstanding(mUnderstanderListener);
            if (ret != 0) {
                showTip("语义理解失败,错误码:" + ret);
            }
        }
    }
    
    private com.iflytek.cloud.SpeechUnderstanderListener mUnderstanderListener = new com.iflytek.cloud.SpeechUnderstanderListener() {
        @Override
        public void onVolumeChanged(int volume, byte[] bytes) {
            showTip("当前正在说话，音量大小：" + volume);
        }
        
        @Override
        public void onBeginOfSpeech() {
            showTip("开始说话");
        }
        
        @Override
        public void onEndOfSpeech() {
            showTip("结束说话");
        }
        
        @Override
        public void onResult(final UnderstanderResult result) {
            showLog("onResult2");
            if (result != null) {
                String text = result.getResultString();
                if (!TextUtils.isEmpty(text)) {
                    showLog("Understand_result_Text:" + text);
                    String rc = JsonParser.getRC(result.getResultString());
                    showLog("rc: " + rc);
                    int i = Integer.valueOf(rc);
                    if (i == 0) {
                        UnderstanderText = JsonParser.getText(text);
                        showLog("UnderstanderText:  " + UnderstanderText);
                    }
                    if (i == 4) {//如果语意理解任然没有正确的返回值
                        OriginalText = JsonParser.getText(text);
                        showLog("OriginalText:  " + OriginalText);
                    } else {
                        OriginalText = "由于本地或者服务器发生错误，无法识别";
                    }
                } else {
                    showTip("识别结果不正确。");
                }
            }
            
            ProgressBar_Layout.setVisibility(View.GONE);
        }
        
        @Override
        public void onError(SpeechError speechError) {
            showTip(speechError.getPlainDescription(true));
        }
        
        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            
        }
    };
    
    String doorstate = null;
    
    GizWifiDeviceListener mListener2 = new GizWifiDeviceListener() {
        @Override
        public void didReceiveData(GizWifiErrorCode result, GizWifiDevice device, ConcurrentHashMap<String, Object> dataMap, int sn) {
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                ConcurrentHashMap<String, Object> deviceStatu = (ConcurrentHashMap<String, Object>) dataMap.get("data");
                if (deviceStatu != null) {
                    boolean Infrared = (boolean) deviceStatu.get("Infrared");
                    String state = "";
                    if (Infrared) {
                        state = "关";
                    } else {
                        state = "开";
                    }
                    doorstate = state;
                }
            }
        }
    };
    
    private SynthesizerListener mSynthesizerListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            
        }
        
        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {
            
        }
        
        @Override
        public void onSpeakPaused() {
            
        }
        
        @Override
        public void onSpeakResumed() {
            
        }
        
        @Override
        public void onSpeakProgress(int i, int i1, int i2) {
            
        }
        
        @Override
        public void onCompleted(SpeechError speechError) {
            
        }
        
        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            
        }
    };
    
    Integer Temperature = -1;
    Integer Humidity = -1;
    GizWifiDeviceListener mListener = new GizWifiDeviceListener() {
        @Override
        public void didReceiveData(GizWifiErrorCode result, GizWifiDevice device,
                                   ConcurrentHashMap<String, Object> dataMap, int sn) {
            if (dataMap.isEmpty()) {
                return;
            }
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                deviceStatu = (ConcurrentHashMap<String, Object>) dataMap.get("data");
                if (deviceStatu != null) {
                    Temperature = (Integer) deviceStatu.get("Temperature");
                    Humidity = (Integer) deviceStatu.get("Humidity");
                }
            }
        }
    };
    
    private void sendCommand(String key, Object value) {
        int sn = 5;
        ConcurrentHashMap<String, Object> hashMap = new ConcurrentHashMap<>();
        hashMap.put(key, value);
        device.write(hashMap, sn);
        Log.i("tanjie", hashMap.toString());
    }
    
    private ConcurrentHashMap<String, Object> deviceStatu;
    
    private void initDevice() {
        Intent intent = getIntent();
        device = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
        deviceStatu = new ConcurrentHashMap<>();
    }
    
    
    class getDeviceInfoTaks extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            device.setListener(mListener);
            device.getDeviceStatus();
            return null;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
}
