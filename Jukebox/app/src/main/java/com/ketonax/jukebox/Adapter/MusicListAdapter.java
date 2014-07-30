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
    /******** What is the size of Passed Arraylist Size ************/
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
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View vi = convertView;
        ViewHolder holder;

        if(convertView==null){

            /****** Inflate .xml file for each row ( Defined below ) *******/
            vi = LayoutInflater.from(context).inflate(R.layout.text_music_list, null);
            /****** View Holder Object to contain .xml file elements ******/
            holder = new ViewHolder();
            holder.albumImage = (ImageView) vi.findViewById(R.id.album_image);
            holder.musicTitle=(TextView)vi.findViewById(R.id.music_title);
            holder.musicArtist=(TextView)vi.findViewById(R.id.music_artist);
            holder.musicDuration=(TextView)vi.findViewById(R.id.music_duration);
            /************  Set holder with LayoutInflater ************/
            vi.setTag( holder );
        }
        else {
            holder = (ViewHolder) vi.getTag();
        }

        mp3Info = mp3Infos.get(position);
        holder.musicTitle.setText(mp3Info.getTitle());
        holder.musicArtist.setText(mp3Info.getArtist());
        holder.musicDuration.setText(MediaUtil.formatTime(mp3Info.getDuration()));

        return vi;
    }
}
