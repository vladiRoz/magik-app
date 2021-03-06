package com.kaltura.magikapp.magikapp.asset_page;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.connect.backend.phoenix.data.KalturaMediaAsset;
import com.kaltura.magikapp.MagikApplication;
import com.kaltura.magikapp.PlayerControlsView;
import com.kaltura.magikapp.R;
import com.kaltura.magikapp.data.JsonFetchHandler;
import com.kaltura.magikapp.magikapp.PlayerControlsController;
import com.kaltura.magikapp.magikapp.PlayerProvider;
import com.kaltura.magikapp.magikapp.PresenterController;
import com.kaltura.magikapp.magikapp.core.FragmentAid;
import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;

import java.util.Observable;
import java.util.Observer;

import static com.kaltura.magikapp.data.JsonFetchHandler.JsonType.STANDALONE_PLAYER;

/**
 * Created by zivilan on 01/01/2017.
 */

public class AssetPageFragment extends Fragment implements PresenterController.OnPresenterControllerEventListener, Observer {

    private static final PKLog log = PKLog.get("AssetPageFragment");

    private boolean isFirstPlay = true;
    private Context mContext;
    private View mContainer;
    private ViewPager mViewPager;
    protected FragmentAid mFragmentAid;
    private static KalturaMediaAsset mAssetInfo;
    private TextView mTitle;
    private TextView mSubTitle;
    private TextView mDescription;
    private ImageView mThumbImage;

    private Player mPlayer;
    private PlayerConfig mPlayerConfig;
    private PlayerControlsController mPlayerControlsController;
    private LinearLayout mPlayerView;
    private PlayerControlsView mPlayerControlsView;
    private FrameLayout mPlayerContainer;

    private boolean nowPlaying;
    ProgressBar progressBar;

    private Spinner videoSpinner, audioSpinner, textSpinner;




    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mFragmentAid = (FragmentAid) context;
        mContext = context;
    }

    public static Fragment newInstance(KalturaMediaAsset asset) {
        mAssetInfo = asset;
        return new AssetPageFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContainer = inflater.inflate(R.layout.asset_page_layout, container, false);
        return mContainer;
    }

    @Override
    public void onStop(){
        super.onStop();
        if (mPlayer != null){
            mPlayer.destroy();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mPlayer != null){
            mPlayer.destroy();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFragmentAid.setToolbarTitle("");
        mFragmentAid.setToolbarHomeButton(ToolbarMediator.BUTTON_BACK);

        mTitle = (TextView) mContainer.findViewById(R.id.asset_title);
        mSubTitle = (TextView) mContainer.findViewById(R.id.asset_sub_title);
        mDescription = (TextView) mContainer.findViewById(R.id.asset_description);

        mThumbImage = (ImageView) mContainer.findViewById(R.id.video_image_thumb);

        String url = mAssetInfo.getImages().get(0).getUrl();
        Glide.with(mContext).load(url.replace("/width/100","/width/360").replace("/height/100","/height/280")).fitCenter().crossFade().into(mThumbImage);

        mTitle.setText(mAssetInfo.getName());
        mSubTitle.setText(getString(R.string.asset_sub_title_sample));
        mDescription.setText(mAssetInfo.getDescription());


        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/" + MagikApplication.get().getConfigurations().getFontFamily());
        mTitle.setTypeface(typeface);
        mTitle.setTextColor(Color.parseColor(MagikApplication.get().getConfigurations().getSecondaryClr()));
        mDescription.setTypeface(typeface);

        mPlayerContainer = (FrameLayout) mContainer.findViewById(R.id.player_container);
        mPlayerView = (LinearLayout) mContainer.findViewById(R.id.player_view);

        mPlayerControlsView = (PlayerControlsView) mContainer.findViewById(R.id.player_controls_view);

        mPlayerControlsController = new PlayerControlsController(mPlayerControlsView, this);

        mPlayerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayerControlsController.handleContainerClick();
            }
        });

        Intent intent = getActivity().getIntent();

        fetchPlayerConfig(intent.getDataString());
    }

    private void fetchPlayerConfig(String playerConfigUrl) {

        JsonFetchHandler.fetchJson(playerConfigUrl, STANDALONE_PLAYER, getActivity(), new JsonFetchHandler.OnJsonFetchedListener() {

            @Override
            public void onJsonFetched(String playerConfigJson) {

                if (TextUtils.isEmpty(playerConfigJson)) {
                    return;
                }

                createPlayer(playerConfigJson);

            }

        });
    }



    private void createPlayer(String playerConfigJson) {

//        String playerConfigNew = playerConfigJson.replace("http://cdnapi.kaltura.com/p/1774581/sp/177458100/playManifest/entryId/1_tzhsuqij/format/applehttp/tags/ipad/protocol/http/f/a.m3u8",
//                mAssetInfo.getFiles().get(0).getUrl()).replace("1_1h1vsv3z_hls",
//                String.valueOf(mAssetInfo.getFiles().get(0).getId()));

        PlayerProvider playerProvider = new PlayerProvider();
        playerProvider.getPlayer(playerConfigJson, getActivity(), new PlayerProvider.OnPlayerReadyListener() {

            @Override
            public void onPlayerReady(Player player, PlayerConfig playerConfig) {

                if (player == null) {
                    return;
                }

                mPlayer = player;
                mPlayerConfig = playerConfig;
                startPlay();
                mPlayer.addEventListener(new PKEvent.Listener() {
                    @Override
                    public void onEvent(PKEvent event) {
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            mContainer.findViewById(R.id.video_image_thumb).setVisibility(View.GONE);
                        }
                    }
                }, PlayerEvent.Type.PLAY);
            }
        });

    }


    private void startPlay() {
        mPlayerView.removeAllViews();
        mPlayerView.addView(mPlayer.getView());
        mPlayerControlsController.setPlayer(mPlayer, mPlayerConfig);
    }

    private void toggleFullScreen(boolean setFullScreen) {

        handleDeviceOrientationChange(setFullScreen);

        if (setFullScreen) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }


    @Override
    public void onPlayerControlsEvent(PlayerControlsView.PlayerControlsEvents.ControlsEvent controlsEvent) {

        PlayerControlsView.PlayerControlsEvents.ControlsEvent.ButtonClickEvent buttonClickEvent = controlsEvent.getButtonClickEvent();

        switch (buttonClickEvent) {

            case FULL_SCREEN_SIZE:
                toggleFullScreen(true);
                break;

            case BACK_BUTTON:
                toggleFullScreen(false);
                break;
        }
    }

    private void handleDeviceOrientationChange(boolean setFullScreen) {

        mPlayerControlsController.handleScreenOrientationChange(setFullScreen);

        if (setFullScreen) {

            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        } else {

            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        switch(newConfig.orientation) {

            case Configuration.ORIENTATION_LANDSCAPE:
                Log.v("asset", "onConfigurationChanged " + "ORIENTATION_LANDSCAPE");
                handleDeviceOrientationChange(true);
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                Log.v("asset", "onConfigurationChanged " + "ORIENTATION_PORTRAIT");
                handleDeviceOrientationChange(false);
                break;

            default:
                Log.v("asset", "onConfigurationChanged " + "default");
        }
    }

    //XXX
    @Override
    public void onCastEvent() {

    }


    //XXX
    @Override
    public void logEvent(String log) {

    }

    //XXX
    @Override
    public void handleError(int error) {

    }

    @Override
    public void update(Observable observable, Object objectStatus) {

    }
}
