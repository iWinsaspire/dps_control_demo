package cn.dolphinstar.ctrl.demo;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.mydlna.dlna.core.ContentDevice;
import com.mydlna.dlna.core.DmcClientWraper;
import com.mydlna.dlna.core.RenderDevice;
import com.mydlna.dlna.service.DlnaDevice;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import cn.dolphinstar.ctrl.demo.utility.DemoActivityBase;
import cn.dolphinstar.ctrl.demo.utility.DemoConst;
import cn.dolphinstar.lib.IDps.IDpsOpenDmcBrowser;
import cn.dolphinstar.lib.POCO.ReturnMsg;
import cn.dolphinstar.lib.ctrlCore.MYOUController;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import cn.dolphinstar.ctrl.demo.utility.DeviceListAdapter;

public class VideoActivity extends DemoActivityBase {


    private LinearLayout llDeviceLayout;
    private Button btnCastV;
    //设备列表
    private ListView lvDevice;
    private DeviceListAdapter lvAdapter;
    private ArrayList<RenderDevice> renderDeviceList;

    //监听设备状态
    IDpsOpenDmcBrowser dpsOpenDmcBrowser = new IDpsOpenDmcBrowser() {
        @Override
        public void DMCServiceStatusNotify(int status) {
        }

        //状态
        @Override
        public void DlnaDeviceStatusNotify(DlnaDevice device) {
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

                new Runnable() {
                    public void run() {
                        device.updateContent(DmcClientWraper.sClient, fAudioCount,
                                fImageCount, fVideoCount, fFileCount);
                    }
                };
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        llDeviceLayout = findViewById(R.id.ll_device_list);

        //投屏按钮
        btnCastV = findViewById(R.id.btn_cast_v);
        btnCastV.setOnClickListener(v->{
            //点击投屏按钮显示设备列表
            llDeviceLayout.setVisibility(View.VISIBLE);
        });

        //设备列表
        renderDeviceList = new ArrayList<>();
        lvDevice = findViewById(R.id.lv_device_list);
        lvDevice.requestLayout();
        lvAdapter = new DeviceListAdapter(this, R.layout.device_list_item, renderDeviceList);
        lvDevice.setAdapter(lvAdapter);
        lvDevice.setOnItemClickListener((parent, view, position, id) -> {
            //点击设备列表中的电视，投屏到该电视上
            if (renderDeviceList != null && renderDeviceList.size() > position) {
                RenderDevice device = renderDeviceList.get(position);
                push2Device(device);
                llDeviceLayout.setVisibility(View.GONE);
            }
        });

        //注意 MYOUController是个单实例，设置监听器将覆盖之前设置的。
        MYOUController.of(VideoActivity.this)
                .SetDmcBrowserListener(dpsOpenDmcBrowser);
    }


    //搜索设备 获取当前发现在线的接收端设备列表
    @SuppressLint("CheckResult")
    private void searchDevices() {
        ArrayList<RenderDevice> list = MYOUController.of(VideoActivity.this)
                .getRenderDevice().GetAllOnlineDevices();
        if (list.size() > 0) {
            Observable.fromArray(list)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(item -> {
                        renderDeviceList.clear();
                        renderDeviceList.addAll(item);
                        lvAdapter.notifyDataSetChanged();
                    });
        } else {
            //没啥用 主要切主线程 操作UI
            Observable.timer(1, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(i -> {
                        renderDeviceList.clear();
                        lvAdapter.notifyDataSetChanged();
                        toast("获取设备数量: " + list.size());
                    });
        }
    }


    //链接投屏实例
    @SuppressLint("CheckResult")
    private void push2Device(RenderDevice device) {

        //投放视频
        ReturnMsg msg = MYOUController.of(VideoActivity.this)
                .getDpsPlayer()
                .PushVideo("https://dolphinstar.cn/fs/video/auth/auth_succes.mp4", "标题", device);

        if (msg.isOk) {
            toast("成功投屏到 -> " + device.nameString);
        } else {
            toast("失败信息 ：" + msg.errMsg);
        }
    }

}