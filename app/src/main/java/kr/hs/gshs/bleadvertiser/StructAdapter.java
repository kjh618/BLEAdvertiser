package kr.hs.gshs.bleadvertiser;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import kr.hs.gshs.blebeaconprotocollibrary.Struct;

/**
 * Created by kjh on 2017-12-10.
 */

public class StructAdapter extends BaseAdapter {
    private ArrayList<Struct> items;
    private Context mContext;
    private LayoutInflater mInflater;

    StructAdapter(Context context, LayoutInflater inflater) {
        super();
        mContext = context;
        mInflater = inflater;
        items = new ArrayList<>();
    }

    public void addItem(Struct item) {
        items.add(item);
    }

    public void removeItem(int position) {
        items.remove(position);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).hashCode();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if(view == null) {
            view = mInflater.inflate(R.layout.list_item_struct, null);
        }

        TextView textViewLen = (TextView) view.findViewById(R.id.textViewLen);
        TextView textViewType = (TextView) view.findViewById(R.id.textViewType);
        TextView textViewData = (TextView) view.findViewById(R.id.textViewData);

        Struct item = items.get(position);

        textViewLen.setText(String.valueOf(item.getLength()));
        textViewType.setText(item.getType().displayName());
        textViewData.setText(item.getData());

        return view;
    }
}
