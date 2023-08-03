package cn.dolphinstar.ctrl.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.mydlna.dlna.core.ContentDevice;
import com.mydlna.dlna.core.DmcClientWraper;
import com.mydlna.dlna.core.RenderDevice;
import com.mydlna.dlna.service.DlnaDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.dolphinstar.ctrl.demo.utility.DemoConst;
import cn.dolphinstar.ctrl.demo.utility.DeviceListAdapter;
import cn.dolphinstar.lib.DpsMirrorConsts;
import cn.dolphinstar.lib.IDps.IDpsOpenDmcBrowser;
import cn.dolphinstar.lib.POCO.MirrorCfg;
import cn.dolphinstar.lib.POCO.ReturnMsg;
import cn.dolphinstar.lib.ctrlCore.MYOUController;
import cn.dolphinstar.lib.wozkit.WozLogger;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MirrorUIActivity extends AppCompatActivity {

    private static final int REQUEST_ALERTWINDOW_PERMISSION_CODE = 103;

    //清晰度控件
    private Spinner spResolution;
    //方向控件
    private Spinner spOrient;

    //码率控件
    private Spinner spBitrate;
    //帧率控件
    private Spinner spVFPS;

    //传输方式选项组
    private RadioGroup rgTranslation;
    //TCP协议选项
    private RadioButton rbTCP;
    //UDP协议选项
    private RadioButton rbUDP;
    private String tempTranslation = "TCP";
    private Button btnOk;


    private MirrorCfg mirrorCfg;

    private  RenderDevice device;

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
        setContentView(R.layout.activity_mirrorui);

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealMetrics(metrics);
        mirrorCfg = new MirrorCfg(metrics.widthPixels,metrics.heightPixels);


        //设备列表
        renderDeviceList = new ArrayList<>();
        lvDevice = findViewById(R.id.lv_device_list);
        lvDevice.requestLayout();
        lvAdapter = new DeviceListAdapter(this, R.layout.device_list_item, renderDeviceList);
        lvDevice.setAdapter(lvAdapter);
        lvDevice.setOnItemClickListener((parent, view, position, id) -> {
            //点击设备列表中的电视，投屏到该电视上
            if (renderDeviceList != null && renderDeviceList.size() > position) {
                 this.device = renderDeviceList.get(position);
                 setTitle(this.device.nameString);
                ReturnMsg connMsg =  MYOUController.of(MirrorUIActivity.this).getRenderDevice()
                        .ConnectByNamString(device.nameString);

                if(connMsg.isOk){
                    //启动镜像先检测是否有窗口上的权限，有直接投，否则授权后投
                    requestAlertWindowPermission();
                }
            }
        });

        _initBitrateOpt();
        _initOkBtn();
        _initResolutionOpt();
        _initTransmissionMode();
        _initVFPSOpt();
        _initOrientOpt();

        //注意 MYOUController是个单实例，设置监听器将覆盖之前设置的。
        MYOUController.of(MirrorUIActivity.this)
                .SetDmcBrowserListener(dpsOpenDmcBrowser);
    }


    //搜索设备 获取当前发现在线的接收端设备列表
    @SuppressLint("CheckResult")
    private void searchDevices() {
        ArrayList<RenderDevice> list = MYOUController.of(MirrorUIActivity.this)
                .getRenderDevice().GetAllOnlineDevices();
        if (list.size() > 0) {
            WozLogger.e("搜索到设备数量->" + list.size());
            Observable.fromArray(list)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(item -> {
                        renderDeviceList.clear();
                        renderDeviceList.addAll(item);
                        lvAdapter.notifyDataSetChanged();
                        lvDevice.setVisibility(View.VISIBLE);
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

    //分辨率选项的初始化
    private void _initResolutionOpt(){
        spResolution = findViewById(R.id.spResolution);
        List<String> listResolution = mirrorCfg.getSupportResolutionList();

        ArrayAdapter<String> adapterResolution = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listResolution);
        adapterResolution.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spResolution.setAdapter(adapterResolution);
        spResolution.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                arg0.setVisibility(View.VISIBLE);
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                arg0.setVisibility(View.VISIBLE);
            }
        });

        spResolution.setSelection(0, true);
        for (int i = 0; i < listResolution.size(); i++) {
            if (mirrorCfg.getResolution().equals(listResolution.get(i))) {
                spResolution.setSelection(i, true);
                break;
            }
        }
    }

    //横竖屏幕
    private void _initOrientOpt(){
        spOrient = findViewById(R.id.spOrient);
        List<Integer> list =  new ArrayList<>();
        list.add(0);
        list.add(1);

        ArrayAdapter<Integer> adapterResolution = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, list);
        adapterResolution.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrient.setAdapter(adapterResolution);
        spOrient.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                arg0.setVisibility(View.VISIBLE);
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                arg0.setVisibility(View.VISIBLE);
            }
        });

        spOrient.setSelection(0, true);

        for (int i = 0; i < list.size(); i++) {
            if (mirrorCfg.getDisplayScale()==list.get(i)) {
                spOrient.setSelection(i, true);
                break;
            }
        }
    }

    //码率选项的初始化
    private void _initBitrateOpt(){
        spBitrate = findViewById(R.id.spBitrate);

        List<String> listBitrate = mirrorCfg.getSupportBitrateList();

        ArrayAdapter<String> adapterRate = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listBitrate);
        adapterRate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBitrate.setAdapter(adapterRate);
        spBitrate.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub

                arg0.setVisibility(View.VISIBLE);
                //spinnerBitrateChoose = (String) spinner_Bitrate.getSelectedItem();
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

                arg0.setVisibility(View.VISIBLE);
            }
        });

        spBitrate.setSelection(0, true);
        for (int i = 0; i < listBitrate.size(); i++) {
            if (mirrorCfg.getBitrate().equals(listBitrate.get(i))) {
                spBitrate.setSelection(i, true);
                break;
            }
        }
    }

    //帧率选项的初始化
    private void _initVFPSOpt(){
        spVFPS = findViewById(R.id.spVFPS);
        List<Integer> listVFPS = mirrorCfg.getSupportVfpsList();
        ArrayAdapter<Integer> adapterVFPS = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listVFPS);
        adapterVFPS.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVFPS.setAdapter(adapterVFPS);
        spVFPS.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub

                arg0.setVisibility(View.VISIBLE);
                //spinnerVFPSChoose = ((Integer)spinner_VFPS.getSelectedItem()).intValue();
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

                arg0.setVisibility(View.VISIBLE);
            }
        });

        spVFPS.setSelection(0, true);
        for (int i = 0; i < listVFPS.size(); i++) {
            if (mirrorCfg.getVfps() == listVFPS.get(i)) {
                spVFPS.setSelection(i, true);
                break;
            }
        }
    }

    //初始化传输协议
    private void _initTransmissionMode(){
        rbTCP = findViewById(R.id.rbTCP);
        rbUDP = findViewById(R.id.rbUDP);
        rbTCP.setChecked(mirrorCfg.getTranslation().equals("TCP"));
        rbUDP.setChecked( mirrorCfg.getTranslation().equals("UDP"));

        rgTranslation = findViewById(R.id.rgTranslation);

        rgTranslation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.rbTCP){
                    tempTranslation = "TCP";
                }else if(i== R.id.rbUDP){
                    tempTranslation = "UDP";
                }
            }
        });

    }

    //配置确定按钮
    private void _initOkBtn(){
        btnOk = findViewById(R.id.btnOk);
        btnOk.setOnClickListener(view -> {
            try {
                String cResolution = (String) spResolution.getSelectedItem();
                String cBitrate = (String) spBitrate.getSelectedItem();
                int cVFPS = ((Integer)spVFPS.getSelectedItem()).intValue();
                mirrorCfg.setResolution(cResolution);
                mirrorCfg.setBitrate(cBitrate);
                mirrorCfg.setVfps(cVFPS);
                mirrorCfg.setTranslation(tempTranslation);
                int orient = ((Integer)spOrient.getSelectedItem()).intValue();
                WozLogger.i("方向 -> " + orient);
                mirrorCfg.setDisplayScale(orient);
                Toast.makeText(getApplicationContext(), "设置成功,请重新投屏!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {

            }
        });

    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_ALERTWINDOW_PERMISSION_CODE){
            if (!Settings.canDrawOverlays(this)) {
                toast("未授权悬浮窗,无法开启镜像功能.");
            }else {
                ReturnMsg msg =  MYOUController.of(MirrorUIActivity.this).getDpsMirror().Start(mirrorCfg);
                if(!msg.isOk){ toast(msg.errMsg);  }
            }
        }else
        if (requestCode == DpsMirrorConsts.RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            MYOUController.of(MirrorUIActivity.this).getDpsMirror().onAfterActivityResult(requestCode,resultCode,data);
            toast("确定镜像");
            //开始镜像 修改UI
            goMirrir2Activity();
        }else{
            toast("取消镜像");
        }
        super.onActivityResult(resultCode,resultCode,data);
    }

    public void toast(String msg) {
        Toast.makeText(MirrorUIActivity.this, msg, Toast.LENGTH_SHORT).show();
        Log.i("Toast",msg);
    }

    private void goMirrir2Activity(){
        Intent intent = new Intent(MirrorUIActivity.this,Mirror2Activity.class);
        startActivity(intent);

    }

    private void requestAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (! Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_ALERTWINDOW_PERMISSION_CODE);
            }else{
                //启动镜像
                ReturnMsg msg =  MYOUController.of(MirrorUIActivity.this).getDpsMirror().Start(mirrorCfg);
                if(!msg.isOk){ toast(msg.errMsg);  }
            }
        }
    }
}
