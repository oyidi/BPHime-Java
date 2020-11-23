package com.windworkshop.bphime;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

public class HistoryDanmuActivity extends AppCompatActivity {

    DanmuListAdapter adapter;
    RecyclerView listView;
    ArrayList<DanmuItem> historyDanmu = new ArrayList<DanmuItem>();
    HistoryData historyData;

    int pastVisiblesItems, visibleItemCount, totalItemCount;
    LinearLayoutManager linearLayoutManager;
    private boolean loading = true;
    boolean isFinalPage = false;

    int page = 0;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_danmu_activity);
        adapter = new DanmuListAdapter(getApplicationContext(), historyDanmu);
        listView = findViewById(R.id.history_danmu_list);
        listView.setAdapter(adapter);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext()){
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        };
        listView.setLayoutManager(linearLayoutManager);
        historyData = new HistoryData(getApplicationContext());

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
    }
    private void loadHistoryDanmu() {
        loading = true;

        ArrayList<DanmuItem> danmus = historyData.loadHistory(page);
        if(danmus.size() > 0){
            historyDanmu.addAll(danmus);
            adapter.notifyDataSetChanged();
        } else {
            isFinalPage = true;
        }

        loading = false;
    }
}
