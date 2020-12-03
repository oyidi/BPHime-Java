package com.windworkshop.bphime.activity;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.windworkshop.bphime.R;
import com.windworkshop.bphime.database.LiveHistoryDatabase;
import com.windworkshop.bphime.object.DanmuItem;

import java.util.ArrayList;

public class GiftStatisticsActivity extends AppCompatActivity {
    DanmuListAdapter adapter;
    RecyclerView listView;
    ArrayList<DanmuItem> historyDanmu = new ArrayList<DanmuItem>();
    LiveHistoryDatabase historyData;

    int pastVisiblesItems, visibleItemCount, totalItemCount;
    LinearLayoutManager linearLayoutManager;
    private boolean loading = true;
    boolean isFinalPage = false;
    Toolbar toolbar;
    int page = 0;

    TextView totalStatisticsText;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_gift_activity);

        toolbar = findViewById(R.id.main_normal_toolbar);
        toolbar.setTitle("礼物记录");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        totalStatisticsText = findViewById(R.id.gift_statistics_text);

        adapter = new DanmuListAdapter(getApplicationContext(), historyDanmu);
        adapter.setShowSendingTime(true);
        listView = findViewById(R.id.history_gift_list);
        listView.setAdapter(adapter);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext()){
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        };
        listView.setLayoutManager(linearLayoutManager);
        historyData = new LiveHistoryDatabase(getApplicationContext());

        listView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if(dy > 0) {
                    visibleItemCount = linearLayoutManager.getChildCount();
                    totalItemCount = linearLayoutManager.getItemCount();
                    pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition();
                    if (loading) {
                        if ( (visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                            if(isFinalPage == false) {
                                loading = false;
                                page += 1;
                                loadHistoryDanmu();
                            }
                        }
                    }
                }

            }
        });
        loadHistoryDanmu();
        ArrayList<DanmuItem> staticsDanmus = historyData.groupGiftHistory();

    }
    private void loadHistoryDanmu() {
        loading = true;

        ArrayList<DanmuItem> danmus = historyData.loadGiftHistory(page);
        if(danmus.size() > 0){
            historyDanmu.addAll(danmus);
            adapter.notifyDataSetChanged();
        } else {
            isFinalPage = true;
        }

        loading = false;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish(); break;
        }
        return super.onOptionsItemSelected(item);
    }
}