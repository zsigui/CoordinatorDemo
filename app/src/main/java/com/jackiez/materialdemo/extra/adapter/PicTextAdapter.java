package com.jackiez.materialdemo.extra.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.ListItemPicTextBinding;

import java.util.ArrayList;

/**
 * Created by zsigui on 16-10-17.
 */

public class PicTextAdapter extends RecyclerView.Adapter<PicTextAdapter.PicHolder> implements View.OnClickListener {

    static final int TAG_POS = 0xffff1234;

    private LayoutInflater mInflater;

    private ArrayList<String> mList;

    public PicTextAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public PicHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ListItemPicTextBinding binding = DataBindingUtil.inflate(mInflater, R.layout.list_item_pic_text, parent, false);
        return new PicHolder(binding);
    }

    @Override
    public void onBindViewHolder(PicHolder holder, int position) {
        holder.mBinding.tvName.setText(mList.get(position));
        holder.mBinding.ivCover.setTag(TAG_POS, position);
        holder.mBinding.ivCover.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    public void updateData(ArrayList<String> s) {
        mList = s;
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        int pos = (int) v.getTag(TAG_POS);
        Snackbar.make(v, String.format("第%s项被点击", pos + 1), Snackbar.LENGTH_SHORT).show();
    }

    public void addMore(int i) {
        notifyItemRangeInserted(getItemCount() - i - 1, i);
    }

    public static class PicHolder extends RecyclerView.ViewHolder {


        ListItemPicTextBinding mBinding;

        public PicHolder(ListItemPicTextBinding binding) {
            super(binding.getRoot());
            setBinding(binding);
        }

        public void setBinding(ListItemPicTextBinding binding) {
            mBinding = binding;
        }
    }
}
