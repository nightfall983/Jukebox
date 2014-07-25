package com.ketonax.jukebox.Adapter;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import android.content.Context;

import com.ketonax.jukebox.Util.MediaUtil;
import com.ketonax.jukebox.Util.Mp3Info;
import java.util.List;
import com.ketonax.jukebox.R;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by haoyang on 7/24/14.
 */
public class MusicListAdapter extends BaseAdapter{

    private Context context;
    private List<Mp3Info> mp3Infos;
    private Mp3Info mp3Info;
    private int pos = -1;

    public MusicListAdapter(Context context, List<Mp3Info> mp3Infos) {
        this.context = context;
        this.mp3Infos = mp3Infos;
    }

    @Override
    public int getCount() {
        return mp3Infos.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        return null;
    }
}
