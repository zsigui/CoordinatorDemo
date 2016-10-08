package com.jackiez.materialdemo.extra.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.extra.adapter.holder.PicVH;
import com.jackiez.materialdemo.extra.listener.OnItemClickListener;

/**
 * Created by zsigui on 16-10-8.
 */

public class PicAdapter extends RecyclerView.Adapter<PicVH> implements View.OnClickListener {

    private static final int TAG_POS = 0x1234FFFF;

    private Context mContext;
    private int[] resIds = new int[]{R.drawable.london_flat, R.drawable.material_flat, R.drawable.sea,
            R.drawable.quila, R.drawable.quila2};
    private OnItemClickListener<Object> mListener;

    public PicAdapter(Context context) {
        mContext = context;
    }

    public OnItemClickListener<Object> getListener() {
        return mListener;
    }

    public void setListener(OnItemClickListener<Object> listener) {
        mListener = listener;
    }

    @Override
    public PicVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PicVH(LayoutInflater.from(mContext).inflate(R.layout.list_item_pic, parent, false));
    }

    @Override
    public void onBindViewHolder(PicVH holder, int position) {
        holder.ivItem.setImageDrawable(mContext.getResources().getDrawable(resIds[position % resIds.length]));
        holder.ivItem.setOnClickListener(this);
        holder.ivItem.setTag(TAG_POS, position);
    }

    @Override
    public int getItemCount() {
        return 24;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            int pos = (int) v.getTag(TAG_POS);
            mListener.onItemClick(v, pos, resIds[pos % resIds.length]);
        }
    }
}
