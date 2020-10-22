package com.example.musicplayer.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.R;
import com.example.musicplayer.domain.MusicContent.Music;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Music}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MusicRecyclerViewAdapter extends RecyclerView.Adapter<MusicRecyclerViewAdapter.MusicViewHolder> {

    private final List<Music> mValues; //音乐list

    public List<Music> getValues() {
        return mValues;
    }

    public void addItem(Music music) { //添加音乐
        mValues.add(music);
        notifyDataSetChanged();
    }

    public void clearItem() { //清空列表
        mValues.clear();
        notifyDataSetChanged();
    }

    public Music getItem(int position) { //获取音乐
        return mValues.get(position);
    }

    public MusicRecyclerViewAdapter(List<Music> items) {
        mValues = items;
    }

    @NotNull
    @Override
    public MusicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.music_fragment_item, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MusicViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mNameView.setText(mValues.get(position).getName());
        holder.itemView.setClickable(true);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNameView;
        public Music mItem;

        public MusicViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = view.findViewById(R.id.name);
        }

        @NotNull
        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }
}