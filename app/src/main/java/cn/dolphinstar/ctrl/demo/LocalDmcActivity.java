package cn.dolphinstar.ctrl.demo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.mydlna.dlna.core.ContentDevice;
import com.mydlna.dlna.core.ContentInfoEx;
import com.mydlna.dlna.core.DmcClientWraper;
import com.mydlna.dlna.core.RenderDevice;
import com.mydlna.dlna.service.DlnaDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.dolphinstar.ctrl.demo.utility.ContentInfoListAdapter;
import cn.dolphinstar.ctrl.demo.utility.DemoActivityBase;
import cn.dolphinstar.ctrl.demo.utility.DemoConst;
import cn.dolphinstar.ctrl.demo.utility.DeviceListAdapter;
import cn.dolphinstar.lib.IDps.IDpsOpenDmcBrowser;
import cn.dolphinstar.lib.POCO.PushContentModel;
import cn.dolphinstar.lib.POCO.ReturnMsg;
import cn.dolphinstar.lib.ctrlCore.MYOUController;
import cn.dolphinstar.lib.wozkit.WozLogger;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LocalDmcActivity extends DemoActivityBase {

    //监听设备状态
    IDpsOpenDmcBrowser dpsOpenDmcBrowser = new IDpsOpenDmcBrowser() {
        @Override
        public void DMCServiceStatusNotify(int status) {
        }

        //状态
        @Override
        public void DlnaDeviceStatusNotify(DlnaDevice device) {
            if (ContentDevice.isContentDevice(device)) {
                switch (device.stateNow) {
                    case DemoConst.DEVICE_STATE_ONLINE:
                        // 有新 内容供应 设备上线
                        searchContentDevice();
                        break;
                    case DemoConst.DEVICE_STATE_OFFLINE:
                        // 有设 内容供应 设备离线
                        searchContentDevice();
                        break;
                    default:
                        WozLogger.e("unknown content device state : " + device.stateNow);
                        break;
                }
            }
            else
            if (RenderDevice.isRenderDevice(device)) {
                switch (device.stateNow) {
                    case DemoConst.DEVICE_STATE_ONLINE:
                        // 有新的接收端设备上线
                        searchDevices();
                        break;
                    case DemoConst.DEVICE_STATE_OFFLINE:
                        // 有接收端设备离线
                        searchDevices();
                        break;
                    default:
                        //unknown render device state
                        break;
                }
            }
        }

        //DMS媒体文件变更通知 照成无需改动
        @Override
        public void DlnaFilesNotify(String udn, int videoCount, int audioCount, int imageCount, int fileCount) {
            if (TextUtils.isEmpty(udn)) {
                return;
            }
            final ContentDevice device = ContentDevice.sDevices.findDeviceByUdn(udn);

            if (device != null) {
                final int fAudioCount = audioCount;
                final int fVideoCount = videoCount;
                final int fImageCount = imageCount;
                final int fFileCount = fileCount;

                showFile();
                new Runnable() {
                    public void run() {
                        device.updateContent(DmcClientWraper.sClient, fAudioCount,
                                fImageCount, fVideoCount, fFileCount);
                    }
                };
            }
        }

    };


    private boolean isConnectDmc = false;
    private String dmcName = null;

    private LinearLayout llDeviceLayout;
    //播放设备列表
    private ListView lvRender;
    private DeviceListAdapter lvRenderAdapter;
    private ArrayList<RenderDevice> renderDeviceList;

    //媒体内容列表
    private ListView lvContent;
    private ContentInfoListAdapter lvContentAdapter;
    private ArrayList<ContentInfoEx> contentDeviceList;

    //被选中的内容
    private ContentInfoEx selectContentInfo;

    //媒体类型 0 视频     1 音乐  2 图片
    private  int mediaType = 0;

    private  void showFile(){

        ArrayList<ContentInfoEx> contents = MYOUController.of(LocalDmcActivity.this)
                .getContentDevice().GetMediaContent(mediaType);

        if(contents.size() >0 ){
            Observable.fromArray(contents)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(item -> {
                        contentDeviceList.clear();
                        contentDeviceList.addAll(contents);
                        lvContentAdapter.notifyDataSetChanged();
                    });

            for (ContentInfoEx video: contents ) {
                WozLogger.e( video.title + "   ->  " + video.resourceUrl);
            }
        }else{
            contentDeviceList.clear();
            lvContentAdapter.notifyDataSetChanged();
            toast("没有文件");
        }



    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_dmc);

        Intent intent = getIntent();
        dmcName = intent.getExtras().getString("myDmcName");

        //内容列表
        contentDeviceList = new ArrayList<>();
        lvContent = findViewById(R.id.lv_content_list);
        lvContent.requestLayout();
        lvContentAdapter = new ContentInfoListAdapter(this, R.layout.content_list_item, contentDeviceList);
        lvContent.setAdapter(lvContentAdapter);
        lvContent.setOnItemClickListener((parent, view, position, id) -> {
            selectContentInfo  = contentDeviceList.get(position);
            //点击设备列表中的电视，投屏到该电视上
            searchDevices();
            llDeviceLayout.setVisibility(View.VISIBLE);
        });

        llDeviceLayout = findViewById(R.id.ll_device_list);
        //播放设备列表
        renderDeviceList = new ArrayList<>();
        lvRender = findViewById(R.id.lv_device_list);
        lvRender.requestLayout();
        lvRenderAdapter = new DeviceListAdapter(this, R.layout.device_list_item, renderDeviceList);
        lvRender.setAdapter(lvRenderAdapter);
        lvRender.setOnItemClickListener((parent, view, position, id) -> {
            //点击设备列表中的电视，投屏到该电视上
            if (renderDeviceList != null && renderDeviceList.size() > position) {
                RenderDevice device = renderDeviceList.get(position);
                push2Device(device);
                llDeviceLayout.setVisibility(View.GONE);
            }
        });


        //注意 MYOUController是个单实例，设置监听器将覆盖之前设置的。
        MYOUController.of(LocalDmcActivity.this)
                .SetDmcBrowserListener(dpsOpenDmcBrowser);

        searchContentDevice();
    }


    //链接投屏实例
    @SuppressLint("CheckResult")
    private void push2Device(RenderDevice device) {
        if(selectContentInfo == null){
            toast("选择投放内容");
            return;
        }
        //0 视频     1 音乐  2 图片
        ReturnMsg msg = null;
        switch (mediaType){
            case 0:
                //投放视频
                  msg = MYOUController.of(LocalDmcActivity.this)
                        .getDpsPlayer()
                        .PushVideo(selectContentInfo.resourceUrl, selectContentInfo.title, device);

                break;
            case 1:
                //投放音乐
                msg = MYOUController.of(LocalDmcActivity.this)
                        .getDpsPlayer()
                        .PushAudio(selectContentInfo.resourceUrl, selectContentInfo.title, device);
                break;
            case 2:
                //投放音乐
                msg = MYOUController.of(LocalDmcActivity.this)
                        .getDpsPlayer()
                        .PushImage(selectContentInfo.resourceUrl, selectContentInfo.title, device);
                break;
            default:
                break;
        }

        if (msg !=null && msg.isOk) {
            toast("成功投屏到 -> " + device.nameString);
        } else {
            toast("失败信息 ：" + msg.errMsg);
        }
    }


    //搜索内容设备
    @SuppressLint("CheckResult")
    private void searchContentDevice() {
        List<String> list = MYOUController.of(LocalDmcActivity.this)
                .getContentDevice().GetAllOnlineDevice();
        if (list.size() > 0) {

            //初始化时要相连接本地dmc
            if(!isConnectDmc && !this.dmcName.isEmpty()){

                for (String item : list) {
                    //基于代码安全考虑 要dmc列表中有本机存在链接
                    if(dmcName.equals(item)){
                        ReturnMsg msg =  MYOUController.of(LocalDmcActivity.this)
                                .getContentDevice().ConnectByNamString(item);
                        if(msg.isOk){
                            isConnectDmc = true;
                            setTitle(dmcName);
                            showFile();
                        }
                        break;
                    }
                }
            }

        } else {
            //没啥用 主要切主线程 操作UI
            Observable.timer(1, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(i -> {
                        showFile();
                    });
        }
    }

    //搜索设备 获取当前发现在线的接收端设备列表
    @SuppressLint("CheckResult")
    private void searchDevices() {
        ArrayList<RenderDevice> list = MYOUController.of(LocalDmcActivity.this)
                .getRenderDevice().GetAllOnlineDevices();
        if (list.size() > 0) {
            Observable.fromArray(list)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(item -> {
                        renderDeviceList.clear();
                        renderDeviceList.addAll(item);
                        lvRenderAdapter.notifyDataSetChanged();
                    });
        } else {
            //没啥用 主要切主线程 操作UI
            Observable.timer(1, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(i -> {
                        renderDeviceList.clear();
                        lvRenderAdapter.notifyDataSetChanged();
                        toast("获取设备数量: " + list.size());
                    });
        }
    }
}