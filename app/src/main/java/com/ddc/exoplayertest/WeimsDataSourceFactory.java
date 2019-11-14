package com.ddc.exoplayertest;

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;

/**
 * Created by Administrator on 2019/11/14.
 * <p>
 * by author wz
 * <p>
 * com.ddc.exoplayertest
 */
public class WeimsDataSourceFactory implements DataSource.Factory {

    private final Context context;
    private final TransferListener<? super DataSource> listener;
    private final DataSource.Factory baseDataSourceFactory;

    public WeimsDataSourceFactory(Context context, String userAgent,
                                   TransferListener<? super DataSource> listener) {
        this(context, listener, new DefaultHttpDataSourceFactory(userAgent, listener));
    }

    public WeimsDataSourceFactory(Context context, TransferListener<? super DataSource> listener,
                                   DataSource.Factory baseDataSourceFactory) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.baseDataSourceFactory = baseDataSourceFactory;
    }

    @Override
    public DataSource createDataSource() {
        //return new WeimsDataSource(context, listener, baseDataSourceFactory.createDataSource());
        return null;
    }
}
