package com.ketonax.jukebox.Activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.List;

import com.ketonax.jukebox.Adapter.MusicListAdapter;
import com.ketonax.jukebox.Adapter.StringAdapter;
import com.ketonax.jukebox.Util.MediaUtil;
import com.ketonax.jukebox.Util.Mp3Info;
import com.ketonax.jukebox.R;

import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Created by haoyang on 7/24/14.
 */
public class MusicList extends Activity implements OnItemClickListener{
    private ListView mMusiclist;
   // String[]days={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
    MediaUtil music = new MediaUtil();
    private List<Mp3Info> mp3Infos = null;
    MusicListAdapter listAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mMusiclist = (ListView) findViewById(R.id.list_music);
       // ArrayAdapter<String> stringAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,days);
      //  mMusiclist.setAdapter(stringAdapter);

        //Set List Adapter

        mp3Infos = MediaUtil.getMp3Infos(MusicList.this);
        //setListAdpter(MediaUtil.getMusicMaps(mp3Infos))
        listAdapter = new MusicListAdapter(this, mp3Infos);
        mMusiclist.setAdapter(listAdapter);

        mMusiclist.setOnItemClickListener(this);
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //TextView temp=(TextView)view;
        //Toast.makeText(this,temp.getText()+" "+i,Toast.LENGTH_SHORT).show();
    }
}

