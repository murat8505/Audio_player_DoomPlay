package com.perm.DoomPlay;


/*
 *    Copyright 2013 Vladislav Krot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    You can contact me <DoomPlaye@gmail.com>
 */


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.Arrays;

public class ListTracksActivity extends AbstractLists
{
    public final static String actionJust = "actionPlayFull";
    public final static String actionPlaylist = "actionPlaylist";
    ActionMode actionMode;
    static String currentAction;

    public void checkDeletedTracks()
    {
        String[] tracks = playlistDB.getTracks(PlaylistActivity.selectedPlaylist);
        for(int i = 0 ; i < tracks.length ; i++)
        {
            if(!new File(tracks[i]).exists())
                playlistDB.deleteTrack(i,PlaylistActivity.selectedPlaylist);
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_tracks);

        getTracksFromIntent();

        initialize();
        initializeAbstract();
        checkIsShown(savedInstanceState);

    }

    void getTracksFromIntent()
    {
        tracks = getIntent().getStringArrayExtra(MainScreenActivity.keyOpenInListTrack);
        if(tracks == null)
            tracks = TracksHolder.songAllPath;
    }

    protected AdapterView.OnItemLongClickListener onItemLongTrackClick = new AdapterView.OnItemLongClickListener()
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
        {
            if(!AddTrackToAlbumDialog.isAdding)
            {
                actionMode = startSupportActionMode(callback);
                actionMode.setTag(position);
                return true;
            }
            return false;

        }
    };

    protected ActionMode.Callback callback = new ActionMode.Callback()
    {
        int position;
        boolean isFirstCall;
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            if(currentAction.equals(actionPlaylist))
                getMenuInflater().inflate(R.menu.action_edit,menu);

            else
                getMenuInflater().inflate(R.menu.action_option,menu);
            isFirstCall = true;
            return true;

        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu){return false;}


        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            if(isFirstCall)
            {
                position = (Integer)mode.getTag();
                isFirstCall = false;
            }
            if(currentAction.equals(actionPlaylist))
            {
                switch (item.getItemId())
                {

                    case R.id.itemDeleteTrack:
                    {
                        trackDelete(position);

                        mode.finish();
                        break;
                    }
                    case R.id.itemTrackDown:
                    {
                        trackChange(false, position);

                        if(position == tracks.length - 1)
                            position = 0;
                        else
                            position++;

                        updateList();
                        break;
                    }
                    case R.id.itemTrackUp:
                    {
                        trackChange(true, position);
                        if(position == 0)
                            position = tracks.length - 1;
                        else
                            position--;

                        updateList();
                        break;
                    }
                    case R.id.itemGetLiricks:
                        LyricsDialog dialog = new LyricsDialog();
                        Bundle bundle = new Bundle();
                        bundle.putString(LyricsDialog.keyLyricsTitle,new Song(tracks[position]).getTitle());
                        dialog.setArguments(bundle);
                        dialog.show(getSupportFragmentManager(),"tag");
                        break;
                }
            }
            else
            {
                switch(item.getItemId())
                {
                    case R.id.itemToPlaylist:
                        FileSystemActivity.showPlaybackDialog(new String[]{tracks[position]},getSupportFragmentManager());
                        break;
                    case R.id.itemSetAsRingtone:
                        Utils.setRingtone(getBaseContext(), tracks[(Integer) mode.getTag()]);
                        break;
                    case R.id.itemGetLiricks:
                        Song song = new Song(tracks[position]);
                        startLiryctDialog(getSupportFragmentManager(),song.getArtist(),song.getTitle());
                        break;
                }
                mode.finish();
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode){}
    };

    static void startLiryctDialog(FragmentManager fragmentManager,String artist,String title)
    {
        LyricsDialog dialog = new LyricsDialog();
        Bundle bundle = new Bundle();
        bundle.putString(LyricsDialog.keyLyricsTitle,artist+" "+title);
        dialog.setArguments(bundle);
        dialog.show(fragmentManager,"tag");
    }

    static boolean isEquals = false;

    void updateList()
    {
        isEquals = Arrays.equals(PlayingService.tracks,tracks);
        tracks = playlistDB.getTracks(PlaylistActivity.selectedPlaylist);

        if(isEquals)
        {
            PlayingService.tracks = tracks;
            markItem(PlayingService.indexCurrentTrack, false);
        }

        adapter.changeData(tracks);

    }

    void trackDelete(int position)
    {
        playlistDB.deleteTrack(position, PlaylistActivity.selectedPlaylist);
        updateList();
        AsyncTask<Integer,Void,Void> asyncAdder  = new AsyncTask<Integer, Void, Void>()
        {
            @Override
            protected Void doInBackground(Integer... params)
            {
                AddTrackToAlbumDialog.isAdding = true;
                playlistDB.setAcordingPositions(params[0],PlaylistActivity.selectedPlaylist);
                AddTrackToAlbumDialog.isAdding = false;
                return null;
            }
        };
        asyncAdder.execute(position);

        if(position == PlayingService.indexCurrentTrack && Arrays.equals(PlayingService.tracks,tracks))
        {
            playingService.playTrackFromList(PlayingService.indexCurrentTrack);
        }
    }

    void trackChange(boolean up, int position)
    {
        int to ;

        if(up)
        {
            if(position == 0)
            {
                to  = tracks.length - 1;
            }
            else
            {
                to = position - 1;
            }
        }
        else
        {
            if(position == tracks.length-1)
            {
                to = 0;
            }
            else
            {
                to = position + 1 ;
            }
        }
        if(Arrays.equals(PlayingService.tracks,tracks))
        {
            if(PlayingService.indexCurrentTrack == to)
                PlayingService.indexCurrentTrack = position;

            else if(PlayingService.indexCurrentTrack == position)
                PlayingService.indexCurrentTrack = to;
        }



        playlistDB.changeColumns(PlaylistActivity.selectedPlaylist,position,to);
    }
    @Override
    protected void onResume()
    {
        super.onResume();

        if(currentAction.equals(actionPlaylist))
        {
            checkDeletedTracks();
            updateList();
        }

    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        setIntent(intent);
        String[] tempTracks = intent.getStringArrayExtra(MainScreenActivity.keyOpenInListTrack);

        if(tempTracks == null && tracks == null)
            tracks = TracksHolder.songAllPath;
        else if(tempTracks != null)
            tracks = tempTracks;

        adapter.changeData(tracks);
        currentAction  = intent.getAction();
    }




    private void initialize()
    {
        listView = (ListView)findViewById(R.id.listAllSongs);
        playlistDB = PlaylistDB.getInstance(this);
        intentService = new Intent(this,PlayingService.class);
        intentService.setAction(PlayingService.actionOffline);
        intentService.putExtra(FullPlaybackActivity.keyService, tracks);
        adapter = new ListTracksAdapter(tracks,this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(onItemTrackClick);
        listView.setOnItemLongClickListener(onItemLongTrackClick);


        imgPlay = (ImageView)findViewById(R.id.imagePlay);
        imgShuffle = (ImageView)findViewById(R.id.imageShuffle);
        imgRepeat = (ImageView)findViewById(R.id.imageRepeat);
        imgNext = (ImageView)findViewById(R.id.imageNext);
        imgPrevious = (ImageView)findViewById(R.id.imagePrevious);

        seekBar = (SeekBar)findViewById(R.id.seek_bar);
        textCurrentTime = (TextView)findViewById(R.id.textElapsed);
        textTotalTime = (TextView)findViewById(R.id.textDuration);
        linearControls = (RelativeLayout)findViewById(R.id.linearControls);

        currentAction = getIntent().getAction();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(!currentAction.equals(actionPlaylist))
        {
            if(!MainScreenActivity.isOldSDK)
                getMenuInflater().inflate(R.menu.bar_list,menu);
            else
                getMenuInflater().inflate(R.menu.bar_list_old,menu);
        }
        else
        {   if(!MainScreenActivity.isOldSDK)
                getMenuInflater().inflate(R.menu.bar_list_edit,menu);
            else
                getMenuInflater().inflate(R.menu.bar_edit_old,menu);
        }
        return true;
    }

    public static String getReadableName(String track)
    {
        return new File(track).getName().substring(0, new File(track).getName().length() - 4);
    }
}
