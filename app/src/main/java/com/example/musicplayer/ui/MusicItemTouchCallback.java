package com.example.musicplayer.ui;

import android.graphics.Color;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.MusicApplication;
import com.example.musicplayer.sqlite.OperationDB;
import com.example.musicplayer.ui.MusicRecyclerViewAdapter.MusicViewHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class MusicItemTouchCallback extends ItemTouchHelper.Callback {

    private final MusicRecyclerViewAdapter adapter;
    private final OperationDB operationDB = OperationDB.getInstance();
    private Messenger messenger; //用于控件位置改变的信息传递

    public MusicItemTouchCallback(MusicRecyclerViewAdapter adapter) {
        this.adapter = adapter;
    }

    public void setMessenger(Messenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, @NotNull RecyclerView.ViewHolder viewHolder) { //获取运动标志
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
        int fromPosition = viewHolder.getAdapterPosition(); //控件长按移动,修改控件位置
        int toPosition = target.getAdapterPosition();
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(adapter.getValues(), i, i + 1);
            }
        }
        else for (int i = fromPosition; i > toPosition; i--) {
            Collections.swap(adapter.getValues(), i, i - 1);
        }
        operationDB.swap(((MusicViewHolder) viewHolder).mItem.getId(), ((MusicViewHolder) target).mItem.getId());
        adapter.notifyItemMoved(fromPosition, toPosition); //适配器更新
        try {
            Message message = Message.obtain(); //发送位置交换信息
            message.what = MusicApplication.SWAP_CODE;
            message.arg1 = fromPosition;
            message.arg2 = toPosition;
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition(); //左右刷动删除控件
        if (direction == ItemTouchHelper.END | direction == ItemTouchHelper.START) {
            adapter.getValues().remove(position);
            adapter.notifyItemRemoved(position);
            operationDB.delete(((MusicRecyclerViewAdapter.MusicViewHolder) viewHolder).mItem.getId());
        }
        try {
            Message message = Message.obtain(); //发送删除信息
            message.what = MusicApplication.DELETE_CODE;
            message.arg1 = position;
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState); //控件长按颜色改变
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
