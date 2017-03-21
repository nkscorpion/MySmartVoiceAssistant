package com.gizwits.opensource.appkit.CustomContent;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gizwits.opensource.appkit.R;


/**
 * 语音交互对话框类
 */

public class DialogManager {

    private Dialog mDialog;
    private ImageView mIcon, mVoice;
    private TextView mLabel;

    private Context mContext;

    public DialogManager(Context mContext) {
        this.mContext = mContext;
    }


    public void recording() {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.VISIBLE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.mipmap.recorder);
            mLabel.setText("手指上滑，取消发送");
        }
    }

    /**
     * 显示正在录音的对话框
     */
    public void showRecordingDialog() {
        mDialog = new Dialog(mContext, R.style.Theme_AudioDialog);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_recorder, null);
        mDialog.setContentView(view);
        mIcon = (ImageView) mDialog.findViewById(R.id.id_recorder_dialog_icon);
        mVoice = (ImageView) mDialog.findViewById(R.id.id_recorder_dialog_voice);
        mLabel = (TextView) mDialog.findViewById(R.id.id_recorder_dialog_label);

        mDialog.show();

    }

    /**
     * 显示上滑取消发送的对话框
     */
    public void wantToCancel() {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.mipmap.cancel);
            mLabel.setText("松开手指，取消发送");
        }
    }

    /**
     * 显示录音时间过短的对话框
     */
    public void tooShort() {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.mipmap.voice_to_short);
            mLabel.setText("录音时间过短");
        }
    }

    /**
     * 隐藏对话框
     */
    public void dimissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    /**
     * 通过level去更新ImageView_voice上的图片
     */
    public void updateVoiceLevel(int level) {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.VISIBLE);
            mLabel.setVisibility(View.VISIBLE);
            int resId = mContext.getResources().getIdentifier("v" + level, "mipmap", mContext.getPackageName());
            mVoice.setImageResource(resId);
        }
    }

}
