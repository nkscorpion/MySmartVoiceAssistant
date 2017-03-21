package com.gizwits.opensource.appkit.CustomContent;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gizwits.opensource.appkit.R;

import java.util.List;

/**
 * 创建者：TAN
 * 创建时间： 2017/3/19.
 */

public class Result_Adapter extends BaseAdapter {
    
    private LayoutInflater mInflater;
    private List<String> resultTexts;
    
    public Result_Adapter(Context context, List<String> data) {
        mInflater = LayoutInflater.from(context);
        resultTexts = data;
    }
    
    @Override
    public int getCount() {
        return resultTexts.size();
    }
    
    @Override
    public Object getItem(int i) {
        return resultTexts.get(i);
    }
    
    @Override
    public long getItemId(int i) {
        return i;
    }
    
    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder=new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_result_text, null);
            viewHolder.tv_result= (TextView) convertView.findViewById(R.id.result_textview);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String result= resultTexts.get(i);
        viewHolder.tv_result.setText(result);
        return convertView;
    }
    
    private class ViewHolder {
        TextView  tv_result;
    }
    
}
