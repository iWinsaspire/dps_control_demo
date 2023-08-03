package cn.dolphinstar.ctrl.demo;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.mydlna.dlna.core.ContentDevice;
import com.mydlna.dlna.core.DmcClientWraper;
import com.mydlna.dlna.core.RenderDevice;
import com.mydlna.dlna.service.DlnaDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.dolphinstar.ctrl.demo.utility.DemoActivityBase;
import cn.dolphinstar.ctrl.demo.utility.DemoConst;
import cn.dolphinstar.lib.IDps.IDpsOpenDmcBrowser;
import cn.dolphinstar.lib.POCO.ReturnMsg;
import cn.dolphinstar.lib.POCO.StartUpCfg;
import cn.dolphinstar.lib.ctrlCore.MYOUController;
import cn.dolphinstar.lib.wozkit.WozLogger;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MainActivity extends DemoActivityBase {

    private static final int REQUEST_PERMISSION_CODE = 100;
    private static int QR_SCAN_REQ_CODE = 9999;

    private StartUpCfg cfg;


    //刷新按钮
    private Button btnGetDevices;


    private Disposable deviceDisposable = null;


    //投屏码输入框
    private EditText etScreenCode;
    //投屏码认证按钮
    private Button btnScreenCode;

    private Button btnLink;
    private Button btnMirror;
    private Button btnScanQr;

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
        setContentView(R.layout.activity_main);

        //UI 按钮处理  727588
        btnGetDevices = findViewById(R.id.btn_get_device_list);
        etScreenCode =  findViewById(R.id.screen_code);
        etScreenCode.setText("269782");
        btnScreenCode = findViewById(R.id.btn_scode);
        btnScreenCode.setOnClickListener(v->{
            String text =  etScreenCode.getText().toString().trim();
            if(!text.isEmpty()){
                MYOUController.of(MainActivity.this).getDpsAuther().Connect(text)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(r->{
                            //输入投屏码认证成功后，主动搜索，可以快点发现
                            searchDevices();
                        },err->{
                            //失败
                            WozLogger.e( err );
                        });
            }

        });

        //视频投屏示例
        btnLink = findViewById(R.id.btn_link);
        btnLink.setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this,VideoActivity.class);
            startActivity(intent);
        });
        //镜像投屏投屏示例
        btnMirror = findViewById(R.id.btn_mirror);

        //扫码认证
        btnScanQr=findViewById(R.id.btn_scan_qr);
        btnScanQr.setOnClickListener(v->{
            ScanUtil.startScan(MainActivity.this, QR_SCAN_REQ_CODE, new HmsScanAnalyzerOptions.Creator()
                    .setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE).create());
        });

        btnGetDevices.setOnClickListener(v -> searchDevices());

        //版本小于 m 直接启动海豚星空投屏服务 否则检测权限后启动
        if (Build.VERSION.SDK_INT >Build.VERSION_CODES.M) {
            checkAndRequestPermission();
        }else{
            dpsSdkStartUp();
        }
    }

    //SDK启动
    @SuppressLint("CheckResult")
    private void dpsSdkStartUp() {
        cfg = new StartUpCfg();
        cfg.MediaServerName = "海豚星空DMS-" + (int) (Math.random() * 900 + 100);
        cfg.IsShowLogger = BuildConfig.DEBUG;
        cfg.AppSecret = "29f89b23775045a9";

        MYOUController.of(MainActivity.this)
                .useMirror()  //使用镜像模块
                .SetDmcBrowserListener(dpsOpenDmcBrowser)
                /*
                .SetPushReady(renderStatus -> {
                    // 状态为播放的时候 开始主动查询进度条
                    if (renderStatus.state == 1) {
                        //主动查询进度条
                        if (deviceDisposable == null) {
                            deviceDisposable = MYOUController.of(MainActivity.this).getDpsPlayer().Query().subscribe(s -> {
                                String stateText = "";
                                switch (s.state) {
                                    case 0:
                                        stateText = "停止";
                                        break;
                                    case 1:
                                        stateText = "播放中...";
                                        break;
                                    case 2:
                                        stateText = "暂停";
                                        break;
                                    default:
                                        break;
                                }
                                WozLogger.w("当前电视状态:" + stateText + "( " + s.state + " )"
                                        .concat("  总时长(秒)：" + s.duration)
                                        .concat("  当前进度(秒):" + s.progress)
                                        .concat("  当前音量:" + s.volume)
                                );

                                //结束 主动查询
                                if (s.state == 0) {
                                    if (deviceDisposable != null) {
                                        deviceDisposable.dispose();
                                        deviceDisposable = null;
                                    }
                                }
                            });
                        }
                    }
                })
                */
                // 启动服务
                .StartService(cfg)
                .subscribe(
                        y -> {
                            //只是为了切主线程 操作UI
                            Observable.timer(300, TimeUnit.MILLISECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(l -> {
                                        toast("Dps SDK启动成功");
                                        setTitle(cfg.MediaServerName);
                                        btnGetDevices.setEnabled(true);
                                    });
                        },
                        e -> toast(e.getLocalizedMessage()));
    }

    //搜索设备 获取当前发现在线的接收端设备列表
    @SuppressLint("CheckResult")
    private void searchDevices() {
        ArrayList<RenderDevice> list = MYOUController.of(MainActivity.this).getRenderDevice().GetAllOnlineDevices();
        if (list.size() > 0) {
            Observable.fromArray(list)
                    //.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(item -> {

                        btnLink.setEnabled(true);
                        btnMirror.setEnabled(true);
                    });
        } else {
            //没啥用 主要切主线程 操作UI
            Observable.timer(1, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(i -> {

                        toast("获取设备数量: " + list.size());
                        btnLink.setEnabled(false);
                        btnMirror.setEnabled(false);
                    });
        }
    }


    //region 动态权限申请

    private void checkAndRequestPermission() {

        List<String> lackedPermission = new ArrayList<>();

        if (!(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (lackedPermission.size() == 0) {
            onPermissionsOk();
        } else {
            String[] requestPermissions = new String[lackedPermission.size()];
            lackedPermission.toArray(requestPermissions);
            requestPermissions(requestPermissions, REQUEST_PERMISSION_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            onPermissionsOk();
        }
    }

    private  void onPermissionsOk(){
        dpsSdkStartUp();
    }
    //endregion


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            //扫码认证回调
            if (requestCode == QR_SCAN_REQ_CODE) {
                String content = "";
                HmsScan obj = intent.getParcelableExtra(ScanUtil.RESULT);
                if (obj != null) {
                    content = obj.originalValue;
                    //二维码认证
                    ReturnMsg rm = MYOUController.of(MainActivity.this).getDpsAuther().ScanQRCode(content);
                    if(rm.isOk){
                        toast("认证成功");
                        searchDevices();
                    }else{
                        toast(rm.errMsg);
                    }
                }else {
                    toast("扫码错误");
                }
            }
        }
        super.onActivityResult(resultCode,resultCode,intent);
    }

    @Override
    protected void onDestroy() {
        //关闭服务
        MYOUController.of(MainActivity.this).Close();
        if (deviceDisposable != null) {
            deviceDisposable.dispose();
        }
        super.onDestroy();
    }
}
