package com.easefun.polyv.livecommon.module.modules.player.playback.view;

import android.support.annotation.NonNull;
import android.view.View;

import com.easefun.polyv.businesssdk.api.common.player.PolyvPlayError;
import com.easefun.polyv.businesssdk.model.video.PolyvLiveMarqueeVO;
import com.easefun.polyv.livecommon.module.modules.player.playback.contract.IPLVPlaybackPlayerContract;
import com.easefun.polyv.livescenes.playback.video.PolyvPlaybackVideoView;

/**
 * mvp-回放播放器view层抽象类
 */
public abstract class PLVAbsPlaybackPlayerView implements IPLVPlaybackPlayerContract.IPlaybackPlayerView {
    @Override
    public void setPresenter(@NonNull IPLVPlaybackPlayerContract.IPlaybackPlayerPresenter presenter) {

    }

    @Override
    public PolyvPlaybackVideoView getPlaybackVideoView() {
        return null;
    }

    @Override
    public View getBufferingIndicator() {
        return null;
    }

    @Override
    public void onPrepared() {

    }

    @Override
    public void onPlayError(PolyvPlayError error, String tips) {

    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onVideoPlay(boolean isFirst) {

    }

    @Override
    public void onVideoPause() {

    }

    @Override
    public void onBufferStart() {

    }

    @Override
    public void onBufferEnd() {

    }

    @Override
    public boolean onLightChanged(int changeValue, boolean isEnd) {
        return false;
    }

    @Override
    public boolean onVolumeChanged(int changeValue, boolean isEnd) {
        return false;
    }

    @Override
    public boolean onProgressChanged(int seekTime, int totalTime, boolean isEnd, boolean isRightSwipe) {
        return false;
    }

    @Override
    public void onDoubleClick() {

    }

    @Override
    public void onGetMarqueeVo(PolyvLiveMarqueeVO marqueeVo, String viewerName) {

    }

    @Override
    public void onServerDanmuOpen(boolean isServerDanmuOpen) {

    }

    @Override
    public void onShowPPTView(int visible) {

    }
}
