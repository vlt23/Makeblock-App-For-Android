package cc.makeblock.makeblock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class DeviceListAdapter extends BaseAdapter {

    private List<String> mData;
    private final int mResource;
    private final LayoutInflater mLayoutInflater;

    public DeviceListAdapter(Context context, List<String> list, int resource) {
        this.mData = list;
        this.mResource = resource;
        this.mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void updateData(List<String> list) {
        this.mData = list;
    }

    @Override
    public int getCount() {
        return this.mData.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View contentView, ViewGroup parent) {
        if (contentView == null) {
            contentView = this.mLayoutInflater.inflate(this.mResource, parent, false);

            TextView titleView = contentView.findViewById(R.id.device_item_title);
            TextView descriptionView = contentView.findViewById(R.id.device_item_description);
            String[] msg = this.mData.get(position).split(" ");
            titleView.setText(msg[0]);
            if (msg.length > 2) {
                descriptionView.setText(msg[1] + " " + msg[2]);
            } else {
                descriptionView.setText(msg[1]);
            }
        }
        return contentView;
    }

}
