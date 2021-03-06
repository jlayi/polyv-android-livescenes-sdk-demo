package com.easefun.polyv.livecommon.module.modules.player.live.presenter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.easefun.polyv.businesssdk.api.auxiliary.IPolyvAuxiliaryVideoViewListenerEvent;
import com.easefun.polyv.businesssdk.api.auxiliary.PolyvAuxiliaryVideoview;
import com.easefun.polyv.businesssdk.api.common.player.PolyvPlayError;
import com.easefun.polyv.businesssdk.api.common.player.listener.IPolyvVideoViewListenerEvent;
import com.easefun.polyv.businesssdk.model.video.PolyvBaseVideoParams;
import com.easefun.polyv.businesssdk.model.video.PolyvDefinitionVO;
import com.easefun.polyv.businesssdk.model.video.PolyvLiveChannelVO;
import com.easefun.polyv.businesssdk.model.video.PolyvLiveLinesVO;
import com.easefun.polyv.businesssdk.model.video.PolyvLiveMarqueeVO;
import com.easefun.polyv.businesssdk.model.video.PolyvLiveVideoParams;
import com.easefun.polyv.businesssdk.model.video.PolyvMediaPlayMode;
import com.easefun.polyv.livecommon.module.config.PLVLiveChannelConfig;
import com.easefun.polyv.livecommon.module.data.IPLVLiveRoomDataManager;
import com.easefun.polyv.livecommon.module.modules.player.live.contract.IPLVLivePlayerContract;
import com.easefun.polyv.livecommon.module.modules.player.live.presenter.data.PLVLivePlayerData;
import com.easefun.polyv.livecommon.module.modules.player.live.presenter.data.PLVPlayInfoVO;
import com.easefun.polyv.livecommon.module.utils.PLVWebUtils;
import com.easefun.polyv.livecommon.module.utils.imageloader.PLVImageLoader;
import com.easefun.polyv.livescenes.video.PolyvLiveVideoView;
import com.easefun.polyv.livescenes.video.api.IPolyvLiveListenerEvent;
import com.plv.foundationsdk.config.PLVPlayOption;
import com.plv.foundationsdk.log.PLVCommonLog;
import com.plv.foundationsdk.utils.PLVControlUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * mvp-直播播放器presenter层实现，实现 IPLVLivePlayerContract.ILivePlayerPresenter 接口
 */
public class PLVLivePlayerPresenter implements IPLVLivePlayerContract.ILivePlayerPresenter {
    // <editor-fold defaultstate="collapsed" desc="变量">
    private static final String TAG = PLVLivePlayerPresenter.class.getSimpleName();
    private static final int WHAT_PLAY_PROGRESS = 1;
    private IPLVLiveRoomDataManager liveRoomDataManager;
    private PLVLivePlayerData livePlayerData;
    private WeakReference<IPLVLivePlayerContract.ILivePlayerView> vWeakReference;

    private PolyvLiveVideoView videoView;
    private PolyvAuxiliaryVideoview subVideoView;

