package com.tehike.client.mst.app.project.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.base.BaseFragment;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.entity.VideoBen;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.utils.HttpBasicRequest;
import com.tehike.client.mst.app.project.utils.HttpUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 描述：$desc$ 列表资源
 * ===============================
 *
 * @author $user$ wpfsean@126.com
 * @version V1.0
 * @Create at:$date$ $time$
 */

public class ResourcesList extends BaseFragment {


    List<VideoBen> intentList = new ArrayList<>();

    /**
     * video组数据
     */
    List<VideoGrourpBean> groupList = new ArrayList<>();

    /**
     * 视频资源组内选项
     */
    List<List<VideoBen>> chirdList = new ArrayList<>();

    /**
     * 当前页面是否可见
     */
    boolean currentPageVisible = false;

    /**
     * 二级列表展示
     */
    @BindView(R.id.expand)
    ExpandableListView expandableListView;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_port_video_resources_layout;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        initVideoGroupData();
    }

    /**
     * 加载视频组数据
     */
    private void initVideoGroupData() {
        if (NetworkUtils.isConnected()) {

            String serverIp = DbConfig.getInstance().getData(4);
            if (TextUtils.isEmpty(serverIp))
                serverIp = AppConfig.SERVERIP;

            //子线程去进行web接口状态请求
            HttpUtils httpUtil = new HttpUtils("http://"+serverIp+":8080/webapi/"+AppConfig.WEB_API_VIDEOGROUPS_PARAMATER, new HttpUtils.GetHttpData() {
                @Override
                public void httpData(String result) {
                    String str = result;
                    if (!TextUtils.isEmpty(str)) {
                        Message message = new Message();
                        message.what = 3;
                        message.obj = str;
                        handler.sendMessage(message);
                    } else {
                        handler.sendEmptyMessage(2);
                    }
                }
            });
            new Thread(httpUtil).start();
        } else {

            handler.sendEmptyMessage(1);
        }
    }

    /**
     * 解析视频组数据
     *
     * @param msg
     */
    private void resolveJSON(Message msg) {
        String result = (String) msg.obj;
        if (TextUtils.isEmpty(result)) {
            handler.sendEmptyMessage(2);
            return;
        }
        try {

            JSONObject jsonObject = new JSONObject(result);
            int count = jsonObject.getInt("count");
            if (count > 0) {
                JSONArray jsonArray = jsonObject.getJSONArray("groups");
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject js = jsonArray.getJSONObject(i);
                        int id = js.getInt("id");
                        String member_count = js.getString("member_count");
                        String name = js.getString("name");
                        VideoGrourpBean videoGrourpBean = new VideoGrourpBean();
                        videoGrourpBean.setId(id);
                        videoGrourpBean.setName(name);
                        videoGrourpBean.setMember_count(member_count);
                        groupList.add(videoGrourpBean);
                    }
                    Logutils.i("BBB--->" + groupList.toString());
                    handler.sendEmptyMessage(4);
                }
            }
        } catch (JSONException e) {
            Logutils.e("group--->>JSONException:--->>" + e.getMessage());
        }
    }

    /**
     * 获取组内数据
     */
    private void displayVideoResources() {
        if (groupList != null && groupList.size() > 0) {
            for (int i = 0; i < groupList.size(); i++) {
                int id = groupList.get(i).getId();
                Logutils.i("id-->>>" + id);

                String serverIp = DbConfig.getInstance().getData(4);
                if (TextUtils.isEmpty(serverIp))
                    serverIp = AppConfig.SERVERIP;



                String url = "http://"+serverIp+":8080/webapi/"+AppConfig.WEB_API_VIDEOGROUPS_SOURCES_PARAMATER+ id;
                if (NetworkUtils.isConnected()) {
                    HttpUtils httpUtil = new HttpUtils(url, new HttpUtils.GetHttpData() {
                        @Override
                        public void httpData(String result) {
                            String str = result;
                            if (!TextUtils.isEmpty(str)) {
                                Message message = new Message();
                                message.what = 5;
                                message.obj = str;
                                handler.sendMessage(message);
                            } else {
                                handler.sendEmptyMessage(2);
                            }
                        }
                    });
                    new Thread(httpUtil).start();
                } else {
                    handler.sendEmptyMessage(1);
                }
            }
        }
    }

    /**
     * 解析组内视频资源
     */
    private void resolveChirdJSON(Message msg) {
        String result = (String) msg.obj;
        if (TextUtils.isEmpty(result)) {
            handler.sendEmptyMessage(2);
            return;
        }
        List<VideoBen> v = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(result);
            int count = jsonObject.getInt("count");
            if (count > 0) {
                JSONArray jsonArray = jsonObject.getJSONArray("sources");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject j = jsonArray.getJSONObject(i);
                    VideoBen videoBen = new VideoBen();
                    videoBen.setName(j.getString("name"));
                    v.add(videoBen);
                }
                chirdList.add(v);
            }
            handler.sendEmptyMessage(6);
        } catch (JSONException e) {
            Logutils.e("chird--->>>JSONException:" + e.getMessage());
        }
    }


    /**
     * 展示数据
     */
    private void displayView() {
        if (chirdList != null && chirdList.size() > 0) {
            expandableListView.setAdapter(new VideoResourceAdapter(getActivity(), groupList, chirdList));
            expandableListView.setGroupIndicator(null);


            expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {

                    ImageView icon = v.findViewById(R.id.group_icon_layout);
                    if (icon != null) {
                        icon.setBackgroundResource(R.mipmap.port_data_icon_to_group_selected);
                    }
                    if (expandableListView.isGroupExpanded(groupPosition)) {
                        expandableListView.collapseGroup(groupPosition);
                        if (icon != null) {
                            icon.setBackgroundResource(R.mipmap.port_data_icon_to_group_normal);
                        }
                    }
                    return false;
                }
            });
        }
    }

    /**
     * 视频组对象
     */
    public class VideoGrourpBean implements Serializable {
        int id;
        String member_count;
        String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getMember_count() {
            return member_count;
        }

        public void setMember_count(String member_count) {
            this.member_count = member_count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "VideoGrourpBean{" +
                    "id=" + id +
                    ", member_count='" + member_count + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }

        public VideoGrourpBean() {
        }
    }


    @OnClick(R.id.bottom_preview_layout)
    public void preview(View view) {

        if (intentList != null && intentList.size()>0 && intentList.size()<=4){
            Intent intent = new Intent();
            intent.setAction("aa");
            intent.putExtra("a",(Serializable) intentList);
            getActivity().sendBroadcast(intent);
            getActivity().finish();
        }
        Logutils.i(intentList.size() + "\n" + intentList.toString());
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        currentPageVisible = isVisibleToUser;

        Logutils.i("资源分组页面是否可见:" + currentPageVisible);

        super.setUserVisibleHint(isVisibleToUser);
    }


    @Override
    public void onDestroyView() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        super.onDestroyView();
    }


    /**
     * 二级列表适配器
     */
    class VideoResourceAdapter extends BaseExpandableListAdapter {

        /**
         * 上下文
         */
        private Context mContext;

        /**
         * 组资源
         */
        private List<ResourcesList.VideoGrourpBean> list;

        /**
         * 组内资源
         */
        List<List<VideoBen>> childList;


        /**
         * 适配器构造方法
         *
         * @param mContext
         * @param list
         * @param chirdList
         */
        public VideoResourceAdapter(Context mContext, List<ResourcesList.VideoGrourpBean> list, List<List<VideoBen>> chirdList) {
            super();
            this.mContext = mContext;
            this.list = list;
            this.childList = chirdList;
        }

        @Override
        public int getGroupCount() {
            //组长度
            return list.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            //组内子项长度
            return childList.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            //组名称
            return list.get(groupPosition).getName();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            //组内item项
            return childList.get(groupPosition).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            //组id
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            //子项itemId
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            VideoResourceAdapter.ViewHolder_group group = null;
            if (convertView == null) {
                group = new VideoResourceAdapter.ViewHolder_group();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.activity_port_videoresources_group_layout, null);
                group.text_group = (TextView) convertView.findViewById(R.id.group_name_layout);
                convertView.setTag(group);
            } else {
                group = (VideoResourceAdapter.ViewHolder_group) convertView.getTag();
            }
            group.text_group.setText(list.get(groupPosition).getName());

            return convertView;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            VideoResourceAdapter.ViewHolder_child child = null;
            if (convertView == null) {
                child = new VideoResourceAdapter.ViewHolder_child();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.activity_port_videoresources_child_layout, null);
                child.text_child = (TextView) convertView.findViewById(R.id.child_name_layout);
                child.cb_child = (CheckBox) convertView.findViewById(R.id.child_check_layout);
                child.icon_child = (ImageView) convertView.findViewById(R.id.child_icon_layout);
                convertView.setTag(child);
            } else {
                child = (VideoResourceAdapter.ViewHolder_child) convertView.getTag();
            }
            child.text_child.setText(childList.get(groupPosition).get(childPosition).getName());
            final ViewHolder_child finalChild = child;
            final ViewHolder_child finalChild1 = child;
            child.cb_child.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {

                        Logutils.i("选中：" + childList.get(groupPosition).get(childPosition).getUsername());
                        if (intentList.size() < 4) {
                            intentList.add(childList.get(groupPosition).get(childPosition));
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finalChild1.icon_child.setBackgroundResource(R.mipmap.port_data_icon_camera__selected);
                            }
                        });
                    } else {
                        Logutils.i("取消选中：" + childList.get(groupPosition).get(childPosition).getUsername());
                        if (intentList.contains(childList.get(groupPosition).get(childPosition)))
                            intentList.remove(childList.get(groupPosition).get(childPosition));

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finalChild.icon_child.setBackgroundResource(R.mipmap.port_data_icon_camera__normal);
                            }
                        });
                    }
                }
            });
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        //组显示的ViewHolder
        class ViewHolder_group {
            TextView text_group;
        }

        //组内item显示的ViewHolder
        class ViewHolder_child {
            TextView text_child;
            CheckBox cb_child;
            ImageView icon_child;
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (currentPageVisible)
                        showProgressFail("请检查网络~");
                    break;
                case 2:
                    if (currentPageVisible)
                        showProgressFail("未获取到数据~");
                    break;
                case 3:
                    resolveJSON(msg);
                    break;
                case 4:
                    if (currentPageVisible)
                        displayVideoResources();
                    break;
                case 5:
                    resolveChirdJSON(msg);
                    break;

                case 6:
                    if (currentPageVisible)
                        displayView();
                    break;
            }
        }
    };


}
