/*
 * Copyright 2015 Synced Synapse. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.ui.sections.audio;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.loader.content.CursorLoader;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.RecyclerViewCursorAdapter;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * Fragment that presents the album genres list
 */
public class AudioGenresListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(AudioGenresListFragment.class);

    public interface OnAudioGenreSelectedListener {
        void onAudioGenreSelected(int genreId, String genreTitle);
    }

    // Activity listener
    private OnAudioGenreSelectedListener listenerActivity;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

    @Override
    protected void onListItemClicked(View view) {
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onAudioGenreSelected(tag.genreId, tag.genreTitle);
    }

    @Override
    protected String getEmptyResultsTitle() { return getString(R.string.no_genres_found_refresh); }

    @Override
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new AudioGenresAdapter(getActivity());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
        Uri uri = MediaContract.AudioGenres.buildAudioGenresListUri(hostInfo != null ? hostInfo.getId() : -1);

        String selection = null;
        String[] selectionArgs = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaContract.AudioGenres.TITLE + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        return new CursorLoader(requireContext(), uri,
                AudioGenreListQuery.PROJECTION, selection, selectionArgs, AudioGenreListQuery.SORT);
    }

    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        try {
            listenerActivity = (OnAudioGenreSelectedListener) ctx;
        } catch (ClassCastException e) {
            throw new ClassCastException(ctx + " must implement OnAudioGenreSelectedListener");
        }
        setSupportsSearch(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    /**
     * Audio genres list query parameters.
     */
    public interface AudioGenreListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.AudioGenres.GENREID,
                MediaContract.AudioGenres.TITLE,
                MediaContract.AudioGenres.THUMBNAIL,
        };

        String SORT = MediaContract.AudioGenres.TITLE + " COLLATE NOCASE ASC";

        int ID = 0;
        int GENREID = 1;
        int TITLE = 2;
        int THUMBNAIL = 3;
    }

    private class AudioGenresAdapter extends RecyclerViewCursorAdapter {

        private final HostManager hostManager;
        private final int artWidth, artHeight;

        public AudioGenresAdapter(Context context) {
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.audiogenrelist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.audiogenrelist_art_heigth) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
        }

        @NonNull
        @Override
        public CursorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext())
                                      .inflate(R.layout.item_music_generic, parent, false);

            return new ViewHolder(view, getContext(), hostManager, artWidth, artHeight, genrelistItemMenuClickListener);
        }

        protected int getSectionColumnIdx() { return AudioGenreListQuery.TITLE; }
    }

    /**
     * View holder pattern
     */
    private static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView titleView;
        TextView detailsView;
        TextView otherInfoView;
        ImageView artView;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;
        int genreId;
        String genreTitle;

        ViewHolder(View itemView, Context context, HostManager hostManager, int artWidth, int artHeight,
                   View.OnClickListener contextMenuClickListener) {
            super(itemView);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            otherInfoView = itemView.findViewById(R.id.other_info);
            artView = itemView.findViewById(R.id.art);

            ImageView contextMenu = itemView.findViewById(R.id.list_context_menu);
            contextMenu.setTag(this);
            contextMenu.setOnClickListener(contextMenuClickListener);
        }

        @Override
        public void bindView(Cursor cursor) {
            genreId = cursor.getInt(AudioGenreListQuery.GENREID);
            genreTitle = cursor.getString(AudioGenreListQuery.TITLE);

            titleView.setText(genreTitle);
            detailsView.setVisibility(View.GONE);
            otherInfoView.setVisibility(View.GONE);
            String thumbnail = cursor.getString(AudioGenreListQuery.THUMBNAIL);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 thumbnail, genreTitle, artView, artWidth, artHeight);


        }
    }

    private final View.OnClickListener genrelistItemMenuClickListener = v -> {
        final ViewHolder viewHolder = (ViewHolder)v.getTag();

        final PlaylistType.Item playListItem = new PlaylistType.Item();
        playListItem.genreid = viewHolder.genreId;

        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_play) {
                MediaPlayerUtils.play(AudioGenresListFragment.this, playListItem);
                return true;
            } else if (itemId == R.id.action_queue) {
                MediaPlayerUtils.queue(AudioGenresListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                return true;
            }
            return false;
        });
        popupMenu.show();
    };
}
