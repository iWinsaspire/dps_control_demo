package cn.dolphinstar.ctrl.demo.utility;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mydlna.dlna.core.ContentInfoEx;

import java.util.List;

import cn.dolphinstar.ctrl.demo.R;

public class ContentInfoListAdapter extends ArrayAdapter {
    private final int resourceId ;
    public ContentInfoListAdapter(Context context, int resource, List<ContentInfoEx> obj) {
        super(context, resource,obj);
        resourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ContentInfoEx contentInfo = (ContentInfoEx)getItem(position);

        View view = LayoutInflater.from(getContext()).inflate(resourceId,null);
        TextView tv = view.findViewById(R.id.content_list_item_text);
        tv.setText(" >> " + contentInfo.title );

        return  view;
    }
}
