package org.funix.lab_6;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

//    private static final int LEVEL_PAUSE = 0;
//    private static final int LEVEL_PLAY = 1;
    private static final MediaPlayer player = new MediaPlayer();
    private static final int STATE_IDE = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSED = 3;

    private final ArrayList<SongEntity> listSong = new ArrayList<>();

    private TextView tvName, tvAlbum, tvTime;
    private SeekBar seekBar;
    private ImageView ivPlay;

    private int index;
    private SongEntity songEntity;
    private Thread thread;
    private int state = STATE_IDE;
    private String totalTime;
    private RecyclerView rv;
    private LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews() {

        ivPlay = findViewById(R.id.iv_play);
        ivPlay.setOnClickListener(this);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_next).setOnClickListener(this);
        tvName = findViewById(R.id.tv_name);
        tvAlbum = findViewById(R.id.tv_album);
        tvTime = findViewById(R.id.tv_time);
        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);

        linearLayoutManager = new LinearLayoutManager(this);

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                next();
            }
        });

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);

            return;
        }

        loadingListSongOffline();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RESULT_OK) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                loadingListSongOffline();
            }
        } else {
            Toast.makeText(this, R.string.txt_alert, Toast.LENGTH_SHORT).show();

            finish();
        }
    }

    /**
     * l???y danh s??ch b??i h??t t??? b??? nh??? g??n cho list
     * g??n list cho adapter c???a RecyclerView ????? hi???n th??? danh s??ch b??i h??t
     */
    @SuppressLint("Range")
    private void loadingListSongOffline() {

        //ContentResolver cho ph??p truy c???p ?????n t??i nguy??n c???a ???ng d???ng th??ng qua 1 ???????ng d???n uri
        Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, null);

        if (c != null) {

            c.moveToFirst();
            listSong.clear();

            // l???p ?????n khi con tr??? ch??a ?????n h??ng ??p cu???i l???y c??c gi?? tr??? c???a b??i h??t th??m v??o list
            while (!c.isAfterLast()) {

                // l???y t??n b??i h??t b???ng l???y String c???a c???t TITLE trong h??ng ??ang duy???t
                @SuppressLint("Range") String name = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));

                // t????ng t??? tr??n
                @SuppressLint("Range") String path = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));

                String album = "N/A";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST));
                }

                listSong.add(new SongEntity(name, path, album));

                c.moveToNext();

            }

            c.close();

            rv = findViewById(R.id.rv_song);
            rv.setLayoutManager(linearLayoutManager);
            rv.setAdapter(new MusicAdapter(listSong, this));

            if (listSong.size() == 0) {
                Toast.makeText(this, "kh??ng c?? b??i h??t trong b??? nh???", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            play();

            playPause();

        }
    }


    /**
     * n???u b??i h??t ??ang ch???y th?? d???ng v?? ng?????c l???i
     */
    private void playPause() {
        if (state == STATE_PLAYING && player.isPlaying()) {

            player.pause();
            ivPlay.setImageResource(R.drawable.bg_play);
            state = STATE_PAUSED;
        } else if (state == STATE_PAUSED) {

            player.start();
            state = STATE_PLAYING;
            ivPlay.setImageResource(R.drawable.ic_pause);
        } else {
            play();
        }

    }

    /**
     * ch???y nh???c
     */
    private void play() {

        // cho recyclerview cu???n ?????n b??i h??t ??ang ch???y
        rv.smoothScrollToPosition(index);

        songEntity = listSong.get(index);

        if (linearLayoutManager != null) {
            setBackgroundColorRecyclerviewItem();
        }

        tvName.setText(songEntity.getName());

        tvAlbum.setText(songEntity.getAlbum());

        player.reset();

        try {
            player.setDataSource(songEntity.getPath());
            player.prepare();
            player.start();

            ivPlay.setImageResource(R.drawable.ic_pause);

            state = STATE_PLAYING;

            // tr??? v??? t???ng th???i gian b??i h??t d???ng String
            totalTime = getTime(player.getDuration());

            // cho ti???n tr??nh t???i ??a c???a seekBar ????ng b???ng t???ng th???i gian c???a b??i h??t
            seekBar.setMax(player.getDuration());

            if (thread == null) {
                startLooping();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * t???o lu???ng ?????ng b??? th???i gian b??i h??t v?? thanh seekBar
     */
    private void startLooping() {

        thread = new Thread(){
            @Override
            public void run() {

                // th???c hi???n sau m???i 2s
                while (true) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        return;
                    }

                    // ch???y b???ng uiThread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateTime();
                        }
                     });
                }
            }
        };

        thread.start();
    }

    /**
     * n???u b??i h??t ??ang ch???y ho???c ??ang d???ng
     * set gi?? tr??? cho tvTime th???i gian ??ang ch???y/ t???ng th???i gian
     * set gi?? tr??? cho seekBar = th???i gian b??i h??t ??ang ch???y
     */
    private void updateTime() {

        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            int time = player.getCurrentPosition();

            tvTime.setText(String.format("%s/%s", getTime(time), totalTime));

            seekBar.setProgress(time);
        }
    }

    /**
     * fomat th???i gian
     * @param duration th???i gian b??i h??t
     * @return String ???? ???????c ?????nh d???ng
     */
    @SuppressLint("SimpleDateFormat")
    private String getTime(int duration) {
        return new SimpleDateFormat("mm:ss").format(new Date(duration));
    }

    /**
     * ch???y b??i h??t truy???n v??o
     * @param tag b??i h??t
     */
    public void playSong(SongEntity tag) {
        index = listSong.indexOf(tag);

//        songEntity = tag;

        play();
    }

    /**
     * x??? l?? s??? ki???n click c??c n??t ??i???u khi???n b??i h??t
     * @param v
     */
    @Override
    public void onClick(View v) {

        // n???u nh???n v??o n??t play th?? g???i h??m ?????i tr???ng th??i ch???y
        if (v.getId() == R.id.iv_play) {

            playPause();

        }
        // nh???n n??t next ho???c back g???i h??m t????ng ???ng
        else if (v.getId() == R.id.iv_next) {
            next();
        } else if (v.getId() == R.id.iv_back) {
            back();
        }

    }

    /**
     * n???u ??ang ch???y b??i h??t c?? index ?????u ti??n th?? g??n index l?? b??i cu???i c??ng trong list
     * ng?????c l???i gi???m index b??i h??t ??i 1 v?? ch??i b??i m???i theo index
     */
    private void back() {

        if (index == 0) {
            index = listSong.size() - 1;
        } else {
            index--;
        }


        play();
    }

    /**
     * n???u index ??ang l?? b??i cu???i c??ng th?? quay l???i b??i ?????u ti??n
     * ng?????c l???i t??ng index l??n 1 v?? ch??i b??i m???i theo index
     */
    private void next() {

        if (index == listSong.size() - 1) {
            index = 0;
        } else {
            index++;
        }

        play();
    }

    /**
     * set m??u n???n cho view item c???a Recyclerview
     */
    private void setBackgroundColorRecyclerviewItem() {

        for (int i = 0; i < listSong.size(); i++) {
            View v = linearLayoutManager.findViewByPosition(i);

            if (v != null) {
                if (v.findViewById(R.id.tv_song).getTag().equals(songEntity)) {
                    v.setBackgroundColor(Color.GREEN);
                } else {
                    v.setBackgroundColor(Color.WHITE);
                }
            }
        }

    }

    /**
     * h???y lu???ng ch???y ng???m
     */
    @Override
    protected void onDestroy() {
        player.release();
        super.onDestroy();
        thread.interrupt();

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    /**
     *  Ph????ng th???c n?? ???????c th???c hi???n khi ng?????i d??ng b???t ?????u thay ?????i ti???n tr??nh c???a thanh SeekBar
     *  cho b??i h??t ch???y theo ti??n tr??nh c???a SeekBar
     * @param seekBar ??ang ???????c l???ng nghe
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            player.seekTo(seekBar.getProgress());
        }
    }
}