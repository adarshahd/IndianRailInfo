package com.adarshahd.indianrailinfo.donate.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.adarshahd.indianrailinfo.donate.R;
import com.adarshahd.indianrailinfo.donate.utilities.Utility;

import java.util.ArrayList;


public class PNRTrackingListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static ListView mListView;
    private static ArrayList<String> mListPNR;
    private static ActionMode mMode;
    private static String [] pnrSelected;
    private static ArrayAdapter<String> mAdapter;
    private static boolean animationOver;
    private Toolbar mToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_pnrtracking_list);
        mToolBar = (Toolbar) findViewById(R.id.appBar);
        setSupportActionBar(mToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mListView = (ListView) findViewById(R.id.pnr_list);
        mListPNR = Utility.getTrackedPNRs(this);
        mAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_multiple_choice,0,mListPNR);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        pnrSelected = new String[mListPNR.size()];
        animationOver = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int j, long l) {
        boolean noItem = true;
        SparseBooleanArray array = new SparseBooleanArray();
        array.clear();
        array = mListView.getCheckedItemPositions();
        for(int i=0; i < mListView.getCount(); ++i) {
            if (mListView.getChildAt(i) == null) {
                continue;
            }
            if(array.get(i)) {
                noItem = false;
                pnrSelected[i] = mListView.getItemAtPosition(i).toString();
            } else {
                pnrSelected[i] = null;
            }
        }
        if(noItem) {
            if(mMode != null ) {
                mMode.finish();
            }
        } else {
            if(mMode == null) {
                mMode = mToolBar.startActionMode(new CallBack());
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
                    e.printStackTrace();
                }
            }
            mMode = null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode arg0, Menu arg1) {
            getMenuInflater().inflate(R.menu.menu_pnr_track_list, arg1);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode arg0,
                                           MenuItem arg1) {
            switch(arg1.getItemId()) {
                case R.id.id_menu_delete_pnr_track:
                    ArrayList<String> list = new ArrayList<String>();
                    ArrayList<Integer> deleteList = new ArrayList<Integer>();
                    for(int i=0;i< pnrSelected.length;++i) {
                        if(pnrSelected[i] == null){
                            continue;
                        }
                        list.add(pnrSelected[i]);
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
        Utility.unTrackPNRs(this,list);
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
                    mListPNR.remove(pnrSelected[id]);
                    if(mListPNR.size() == 0) {
                        finish();
                    }
                }

                mAdapter = new ArrayAdapter<>(PNRTrackingListActivity.this,android.R.layout.simple_list_item_multiple_choice,0,mListPNR);
                mListView.setAdapter(mAdapter);
                pnrSelected = null;
                pnrSelected = new String[mListPNR.size()];
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
                Log.e("PNRTrackList: ", "Error starting animation for child: " + id);
            }
        }
    }
}