    private IPolyvVideoViewListenerEvent.OnGestureClickListener onSubGestureClickListener;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="构造器">
    public PLVLivePlayerPresenter(@NonNull IPLVLiveRoomDataManager liveRoomDataManager) {
        this.liveRoomDataManager = liveRoomDataManager;
        livePlayerData = new PLVLivePlayerData();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="对外API - 实现IPLVLivePlayerContract.ILivePlayerPresenter定义的方法">
    @Override
    public void registerView(@NonNull IPLVLivePlayerContract.ILivePlayerView v) {
        this.vWeakReference = new WeakReference<>(v);
        v.setPresenter(this);
    }

    @Override
    public void unregisterView() {
        if (vWeakReference != null) {
            vWeakReference.clear();
            vWeakReference = null;
        }
    }

    @Override
    public void init() {
        if (getView() == null) {
            return;
        }
        //init data
        videoView = getView().getLiveVideoView();
        subVideoView = getView().getSubVideoView();
        initSubVideoViewListener();
        initVideoViewListener();
    }

    @Override
    public void startPlay() {
        PolyvLiveVideoParams liveVideoParams = new PolyvLiveVideoParams(
                getConfig().getChannelId(),
                getConfig().getAccount().getUserId(),
                getConfig().getUser().getViewerId()
        );
        liveVideoParams.buildOptions(PolyvBaseVideoParams.WAIT_AD, true)
                .buildOptions(PolyvBaseVideoParams.MARQUEE, true)
                .buildOptions(PolyvBaseVideoParams.PARAMS2, getConfig().getUser().getViewerName());
        if (videoView != null) {
            videoView.playByMode(liveVideoParams, PLVPlayOption.PLAYMODE_LIVE);
        }
        startPlayProgressTimer();
    }

    @Override
    public void restartPlay() {
        if (getView() != null) {
            getView().onRestartPlay();
        }
        startPlay();
    }

    @Override
    public void pause() {
        if (videoView != null) {
            videoView.pause();
        }
    }

    @Override
    public void resume() {
        if (videoView != null) {
            videoView.start();
        }
    }

    @Override
    public void stop() {
        if (videoView != null) {
            videoView.stopPlay();
        }
    }

    @Override
    public boolean isPlaying() {
        if (videoView != null) {
            return videoView.isPlaying();
        }
        return false;
    }

    @Override
    public int getLinesCount() {
        return (videoView == null || videoView.getModleVO() == null || videoView.getModleVO().getLines() == null)
                ? 1 : videoView.getModleVO().getLines().size();
    }

    @Nullable
    @Override
    public List<PolyvDefinitionVO> getBitrateVO() {
        List<PolyvDefinitionVO> definitionVOS = null;
        if (videoView != null) {
            PolyvLiveChannelVO channelVO = videoView.getModleVO();
            if (channelVO != null) {
                List<PolyvLiveLinesVO> liveLines = channelVO.getLines();//线路数量
                if (liveLines != null) {
                    PolyvLiveLinesVO linesVO = liveLines.get(getLinesPos());//当前线路信息
                    if (linesVO != null && linesVO.getMultirateModel() != null) {//存在码率信息
                        if (channelVO.isMutilrateEnable()) {//多码率可用
                            definitionVOS = linesVO.getMultirateModel().getDefinitions();
                        } else {
                            definitionVOS = new ArrayList<>();
                            definitionVOS.add(new PolyvDefinitionVO(linesVO.getMultirateModel().getDefaultDefinition()
                                    , linesVO.getMultirateModel().getDefaultDefinitionUrl()));
                        }
                    }
                }
            }
        }
        return definitionVOS;
    }

    @Override
    public int getMediaPlayMode() {
        if (videoView != null) {
            return videoView.getMediaPlayMode();
        }
        return PolyvMediaPlayMode.MODE_VIDEO;
    }

    @Override
    public void changeMediaPlayMode(int mediaPlayMode) {
        if (videoView != null) {
            videoView.changeMediaPlayMode(mediaPlayMode);
        }
    }

    @Override
    public void changeLines(int linesPos) {
        if (videoView != null) {
            videoView.changeLines(linesPos);
        }
    }

    @Override
    public void changeBitRate(int bitRate) {
        if (videoView != null) {
            videoView.changeBitRate(bitRate);
        }
    }

    @Override
    public Bitmap screenshot() {
        return videoView == null ? null : videoView.screenshot();
    }

    @Override
    public PolyvLiveChannelVO getChannelVO() {
        return videoView == null ? null : videoView.getModleVO();
    }

    @Override
    public int getLinesPos() {
        return videoView == null ? 0 : videoView.getLinesPos();
    }

    @Override
    public int getBitratePos() {
        return videoView == null ? 0 : videoView.getBitratePos();
    }

    @Override
    public void setVolume(int volume) {
        if (videoView != null) {
            videoView.setVolume(volume);
        }
    }

    @Override
    public int getVolume() {
        return videoView == null ? 0 : videoView.getVolume();
    }

    @Override
    public void setPlayerVolume(int volume) {
        if (videoView != null) {
            videoView.setPlayerVolume(volume);
        }
        if (subVideoView != null) {
            subVideoView.setPlayerVolume(volume);
        }
    }

    @Override
    public void setNeedGestureDetector(boolean need) {
        if (videoView != null) {
            videoView.setNeedGestureDetector(need);
        }
    }

    @NonNull
    @Override
    public PLVLivePlayerData getData() {
        return livePlayerData;
    }

    @Override
    public void destroy() {
        unregisterView();
        if (videoView != null) {
            videoView.destroy();
        }
        videoView = null;
        subVideoView = null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="播放器 - 初始化subVideo, videoView的监听器配置">
    private void initSubVideoViewListener() {
        if (subVideoView != null) {
            subVideoView.setOnVideoPlayListener(new IPolyvVideoViewListenerEvent.OnVideoPlayListener() {
                @Override
                public void onPlay(boolean isFirst) {
                    if (getView() != null) {
                        getView().onSubVideoViewPlay(isFirst);
                    }
                }
            });
            subVideoView.setOnGestureClickListener(onSubGestureClickListener = new IPolyvVideoViewListenerEvent.OnGestureClickListener() {
                @Override
                public void callback(boolean start, boolean end) {
                    if (getView() != null) {
                        getView().onSubVideoViewClick(videoView == null || videoView.isPlaying());
                    }
                }
            });
            subVideoView.setOnSubVideoViewLoadImage(new IPolyvAuxiliaryVideoViewListenerEvent.IPolyvOnSubVideoViewLoadImage() {
                @Override
                public void onLoad(String imageUrl, final ImageView imageView, final String coverHref) {
                    PLVImageLoader.getInstance().loadImage(subVideoView.getContext(), imageUrl, imageView);
                    if (!TextUtils.isEmpty(coverHref)) {
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PLVWebUtils.openWebLink(coverHref, subVideoView.getContext());
                            }
                        });
                    }
                }
            });
        }
    }

    private void initVideoViewListener() {
        if (videoView != null) {
            videoView.setOnErrorListener(new IPolyvVideoViewListenerEvent.OnErrorListener() {
                @Override
                public void onError(int what, int extra) {/**/}

                @Override
                public void onError(PolyvPlayError error) {
                    setDefaultViewStatus();

                    String tips = error.playStage == PolyvPlayError.PLAY_STAGE_HEADAD ? "片头广告"
                            : error.playStage == PolyvPlayError.PLAY_STAGE_TAILAD ? "片尾广告"
                            : error.playStage == PolyvPlayError.PLAY_STAGE_TEASER ? "暖场视频"
                            : error.isMainStage() ? "主视频" : "";
                    tips += "播放异常\n" + error.errorDescribe + " (errorCode:" + error.errorCode +
                            "-" + error.playStage + ")\n" + error.playPath;
                    if (getView() != null) {
                        getView().onPlayError(error, tips);
                    }
                }
            });
            videoView.setOnNoLiveAtPresentListener(new IPolyvLiveListenerEvent.OnNoLiveAtPresentListener() {
                @Override
                public void onNoLiveAtPresent() {
                    PLVCommonLog.d(TAG, "onNoLiveAtPresent");
                    videoView.removeRenderView();
                    livePlayerData.postNoLive();
                    if (getView() != null) {
                        getView().onNoLiveAtPresent();
                    }
                }

                @Override
                public void onLiveEnd() {
                    PLVCommonLog.d(TAG, "onLiveEnd");
                    livePlayerData.postLiveEnd();
                    if (getView() != null) {
                        getView().onLiveEnd();
                    }
                }

                @Override
                public void onLiveStop() {
                    PLVCommonLog.d(TAG, "onLiveStop");
                    livePlayerData.postLiveStop();
                    if (getView() != null) {
                        getView().onLiveStop();
                    }
                }
            });
            videoView.setOnPreparedListener(new IPolyvVideoViewListenerEvent.OnPreparedListener() {
                @Override
                public void onPrepared() {
                    PLVCommonLog.d(TAG, "onPrepared");
                    livePlayerData.postPrepared();
                    liveRoomDataManager.setSessionId(videoView.getModleVO() != null ? videoView.getModleVO().getChannelSessionId() : null);
                    if (videoView.getMediaPlayMode() == PolyvMediaPlayMode.MODE_AUDIO) {
                        videoView.removeRenderView();//need clear&unregister
                    }
                    if (getView() != null) {
                        getView().onPrepared(videoView.getMediaPlayMode());
                    }
                }

                @Override
                public void onPreparing() {/**/}
            });
            videoView.setOnLinesChangedListener(new IPolyvLiveListenerEvent.OnLinesChangedListener() {
                @Override
                public void OnLinesChanged(int linesPos) {
                    livePlayerData.postLinesChange(linesPos);
                    if (getView() != null) {
                        getView().onLinesChanged(linesPos);
                    }
                }
            });
            videoView.setOnGetMarqueeVoListener(new IPolyvVideoViewListenerEvent.OnGetMarqueeVoListener() {
                @Override
                public void onGetMarqueeVo(PolyvLiveMarqueeVO marqueeVo) {
                    if (getView() != null) {
                        getView().onGetMarqueeVo(marqueeVo, getConfig().getUser().getViewerName());
                    }
                }
            });
            videoView.setOnGestureClickListener(new IPolyvVideoViewListenerEvent.OnGestureClickListener() {
                @Override
                public void callback(boolean start, boolean end) {
                    //如果当前没有直播，才会将单击事件传递
                    if (!videoView.isOnline() && onSubGestureClickListener != null) {
                        onSubGestureClickListener.callback(start, end);
                    }
                }
            });
            videoView.setOnGestureLeftDownListener(new IPolyvVideoViewListenerEvent.OnGestureLeftDownListener() {
                @Override
                public void callback(boolean start, boolean end) {
                    int brightness = videoView.getBrightness((Activity) videoView.getContext()) - 8;
                    brightness = Math.max(0, brightness);
                    if (getView() != null) {
                        boolean result = getView().onLightChanged(brightness, end);
                        if (start && result) {
                            videoView.setBrightness((Activity) videoView.getContext(), brightness);
                        }
                    }
                }
            });
            videoView.setOnGestureLeftUpListener(new IPolyvVideoViewListenerEvent.OnGestureLeftUpListener() {
                @Override
                public void callback(boolean start, boolean end) {
                    int brightness = videoView.getBrightness((Activity) videoView.getContext()) + 8;
                    brightness = Math.min(100, brightness);
                    if (getView() != null) {
                        boolean result = getView().onLightChanged(brightness, end);
                        if (start && result) {
                            videoView.setBrightness((Activity) videoView.getContext(), brightness);
                        }
                    }
                }
            });
            videoView.setOnGestureRightDownListener(new IPolyvVideoViewListenerEvent.OnGestureRightDownListener() {
                @Override
                public void callback(boolean start, boolean end) {
                    int volume = videoView.getVolume() - PLVControlUtils.getVolumeValidProgress(videoView.getContext(), 8);
                    volume = Math.max(0, volume);
                    if (getView() != null) {
                        boolean result = getView().onVolumeChanged(volume, end);
                        if (start && result) {
                            videoView.setVolume(volume);
                        }
                    }
                }
            });
            videoView.setOnGestureRightUpListener(new IPolyvVideoViewListenerEvent.OnGestureRightUpListener() {
                @Override
                public void callback(boolean start, boolean end) {
                    int volume = videoView.getVolume() + PLVControlUtils.getVolumeValidProgress(videoView.getContext(), 8);
                    volume = Math.min(100, volume);
                    if (getView() != null) {
                        boolean result = getView().onVolumeChanged(volume, end);
                        if (start && result) {
                            videoView.setVolume(volume);
                        }
                    }
                }
            });
            videoView.setOnDanmuServerOpenListener(new IPolyvLiveListenerEvent.OnDanmuServerOpenListener() {
                @Override
                public void onDanmuServerOpenListener(boolean isServerDanmuOpen) {
                    if (getView() != null) {
                        getView().onServerDanmuOpen(isServerDanmuOpen);
                    }
                }
            });
            videoView.setMicroPhoneListener(new IPolyvLiveListenerEvent.MicroPhoneListener() {
                @Override
                public void showMicPhoneLine(int visible) {
                    PLVCommonLog.d(TAG, "showMicPhoneLine visible=" + visible);
                    livePlayerData.postLinkMicOpen(visible == View.VISIBLE, "audio".equals(videoView.getLinkMicType()));
                }
            });
            videoView.setOnPPTShowListener(new IPolyvVideoViewListenerEvent.OnPPTShowListener() {
                @Override
                public void showPPTView(int visible) {
                    livePlayerData.postPPTShowState(visible == View.VISIBLE);
                    if (getView() != null) {
                        getView().onShowPPTView(visible);
                    }
                }
            });
            videoView.setOnSEIRefreshListener(new IPolyvVideoViewListenerEvent.OnSEIRefreshListener() {
                @Override
                public void onSEIRefresh(int seiType, byte[] seiData) {
                    long ts = Long.parseLong(new String(seiData));
                    PLVCommonLog.v(TAG, "sei ts = " + ts);
                    livePlayerData.postSeiData(ts);
                }
            });
            videoView.setOnNetworkStateListener(new IPolyvVideoViewListenerEvent.OnNetworkStateListener() {
                @Override
                public boolean onNetworkRecover() {
                    PLVCommonLog.d(TAG, "PLVLivePlayerPresenter.onNetworkRecover");
                    if (getView() != null) {
                        return getView().onNetworkRecover();
                    }
                    return false;
                }

                @Override
                public boolean onNetworkError() {
                    PLVCommonLog.d(TAG, "PLVLivePlayerPresenter.onNetworkError");
                    return false;
                }
            });
        }
    }

    private void setDefaultViewStatus() {
        videoView.removeRenderView();
        if (getView() != null && getView().getBufferingIndicator() != null) {
            getView().getBufferingIndicator().setVisibility(View.GONE);
        }
        if (getView() != null && getView().getNoStreamIndicator() != null) {
            getView().getNoStreamIndicator().setVisibility(View.VISIBLE);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="播放器 - 定时获取播放信息任务">
    private Handler selfHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == WHAT_PLAY_PROGRESS) {
                startPlayProgressTimer();
            }
        }
    };

    private void startPlayProgressTimer() {
        stopPlayProgressTimer();
        if (videoView != null) {
            livePlayerData.postPlayInfoVO(
                    new PLVPlayInfoVO.Builder()
                            .isPlaying(videoView.isPlaying())
                            .build()
            );
            selfHandler.sendEmptyMessageDelayed(WHAT_PLAY_PROGRESS, 1000);
        } else {
            selfHandler.sendEmptyMessageDelayed(WHAT_PLAY_PROGRESS, 1000);
        }
    }

    private void stopPlayProgressTimer() {
        selfHandler.removeMessages(WHAT_PLAY_PROGRESS);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="内部工具方法">
    private IPLVLivePlayerContract.ILivePlayerView getView() {
        return vWeakReference != null ? vWeakReference.get() : null;
    }

    private PLVLiveChannelConfig getConfig() {
        return liveRoomDataManager.getConfig();
    }
    // </editor-fold>
}
