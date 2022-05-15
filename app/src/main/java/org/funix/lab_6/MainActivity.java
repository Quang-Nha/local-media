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
     * lấy danh sách bài hát từ bộ nhớ gán cho list
     * gán list cho adapter của RecyclerView để hiển thị danh sách bài hát
     */
    @SuppressLint("Range")
    private void loadingListSongOffline() {

        //ContentResolver cho phép truy cập đến tài nguyên của ứng dụng thông qua 1 đường dẫn uri
        Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, null);

        if (c != null) {

            c.moveToFirst();
            listSong.clear();

            // lặp đến khi con trỏ chưa đến hàng áp cuối lấy các giá trị của bài hát thêm vào list
            while (!c.isAfterLast()) {

                // lấy tên bài hát bằng lấy String của cột TITLE trong hàng đang duyệt
                @SuppressLint("Range") String name = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));

                // tương tự trên
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
                Toast.makeText(this, "không có bài hát trong bộ nhớ", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            play();

            playPause();

        }
    }


    /**
     * nếu bài hát đang chạy thì dừng và ngược lại
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
     * chạy nhạc
     */
    private void play() {

        // cho recyclerview cuộn đến bài hát đang chạy
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

            // trả về tổng thời gian bài hát dạng String
            totalTime = getTime(player.getDuration());

            // cho tiến trình tối đa của seekBar đúng bằng tổng thời gian của bài hát
            seekBar.setMax(player.getDuration());

            if (thread == null) {
                startLooping();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * tạo luồng đồng bộ thời gian bài hát và thanh seekBar
     */
    private void startLooping() {

        thread = new Thread(){
            @Override
            public void run() {

                // thực hiện sau mỗi 2s
                while (true) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        return;
                    }

                    // chạy bằng uiThread
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
     * nếu bài hát đang chạy hoặc đang dừng
     * set giá trị cho tvTime thời gian đang chạy/ tổng thời gian
     * set giá trị cho seekBar = thời gian bài hát đang chạy
     */
    private void updateTime() {

        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            int time = player.getCurrentPosition();

            tvTime.setText(String.format("%s/%s", getTime(time), totalTime));

            seekBar.setProgress(time);
        }
    }

    /**
     * fomat thời gian
     * @param duration thời gian bài hát
     * @return String đã được định dạng
     */
    @SuppressLint("SimpleDateFormat")
    private String getTime(int duration) {
        return new SimpleDateFormat("mm:ss").format(new Date(duration));
    }

    /**
     * chạy bài hát truyền vào
     * @param tag bài hát
     */
    public void playSong(SongEntity tag) {
        index = listSong.indexOf(tag);

//        songEntity = tag;

        play();
    }

    /**
     * xử lý sự kiện click các nút điều khiển bài hát
     * @param v
     */
    @Override
    public void onClick(View v) {

        // nếu nhấn vào nút play thì gọi hàm đổi trạng thái chạy
        if (v.getId() == R.id.iv_play) {

            playPause();

        }
        // nhấn nút next hoặc back gọi hàm tương ứng
        else if (v.getId() == R.id.iv_next) {
            next();
        } else if (v.getId() == R.id.iv_back) {
            back();
        }

    }

    /**
     * nếu đang chạy bài hát có index đầu tiên thì gán index là bài cuối cùng trong list
     * ngược lại giảm index bài hát đi 1 và chơi bài mới theo index
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
     * nếu index đang là bài cuối cùng thì quay lại bài đầu tiên
     * ngược lại tăng index lên 1 và chơi bài mới theo index
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
     * set màu nền cho view item của Recyclerview
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
     * hủy luồng chạy ngầm
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
     *  Phương thức nà được thực hiện khi người dùng bắt đầu thay đổi tiến trình của thanh SeekBar
     *  cho bài hát chạy theo tiên trình của SeekBar
     * @param seekBar đang được lắng nghe
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            player.seekTo(seekBar.getProgress());
        }
    }
}