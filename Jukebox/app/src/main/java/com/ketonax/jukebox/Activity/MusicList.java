package com.ketonax.jukebox.Activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import java.util.List;

import com.ketonax.jukebox.Adapter.MusicListAdapter;
import com.ketonax.jukebox.Util.MediaUtil;
import com.ketonax.jukebox.Util.Mp3Info;
import com.ketonax.jukebox.R;


/**
 * Created by haoyang on 7/24/14.
 */
public class MusicList extends Activity{
    private ListView mMusiclist;
    private List<Mp3Info> mp3Infos = null;
    MusicListAdapter listAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_list);

        mMusiclist = (ListView) findViewById(R.id.list_music);
        mp3Infos = MediaUtil.getMp3Infos(MusicList.this);
        // setListAdpter(MediaUtil.getMusicMaps(mp3Infos))
        listAdapter = new MusicListAdapter(this, mp3Infos);
        mMusiclist.setAdapter(listAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

