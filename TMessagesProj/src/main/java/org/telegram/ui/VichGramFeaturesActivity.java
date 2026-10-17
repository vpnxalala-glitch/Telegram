package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VichGramPrefs;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * VichGram: Settings → Функции
 * Structure: section header → toggle button → explanation.
 */
public class VichGramFeaturesActivity extends BaseFragment {

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_CHECK = 1;
    private static final int VIEW_INFO = 2;

    private static final int ID_HEADER_WATCH = 1;
    private static final int ID_DELETED = 2;
    private static final int ID_DELETED_INFO = 3;
    private static final int ID_EDITS = 4;
    private static final int ID_EDITS_INFO = 5;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<Item> items = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Функции");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            Item item = items.get(position);
            if (item.id == ID_DELETED) {
                boolean next = !VichGramPrefs.isWatchDeletedMessages();
                VichGramPrefs.setWatchDeletedMessages(next);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(next);
                }
            } else if (item.id == ID_EDITS) {
                boolean next = !VichGramPrefs.isWatchMessageEdits();
                VichGramPrefs.setWatchMessageEdits(next);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(next);
                }
            }
        });

        rebuildItems();
        return fragmentView;
    }

    private void rebuildItems() {
        items.clear();
        // Раздел
        items.add(new Item(VIEW_HEADER, ID_HEADER_WATCH, "Смотреть"));
        // Кнопка + пояснение
        items.add(new Item(VIEW_CHECK, ID_DELETED, "Удалённые сообщения"));
        items.add(new Item(VIEW_INFO, ID_DELETED_INFO,
                "Если включено, VichGram будет сохранять и показывать сообщения, которые собеседник удалил."));
        items.add(new Item(VIEW_CHECK, ID_EDITS, "Изменения сообщений"));
        items.add(new Item(VIEW_INFO, ID_EDITS_INFO,
                "Если включено, VichGram будет показывать, как сообщение выглядело до редактирования."));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static class Item {
        final int viewType;
        final int id;
        final CharSequence text;

        Item(int viewType, int id, CharSequence text) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == VIEW_CHECK;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_HEADER) {
                view = new HeaderCell(getContext());
            } else if (viewType == VIEW_CHECK) {
                view = new TextCheckCell(getContext());
            } else {
                view = new TextInfoPrivacyCell(getContext());
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Item item = items.get(position);
            if (holder.getItemViewType() == VIEW_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.getItemViewType() == VIEW_INFO) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(item.text);
            } else if (holder.getItemViewType() == VIEW_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                boolean checked = false;
                if (item.id == ID_DELETED) {
                    checked = VichGramPrefs.isWatchDeletedMessages();
                } else if (item.id == ID_EDITS) {
                    checked = VichGramPrefs.isWatchMessageEdits();
                }
                boolean divider = position + 1 < items.size() && items.get(position + 1).viewType == VIEW_CHECK;
                cell.setTextAndCheck(item.text.toString(), checked, divider);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        if (listView != null) {
            listView.setPadding(0, 0, 0, bottom);
            listView.setClipToPadding(false);
        }
    }
}
