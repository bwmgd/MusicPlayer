package com.example.musicplayer.ui;

import android.graphics.Color;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.sqlite.OperationDB;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;

public class MyItemTouchCallback extends ItemTouchHelper.Callback {

    private final MusicRecyclerViewAdapter adapter;
    private final OperationDB operationDB = OperationDB.getInstance();

    public MyItemTouchCallback(MusicRecyclerViewAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, @NotNull RecyclerView.ViewHolder viewHolder) {
        int dragFlag;
        int swipeFlag;
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            dragFlag = ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            swipeFlag = 0;
        }
        else {
            dragFlag = ItemTouchHelper.DOWN | ItemTouchHelper.UP;
            swipeFlag = ItemTouchHelper.END | ItemTouchHelper.START;
        }

        return makeMovementFlags(dragFlag, swipeFlag);
    }

    @Override
    public boolean onMove(@NotNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(adapter.getValues(), i, i + 1);
            }
        }
        else for (int i = fromPosition; i > toPosition; i--) {
            Collections.swap(adapter.getValues(), i, i - 1);
        }
        Objects.requireNonNull(recyclerView.getAdapter()).notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (direction == ItemTouchHelper.END | direction == ItemTouchHelper.START) {
            adapter.getValues().remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, adapter.getItemCount());
            operationDB.delete(((MusicRecyclerViewAdapter.MusicViewHolder) viewHolder).mItem.getId());
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder.itemView.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public void clearView(@NotNull RecyclerView recyclerView, @NotNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setBackgroundColor(0);
    }
}
