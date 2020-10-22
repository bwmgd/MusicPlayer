package com.example.musicplayer.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.musicplayer.R;
import com.example.musicplayer.domain.MusicContent;
import com.example.musicplayer.sqlite.OperationDB;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 */
public class MusicFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;

    private final OperationDB operationDB = OperationDB.getInstance();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MusicFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static MusicFragment newInstance(int columnCount) {
        MusicFragment fragment = new MusicFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    private MusicRecyclerViewAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.music_fragment_item_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), OrientationHelper.VERTICAL));
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            }
            else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            //从数据库获取音乐
            ArrayList<MusicContent.Music> items = operationDB.getAllMusic();
            adapter = new MusicRecyclerViewAdapter(items);
            recyclerView.setAdapter(adapter);

        }
        return view;
    }

    //添加音乐
    public void addItem(MusicContent.Music music){
        adapter.addItem(music);
        operationDB.insert(music);
    }

    //清空列表
    public void clearItem(){
        adapter.clearItem();
        operationDB.clear();
    }
}