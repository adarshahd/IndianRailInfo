/**
 *   Copyright (C) 2013  Adarsha HD
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
package com.adarshahd.indianrailinfo.donate;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;

/**
 * Created by ahd on 6/26/13.
 */
public class PNRTrackList extends SherlockListActivity implements AdapterView.OnItemClickListener {
    private static ListView mListView;
    private static ArrayList<String> mListPNR;
    private static ActionMode mMode;
    private static String [] pnrsSelected;
    private static ArrayAdapter<String> mAdapter;
    private static boolean animationOver;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_list_pnr_track);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("PNR Tracking List");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mListView = getListView();
        mListPNR = (ArrayList<String>) PNRDatabase.getPNRDatabase(this).getPNRTrackList();
        mAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,0,mListPNR);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        pnrsSelected = new String[mListPNR.size()];
        animationOver = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        /*if(mMode == null) {
            //Toast.makeText(this,"Creating action mode",Toast.LENGTH_SHORT).show();
            mMode = startActionMode(new CallBack());
        }*/
        /* Weired! the below code doesn't work on Gingerbread. Probably setItemChecked() is fired at a later stage.
        if(((CheckedTextView)view).isChecked()) {
            pnrsSelected[position] = mListView.getItemAtPosition(position).toString();
            view.setBackgroundColor(Color.argb(0xFF,0x33,0xB5,0xE5));
        } else {
            pnrsSelected[position] = null;
            view.setBackgroundColor(Color.TRANSPARENT);
        }*/
        boolean noItem = true;
        SparseBooleanArray array = new SparseBooleanArray();
        array.clear();
        array = mListView.getCheckedItemPositions();
        for(int i=0; i < mListView.getCount(); ++i) {
            if (mListView.getChildAt(i) == null) {
                continue;
            }
            if(array.get(i) == true) {
                noItem = false;
                pnrsSelected[i] = mListView.getItemAtPosition(i).toString();
            } else {
                pnrsSelected[i] = null;
            }
        }
        if(noItem) {
            if(mMode != null ) {
                mMode.finish();
            }
        } else {
            if(mMode == null) {
                mMode = startActionMode(new CallBack());
            }
        }
    }

    private final class CallBack implements ActionMode.Callback {

        @Override
        public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            for(int i=0;i<mListView.getCount();++i) {
                try {
                    ((CheckedTextView) mListView.getChildAt(i)).setChecked(false);
                } catch (NullPointerException e) {
                }
            }
            mMode = null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode arg0, Menu arg1) {
            getSupportMenuInflater().inflate(R.menu.menu_pnr_track_list, arg1);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode arg0,
                                           MenuItem arg1) {
            switch(arg1.getItemId()) {
                case R.id.id_menu_delete_pnr_track:
                    ArrayList<String> list = new ArrayList<String>();
                    ArrayList<Integer> deleteList = new ArrayList<Integer>();
                    for(int i=0;i<pnrsSelected.length;++i) {
                        if(pnrsSelected[i] == null){
                            continue;
                        }
                        list.add(pnrsSelected[i]);
                        deleteList.add(i);
                    }
                    stopPNRTracking(list);
                    animationOver = false;
                    arg0.finish();
                    animateDeletion(deleteList);

                    return true;
                default:
                    arg0.finish();
                    return true;
            }
        }
    }

    private void stopPNRTracking(ArrayList<String> list) {
        PNRDatabase.getPNRDatabase(this).stopTrackingPNRs(list);
    }

    private void animateDeletion(final ArrayList<Integer> ids) {
        final int initialHeight = mListView.getChildAt(ids.get(0)).getMeasuredHeight();
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                for(int id : ids) {
                    ((CheckedTextView)mListView.getChildAt(id)).setChecked(false);
                    mListPNR.remove(pnrsSelected[id]);
                    if(mListPNR.size() == 0) {
                        finish();
                    }
                }
                //
                if((Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1)) {
                    for(int i=0;i<mListView.getCount();++i) {
                        try {
                            ((CheckedTextView) mListView.getChildAt(i)).setChecked(false);
                            //mListView.getChildAt(i).setActivated(false);
                        } catch (NullPointerException e) {
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                    mListView.invalidate();
                    pnrsSelected = null;
                    pnrsSelected = new String[mListPNR.size()];

                    return;
                }
                mAdapter = new ArrayAdapter<String>(PNRTrackList.this,android.R.layout.simple_list_item_multiple_choice,0,mListPNR);
                mListView.setAdapter(mAdapter);
                pnrsSelected = null;
                pnrsSelected = new String[mListPNR.size()];
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(final float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1 && !animationOver) {
                    for (int id : ids) {
                        //mListView.getChildAt(id).getLayoutParams().height = 0;
                        mListView.getChildAt(id).setVisibility(View.GONE);
                    }
                    animationOver = true;
                } else {
                    if (!animationOver) {
                        for (int id : ids) {
                            mListView.getChildAt(id).getLayoutParams().height = (int) (initialHeight - (initialHeight*interpolatedTime));
                            mListView.getChildAt(id).requestLayout();
                        }
                    }
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setAnimationListener(listener);
        animation.setDuration(200);
        for (int id : ids) {
            try {
                mListView.getChildAt(id).startAnimation(animation);
            } catch (Exception e) {
                Log.i("PNRTrackList: ","Error starting animation for child: " + id);
            }
        }
    }
}