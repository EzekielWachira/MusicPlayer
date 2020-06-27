package com.ezzy.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS = 12;

    private static final int PERMISSIONS_COUNT = 1;

    @SuppressLint("NewApi")
    private boolean arePermissionsGranted(){
        for (int i = 0; i < PERMISSIONS_COUNT;  i++){
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_DENIED){
                return true;  //Permissions  are not granted
            }
        }
        //Permissions are already granted
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (arePermissionsGranted()){
            //Prevents the app from asking the user permissions all over again
            ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            recreate();
        }else {
            onResume();
        }
    }

    private boolean isMusicPlayerInitialized;
    private List<String> musicFileList;

    private void addMusicFileFrom(String dirPath){
        final File musicDir = new File(dirPath);
        if (!musicDir.exists()){
            musicDir.mkdir();
            return;
        }
        final File[] files = musicDir.listFiles();
        Log.d(TAG, "Files length: === " + files.length );
        for (File file : files){
            final String path = file.getAbsolutePath();
            if (path.endsWith(".mp3")){
                musicFileList.add(path);
            }
        }
    }

    private void fillMusicList(){
        musicFileList.clear();
        addMusicFileFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC
        )));
        addMusicFileFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
        )));
    }

    private MediaPlayer mp;

    private int playMusicFile(String path){
        mp = new MediaPlayer();
        try {
            mp.setDataSource(path);
            mp.prepare();
            mp.start();
        }catch (Exception e){
            e.printStackTrace();
        }
        return mp.getDuration();
    }

    private int songPosition;
    private volatile boolean isSongPlaying;
    private TextView songPositionTextView;
    private TextView songDurationTextView;
    private View playBackControls;
    private SeekBar seekBar;
    private Button pauseButton;
    private int mPosition;

    private void playMusic(){
        final String musicPath = musicFileList.get(mPosition);
        final int musicDuration = playMusicFile(musicPath)/1000;
        seekBar.setMax(musicDuration);
        seekBar.setVisibility(View.VISIBLE);
        playBackControls.setVisibility(View.VISIBLE);
        songDurationTextView.setText(String.valueOf(musicDuration/60)+ ":" + String.valueOf(musicDuration%60));
        new Thread(){
            public void run() {
                isSongPlaying = true;
                songPosition = 0;
                while (songPosition < musicDuration){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (isSongPlaying){
                        songPosition++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                seekBar.setProgress(songPosition);
                                songPositionTextView.setText(String.valueOf(songPosition/60) + ":" + String.valueOf(songPosition%60));
                            }
                        });
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mp.pause();
                        songPosition = 0;
                        songPositionTextView.setText("0");
                        mp.seekTo(songPosition);
                        pauseButton.setText("Play");
                        isSongPlaying = false;
                        seekBar.setProgress(songPosition);
                    }
                });
               
            }
        }.start();
    }

    //We request the permissions
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M  && arePermissionsGranted()){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if (!isMusicPlayerInitialized){
            final ListView listView = findViewById(R.id.listview);
            final TextAdapter textAdapter = new TextAdapter();
            musicFileList = new ArrayList<>();
            fillMusicList();
            seekBar = findViewById(R.id.seekBar);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int songProgress;
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    songProgress = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    songPosition = songProgress;
                    mp.seekTo(songProgress);
                }
            });
            textAdapter.setData(musicFileList);
            listView.setAdapter(textAdapter);

            songPositionTextView = findViewById(R.id.currentPosition);
            songDurationTextView = findViewById(R.id.songDuration);

            pauseButton = findViewById(R.id.pauseButton);

            playBackControls = findViewById(R.id.playBackControls);

            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isSongPlaying){
                        mp.pause();
                        pauseButton.setText("Play");
                    }else {
                        if (songPosition == 0){
                            playMusic();
                        }else {
                            mp.start();
                        }
                        pauseButton.setText("Pause");
                    }
                    isSongPlaying = !isSongPlaying;
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mPosition = position;
                    playMusic();
                }
            });
            isMusicPlayerInitialized = true;
        }
    }

    class TextAdapter extends BaseAdapter{
        private List<String> data = new ArrayList<>();

        void setData(List<String> mData){
            data.clear();
            data.addAll(mData);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.myItem)));
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = data.get(position);
            holder.info.setText(item.substring(item.lastIndexOf('/') + 1));
            return convertView;
        }

        class ViewHolder{
            TextView info;

            ViewHolder(TextView mInfo){
                this.info = mInfo;
            }
        }
    }
}