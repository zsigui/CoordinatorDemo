package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.extra.widget.MonthDateView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zsigui on 16-12-13.
 */

public class CustomViewActivity extends AppCompatActivity {

    MonthDateView mdv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_view2);
//        final HListView list = (HListView) findViewById(R.id.listView);
//        final MyAdapter adapter = new MyAdapter(this, getData());
//        list.setAdapter(adapter);
//        mdv = (MonthDateView) findViewById(R.id.mdv_content);
//        mdv.setItemTapListener(new MonthDateView.onItemTapListener() {
//            @Override
//            public void onItemTap(int index) {
//                Toast.makeText(CustomViewActivity.this, "单击事件 : " + index,  Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onItemLongTap(int index) {
//                Toast.makeText(CustomViewActivity.this, "长按事件 : " + index,  Toast.LENGTH_SHORT).show();
//
//            }
//
//            @Override
//            public void onItemDoubleTap(int index) {
//                Toast.makeText(CustomViewActivity.this, "双击事件 : " + index,  Toast.LENGTH_SHORT).show();
//
//            }
//        });
    }

    private List<Map<String, Object>> getData(){

        int [] pic = {R.drawable.a,R.drawable.b};

        ArrayList<Map<String,Object>> list = new ArrayList<>();
        HashMap<String, Object> map;
        for(int i = 0;i<pic.length;i++){
            map =new HashMap<>();
            map.put("index", "第"+(i+1)+"张");
            map.put("img", pic[i]);
            list.add(map);
        }
        return list;

    }

    public static class MyAdapter extends BaseAdapter {

        private Context mContext ;
        private List<Map<String,Object>> mList;

        public MyAdapter(Context context ,List<Map<String,Object>> list){
            this.mContext = context;
            this.mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HolderView holderView = null;
            if(convertView == null ){
                holderView = new HolderView();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent);

                holderView.imageView =(ImageView) convertView.findViewById(R.id.imageView);
                holderView.textView = (TextView) convertView.findViewById(R.id.textView);

                convertView.setTag(holderView);
            }else{
                holderView = (HolderView) convertView.getTag();
            }


            holderView.imageView.setImageResource((Integer) mList.get(position).get("img"));
            holderView.textView.setText((String) mList.get(position).get("index"));

            //return convertView.findViewById(R.id.item);
            return convertView;
        }

        class HolderView{
            ImageView imageView;
            TextView textView;
        }
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, CustomViewActivity.class));
    }
}
