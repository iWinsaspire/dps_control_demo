package cn.dolphinstar.ctrl.demo;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import java.util.ArrayList;
import java.util.List;

import cn.dolphinstar.ctrl.demo.utility.DemoActivityBase;
import cn.dolphinstar.lib.POCO.ReturnMsg;
import cn.dolphinstar.lib.POCO.StartUpCfg;
import cn.dolphinstar.lib.ctrlCore.MYOUController;
import cn.dolphinstar.lib.wozkit.WozLogger;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MainActivity extends DemoActivityBase {

    private static final int REQUEST_PERMISSION_CODE = 100;
    private static int QR_SCAN_REQ_CODE = 9999;


    //投屏码输入框
    private EditText etScreenCode;
    //投屏码认证按钮
    private Button btnScreenCode;

    private Button btnLink;
    private Button btnMirror;
    private Button btnScanQr;
    private  Button btnLocalFile;
    StartUpCfg cfg = null;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI 按钮处理
        etScreenCode =  findViewById(R.id.screen_code);
        //etScreenCode.setText("269782");
        btnScreenCode = findViewById(R.id.btn_scode);
        btnScreenCode.setOnClickListener(v->{
            String text =  etScreenCode.getText().toString().trim();
            if(!text.isEmpty()){
                MYOUController.of(MainActivity.this).getDpsAuther().Connect(text)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(r->{
                            //输入投屏码认证成功后，主动搜索，可以快点发现
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
        btnMirror.setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this,MirrorUIActivity.class);
            startActivity(intent);
        });

        //本地文件投屏实例
        btnLocalFile = findViewById(R.id.btn_local_file);
        btnLocalFile.setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this,LocalDmcActivity.class);
            intent.putExtra("myDmcName",cfg.MediaServerName);
            startActivity(intent);
        });
        //扫码认证
        btnScanQr=findViewById(R.id.btn_scan_qr);
        btnScanQr.setOnClickListener(v->{
            ScanUtil.startScan(MainActivity.this, QR_SCAN_REQ_CODE, new HmsScanAnalyzerOptions.Creator()
                    .setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE).create());
        });


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
        cfg.AppSecret = "xxxxxxx"; //这里填入你的秘钥


        //demo 特殊配置信息 ，非必要。按自己想要的方式给 AppId AppSecret赋值就好
        if(!BuildConfig.dpsAppId.isEmpty()){
            //虽然这里可以配置AppId，
            //但app/src/main/assets/dpsAppInfo文件还是必须存在，可以不配置真的值。
            cfg.AppId = BuildConfig.dpsAppId;
        }
        if(!BuildConfig.dpsAppSecret.isEmpty()){
            cfg.AppSecret = BuildConfig.dpsAppSecret;
        }

        MYOUController.of(MainActivity.this)
                .useMirror()  //使用镜像模块
                .StartService(cfg)   // 启动服务
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        y -> {
                            toast("Dps SDK启动成功");
                            setTitle(cfg.MediaServerName);
                            btnLink.setEnabled(true);
                            btnMirror.setEnabled(true);
                            btnLocalFile.setEnabled(true);
                        },
                        e -> toast(e.getLocalizedMessage()));
    }

    //region 动态权限申请

    private void checkAndRequestPermission() {

        List<String> lackedPermission = new ArrayList<>();

        //程序可以读取设备外部存储空间
        if (!(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        //扫码
        if (!(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.CAMERA);
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
                    ReturnMsg rm = MYOUController.of(MainActivity.this)
                            .getDpsAuther().ScanQRCode(content);
                    if(rm.isOk){
                        toast("认证成功");
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

        super.onDestroy();
    }
}
