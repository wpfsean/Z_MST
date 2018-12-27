package com.tehike.client.mst.app.project.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.tehike.client.mst.app.project.R;

import java.util.List;


/**
 * Created by Root on 2018/7/9.
 * <p>
 * y底部的sliding滑动适配器
 * <p>
 * 根據type判定
 * <p>
 * type:
 * 0:網絡對講
 * 1:視頻監控
 * 2:即時通信
 * 3:應急報警
 * 4:申請供彈
 */

public class ButtomSlidingAdapter extends RecyclerView.Adapter<ButtomSlidingAdapter.ViewHolder> {

    Context context;
    int[] images;
    int type;

    //回调
    private OnItemClickListener onItemClickListener;

    public ButtomSlidingAdapter(Context context, int[] images, int type) {
        this.context = context;
        this.images = images;
        this.type = type;
    }

    //设置回调方法
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public ButtomSlidingAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.activity_port_bottom_item_layout, null);
        ButtomSlidingAdapter.ViewHolder holder = new ButtomSlidingAdapter.ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ButtomSlidingAdapter.ViewHolder holder, final int position) {

        if (position == 0)
            holder.imageButton.setBackgroundResource(R.mipmap.port_network_intercom_btn_selected);
        else
            holder.imageButton.setBackgroundResource(images[position]);

        if (onItemClickListener != null) {
            holder.imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onClick(position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton imageButton;

        public ViewHolder(View itemView) {
            super(itemView);
            imageButton = itemView.findViewById(R.id.bottom_item_bg_btn);

        }
    }

    //回调
    public interface OnItemClickListener {
        void onClick(int position);
    }
}
