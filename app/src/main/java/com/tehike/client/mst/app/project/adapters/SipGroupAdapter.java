package com.tehike.client.mst.app.project.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.entity.SipGroupBean;
import java.util.List;

/**
 * SipGroup分组适配器
 * <p>
 * Created by wpf
 */

public class SipGroupAdapter extends RecyclerView.Adapter<SipGroupAdapter.GridViewHolder> {
    /**
     * 上下文对象
     */
    private Context mContext;

    /**
     * 盛放数据的集合
     */
    private List<SipGroupBean> mDateBeen;

    /**
     * 回调接口
     */
    private MyItemClickListener mItemClickListener;

    public SipGroupAdapter(Context context, List<SipGroupBean> dateBeen) {
        mContext = context;
        mDateBeen = dateBeen;
    }

    @Override
    public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = View.inflate(mContext, R.layout.sip_group_recyclearview_item, null);
        GridViewHolder gridViewHolder = new GridViewHolder(itemView, mItemClickListener);
        return gridViewHolder;
    }

    @Override
    public void onBindViewHolder(GridViewHolder holder, int position) {
        SipGroupBean dateBean = mDateBeen.get(position);
        holder.setData(dateBean);
    }

    @Override
    public int getItemCount() {
        if (mDateBeen != null && mDateBeen.size() > 0) {
            return mDateBeen.size();
        }
        return 0;
    }

    public static class GridViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private MyItemClickListener mListener;
        private final TextView item_title;

        public GridViewHolder(View itemView, MyItemClickListener myItemClickListener) {
            super(itemView);
            item_title = (TextView) itemView.findViewById(R.id.show_sip_group_name);
            this.mListener = myItemClickListener;
            itemView.setOnClickListener(this);
        }

        public void setData(SipGroupBean data) {
            item_title.setText(data.getGroup_name());
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClick(v, getPosition());
            }
        }
    }

    //item点击回调
    public interface MyItemClickListener {
        void onItemClick(View view, int position);
    }

    //设置回调方式
    public void setItemClickListener(MyItemClickListener myItemClickListener) {
        this.mItemClickListener = myItemClickListener;
    }
}
