package cn.dolphinstar.ctrl.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.dolphinstar.lib.DpsMirrorConsts;
import cn.dolphinstar.lib.POCO.MirrorCfg;
import cn.dolphinstar.lib.POCO.ReturnMsg;
import cn.dolphinstar.lib.ctrlCore.MYOUController;
import cn.dolphinstar.lib.wozkit.WozLogger;

public class TvActivity extends AppCompatActivity {

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

    private Button btnMirror;

    private MirrorCfg mirrorCfg;

    String renderDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv);

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealMetrics(metrics);
        mirrorCfg = new MirrorCfg(metrics.widthPixels,metrics.heightPixels);
        Intent intent  = getIntent();
        renderDeviceName = intent.getStringExtra("renderDeviceName");

        //mirrorCfg.setDisplayScale(0);

        _initBitrateOpt();
        _initMirrorBtn();
        _initOkBtn();
        _initResolutionOpt();
        _initTransmissionMode();
        _initVFPSOpt();
        _initOrientOpt();
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


    //开启镜像按钮
    private int mirrorState = 0;
    private  void _initMirrorBtn(){
        btnMirror = findViewById(R.id.startMirror);

        ReturnMsg connMsg =  MYOUController.of(TvActivity.this).getRenderDevice().ConnectByNamString(renderDeviceName);
        if(!connMsg.isOk){
            btnMirror.setEnabled(false);
            WozLogger.e(connMsg.errMsg);
        }

        btnMirror.setOnClickListener(view ->{
            if(mirrorState == 0) {
                requestAlertWindowPermission();

            }else{
                mirrorState = 0 ;
                MYOUController.of(TvActivity.this).getDpsMirror().Stop();
                btnOk.setEnabled(true);
                btnMirror.setText("开启镜像");
                btnOk.setEnabled(false);
                btnOk.setText("确定");
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
                ReturnMsg msg =  MYOUController.of(TvActivity.this).getDpsMirror().Start(mirrorCfg);
                if(!msg.isOk){ toast(msg.errMsg);  }
            }
        }else
        if (requestCode == DpsMirrorConsts.RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            MYOUController.of(TvActivity.this).getDpsMirror().onAfterActivityResult(requestCode,resultCode,data);
            toast("确定镜像");
            //开始镜像 修改UI
            goMirrir2Activity();
        }else{
            toast("取消镜像");
        }
    }

    public void toast(String msg) {
        Toast.makeText(TvActivity.this, msg, Toast.LENGTH_SHORT).show();
        Log.i("Toast",msg);
    }

    private void goMirrir2Activity(){
        Intent intent = new Intent(TvActivity.this,Mirror2Activity.class);
        startActivity(intent);

    }

    private void requestAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (! Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_ALERTWINDOW_PERMISSION_CODE);
            }else{
                ReturnMsg msg =  MYOUController.of(TvActivity.this).getDpsMirror().Start(mirrorCfg);
                if(!msg.isOk){ toast(msg.errMsg);  }
            }
        }
    }
}
