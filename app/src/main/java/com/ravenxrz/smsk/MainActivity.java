package com.ravenxrz.smsk;

import android.Manifest;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{

    private final String TAG = MainActivity.class.getSimpleName();

    private final String[] permissions = new String[]{Manifest.permission.READ_SMS};

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listview);

        // 开始分析
        callAnalyzer();
    }

    private void callAnalyzer() {
        if(!EasyPermissions.hasPermissions(this,permissions)) {
            // 如果没有权限，就需要申情
            EasyPermissions.requestPermissions(this, "需要获取读取短信权限", 1, permissions);
        }else{
            doAnalysis();
        }
    }

    private void doAnalysis(){
        List<String> smsList = getSmsBody();
        Set<String> names ;

        if(0 == smsList.size()){
            Toast.makeText(this,"未检测到短信信息",Toast.LENGTH_SHORT).show();
            return;
        }else{
            names = analyisRegisterInfos(smsList);

            // 更新listview
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    MainActivity.this, android.R.layout.simple_list_item_1, names.toArray(new String[0]));
            listView.setAdapter(adapter);
        }
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        // 权限申请成功
        doAnalysis();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        // 权限申请失败
        Toast.makeText(this, "权限获取失败，正在退出", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 将结果转发到easypermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 获取短信
     * @return 如果成功，返回短信的列表
     *          如果失败,返回短信列表，但size为0
     */
    public List<String> getSmsBody() {

        Cursor cursor = null;
        List<String> smsList = new ArrayList<>();

        try {
            cursor = getContentResolver().query(
                    Uri.parse("content://sms/inbox"),
                    new String[]{"body"},
                    null, null, "date desc"
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String body = cursor.getString(cursor.getColumnIndex("body"));// 在这里获取短信信息
                    smsList.add(body);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return smsList;
    }

    /**
     * 由给定的信息来分析出注册过哪些app或网站
     * @param smsList 信息集合
     * @return 注册过哪些app或者网站的名字
     */
    private Set<String> analyisRegisterInfos(List<String> smsList){
       Set<String> names = new HashSet<>() ;

       // 过滤掉未包含“验证码”的数据
        Iterator<String> iter = smsList.iterator();
        String currentElement;
        while (iter.hasNext()){
            currentElement = iter.next();
            if(!currentElement.contains("验证码")){
               iter.remove();
            }
        }

        // 如果过滤后发现已经没有包含验证码的信息
        if(0 == smsList.size()){
            return names;
        }

        // 采用正则表达式来捕获注册过的公式
        String pattern = "(\\[|\\【)(.*?)(\\]|\\】)";
        Pattern r = Pattern.compile(pattern);
        Matcher m;

        for (String msg : smsList){
           m = r.matcher(msg);
           if(m.find()){
                names.add(m.group(2));
           }
        }

        return names;
    }


}
