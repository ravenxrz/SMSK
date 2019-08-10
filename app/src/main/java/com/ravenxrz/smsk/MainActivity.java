package com.ravenxrz.smsk;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.EasyPermissions;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, AdapterView.OnItemClickListener {

    private final String TAG = MainActivity.class.getSimpleName();

    private final String[] permissions = new String[]{Manifest.permission.READ_SMS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private ListView listView;
    // listview的adapter
    private ArrayAdapter<String> adapter;

    // 过滤后的短信列表
    Set<String> names = new HashSet<>();

    // 导出的路径
    private String exportPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
            .getAbsolutePath()+"/sms_export.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listview);
        listView.setOnItemClickListener(this);

        // 开始分析
        callAnalyzer();
    }

    /**
     * 调用分析器，判断是否有读取短信的权限,如果有，再调用真实的分析函数-doAnalysis()
     */
    private void callAnalyzer() {
        if(!EasyPermissions.hasPermissions(this,permissions)) {
            // 如果没有权限，就需要申情
            EasyPermissions.requestPermissions(this, "需要获取读取短信权限", 1, permissions);
        }else{
            doAnalysis();
        }
    }

    /**
     * 真实做分析的函数，包括分析sms，然后显示在listview上
     */
    private void doAnalysis(){
        List<String> smsList = getSmsBody();

        if(0 == smsList.size()){
            Toast.makeText(this,"未检测到短信信息",Toast.LENGTH_SHORT).show();
        }else{
            names = analyisRegisterInfos(smsList);

            // 更新listview
           adapter = new ArrayAdapter<String>(
                    MainActivity.this, android.R.layout.simple_list_item_1, names.toArray(new String[0]));
            listView.setAdapter(adapter);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String name = adapter.getItem(i);

        Intent intent = new Intent(this,WebActivity.class);
        intent.putExtra("keyword",name);
        startActivity(intent);
    }

    /**
     * 创造menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("导出数据");
        return true;
    }



    /**
     * menu菜单点击后的回调函数
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // 只有一个menu，不需要做判断
        if(0 == names.size()){
            Toast.makeText(this,"当前数据为空，无法导出",Toast.LENGTH_SHORT).show();
            return false;
        }

        // 尝试
        boolean isSaved;
        isSaved = saveNames(names);
        if (!isSaved){
            Toast.makeText(this, "导出数据时出错，请重试", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            Toast.makeText(this, "已成功导出,请在"+exportPath+"中查看", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 将获取的名字保存下来
     * @param names
     * @return
     */
    private boolean saveNames(Set<String> names){
        if(exportPath == null || "".equals(exportPath)){
            return false;
        }

        try {
            // 创建文件
            File file = new File(exportPath);
            if(!file.exists() && !file.createNewFile()) {
                return false;
            }

            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for(String name : names) {
                bw.write(name);
                bw.write("\n");
            }
            bw.flush();

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
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
