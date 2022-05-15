package org.funix.lab_6;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicHolder> {

    private final ArrayList<SongEntity> listSong;

    private final Context mContext;

    public MusicAdapter(ArrayList<SongEntity> listSong, Context mContext) {
        this.listSong = listSong;
        this.mContext = mContext;
    }

    public class MusicHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        View view;

        public MusicHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;

            tvName = itemView.findViewById(R.id.tv_song);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.startAnimation(AnimationUtils.loadAnimation(mContext, androidx.appcompat.R.anim.abc_fade_in));

                    ((MainActivity) mContext).playSong((SongEntity) tvName.getTag());

//                    view.setBackgroundColor(Color.GREEN);
                }
            });

        }
    }

    @NonNull
    @Override
    public MusicAdapter.MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(mContext).inflate(R.layout.item_song, parent, false);

        return new MusicHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicAdapter.MusicHolder holder, int position) {

        SongEntity item = listSong.get(position);

        holder.tvName.setText(item.getName());

        holder.tvName.setTag(item);

//        if (position == 0) {
//            holder.view.setBackgroundColor(Color.GREEN);
//        }

    }


    @Override
    public int getItemCount() {
        return listSong.size();
    }
}
