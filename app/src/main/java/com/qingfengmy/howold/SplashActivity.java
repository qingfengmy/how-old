package com.qingfengmy.howold;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.widget.LinearLayout;

import net.youmi.android.AdManager;
import net.youmi.android.banner.AdSize;
import net.youmi.android.banner.AdView;
import net.youmi.android.spot.SpotDialogListener;
import net.youmi.android.spot.SpotManager;

/**
 * Created by Administrator on 2015/5/15.
 */
public class SplashActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        // ��ʼ��
        AdManager.getInstance(this).init("e4c2a0f8b449e7df",
                "dbc33b452df7de51", true);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setClass(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2000);
        SpotManager.getInstance(this).showSpotAds(this);
    }

    @Override
    public void onBackPressed() {
        // �������Ҫ�����Ե�����˹رղ�����棨��ѡ����
        if (!SpotManager.getInstance(this).disMiss(true)) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        // ��������ô˷�������home����ʱ������ͼ���޷���ʾ�������
        SpotManager.getInstance(this).disMiss(false);

        super.onStop();
    }
}
