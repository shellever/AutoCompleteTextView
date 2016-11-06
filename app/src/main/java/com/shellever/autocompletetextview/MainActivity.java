package com.shellever.autocompletetextview;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MAX_HISTORY_COUNT = 50;                    // 最大的历史记录数
    private static final String SP_NAME = "recent_history";             //
    private static final String SP_KEY_SEARCH = "history_search";       //
    private static final String SP_KEY_CUSTOM = "history_custom";       //
    private static final String SP_SEPARATOR = ":-P";                   // 分隔每条历史记录"/-_0_-\\\\"
    private static final String SP_EMPTY_TAG = "<empty>";               // 空白记录标识

    private Button mSaveBtn;
    private Button mClearBtn;

    private AutoCompleteTextView mSearchTv;
    private ArrayAdapter<String> mSearchAdapter;

    private ImageView mDeleteIv;
    private AutoCompleteTextView mCustomTv;
    private AutoCompleteAdapter mCustomAdapter;
    private static final int MAX_ONCE_MATCHED_ITEM = 5;                 // 提示框最多要显示的记录行数
    private static int simpleItemHeight;                                // 单行item的高度值
    private static int prevCount = -1;                                  // 上一次记录行数
    private static int curCount = -1;                                   // 当前记录行数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSaveBtn = (Button) findViewById(R.id.btn_save);
        mClearBtn = (Button) findViewById(R.id.btn_clear);

        mSaveBtn.setOnClickListener(this);
        mClearBtn.setOnClickListener(this);

        initSearchView();
        initCustomView();

        // ??原生的Java在含有空格的字符串中无法转换成小写字母，直接返回原来的字符串
        TextView mTestTv = (TextView) findViewById(R.id.tv_test);
        String test = "Hello World And Hello Me, I'm Mr.DJ.";
        mTestTv.setText(test.toLowerCase());        // hello world and hello me, i'm mr.dj.
    }

    private void initSearchView() {
        mSearchTv = (AutoCompleteTextView) findViewById(R.id.tv_search);
        String[] mSearchHistoryArray = getHistoryArray(SP_KEY_SEARCH);
        mSearchAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                mSearchHistoryArray
        );
        mSearchTv.setAdapter(mSearchAdapter);  // 设置适配器

        // 设置下拉提示框的高度为200dp
        // mAutoCompleteTv.setDropDownHeight();      // 或XML中为android:dropDownHeight="200dp"

        // 默认当输入2个字符以上才会提示， 现在当设置输入1个字符就自动提示
        // mAutoCompleteTv.setThreshold(1);          // 或XML中为android:completionThreshold="1"

        // 设置下拉提示框中底部的提示
        // mAutoCompleteTv.setCompletionHint("最近的5条记录");

        // 设置单行输入限制
        // mAutoCompleteTv.setSingleLine(true);
    }

    private void initCustomView() {
        mCustomTv = (AutoCompleteTextView) findViewById(R.id.tv_custom);
        mDeleteIv = (ImageView) findViewById(R.id.iv_custom);
        mDeleteIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCustomTv.setText("");              // 清空TextView的内容
            }
        });

        ArrayList<String> mOriginalValues = new ArrayList<>();
        String[] mCustomHistoryArray = getHistoryArray(SP_KEY_CUSTOM);
        mOriginalValues.addAll(Arrays.asList(mCustomHistoryArray));     // String[] => ArrayList<String>
//        for (String s : mCustomHistoryArray) {
//            mOriginalValues.add(s);
//        }
        mCustomAdapter = new AutoCompleteAdapter(this, mOriginalValues);
        mCustomAdapter.setDefaultMode(AutoCompleteAdapter.MODE_STARTSWITH | AutoCompleteAdapter.MODE_SPLIT);    // 设置匹配模式

        simpleItemHeight = mCustomAdapter.getSimpleItemHeight();
        Toast.makeText(this, "simpleItemHeight: " + simpleItemHeight, Toast.LENGTH_SHORT).show(); // 103

        mCustomAdapter.setOnFilterResultsListener(new AutoCompleteAdapter.OnFilterResultsListener() {
            @Override
            public void onFilterResultsListener(int count) {
                curCount = count;
                if (count > MAX_ONCE_MATCHED_ITEM) {        // 限制提示框最多要显示的记录行数
                    curCount = MAX_ONCE_MATCHED_ITEM;
                }
                if (curCount != prevCount) {                // 仅当目前的数目和之前的不同才重新设置下拉框高度，避免重复设置
                    prevCount = curCount;
                    mCustomTv.setDropDownHeight(simpleItemHeight * curCount);
                }
            }
        });

        mCustomAdapter.setOnSimpleItemDeletedListener(new AutoCompleteAdapter.OnSimpleItemDeletedListener() {
            @Override
            public void onSimpleItemDeletedListener(String value) {
                String old_history = getHistoryFromSharedPreferences(SP_KEY_CUSTOM);    // 获取之前的记录
                String new_history = old_history.replace(value + SP_SEPARATOR, "");     // 用空字符串替换掉要删除的记录
                saveHistoryToSharedPreferences(SP_KEY_CUSTOM, new_history);             // 保存修改过的记录
            }
        });

        mCustomTv.setAdapter(mCustomAdapter);       //
        mCustomTv.setThreshold(1);                  //

        // 设置下拉时显示的提示行数 (此处不设置也可以，因为在AutoCompleteAdapter中有专门的事件监听来实时设置提示框的高度)
        // mCustomTv.setDropDownHeight(simpleItemHeight * MAX_ONCE_MATCHED_ITEM);
    }

    private String[] getHistoryArray(String key) {
        String[] array = getHistoryFromSharedPreferences(key).split(SP_SEPARATOR);
        if (array.length > MAX_HISTORY_COUNT) {         // 最多只提示最近的50条历史记录
            String[] newArray = new String[MAX_HISTORY_COUNT];
            System.arraycopy(array, 0, newArray, 0, MAX_HISTORY_COUNT); // 实现数组间的内容复制
        }
        return array;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save:
                if (getCurrentFocusedViewId() == R.id.tv_search) {
                    // saveSearchHistory();
                    saveHistory(mSearchTv, SP_KEY_SEARCH, "Search");
                } else if (getCurrentFocusedViewId() == R.id.tv_custom) {
                    // saveCustomHistory();
                    saveHistory(mCustomTv, SP_KEY_CUSTOM, "Custom");
                }
                break;
            case R.id.btn_clear:
                mSearchAdapter.clear();                 // 实时清除下拉提示框中的历史记录
                mCustomAdapter.clear();
                clearHistoryInSharedPreferences();      // 试试清除历史记录
                Toast.makeText(this, "All histories cleared", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void saveSearchHistory() {
        String text = mSearchTv.getText().toString().trim();       // 获取搜索框文本信息
        if (TextUtils.isEmpty(text)) {                      // null or ""
            Toast.makeText(this, "Please type something again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String old_text = getHistoryFromSharedPreferences(SP_KEY_SEARCH);    // 获取SP中保存的历史记录
        StringBuilder sb;
        if (SP_EMPTY_TAG.equals(old_text)) {
            sb = new StringBuilder();
        } else {
            sb = new StringBuilder(old_text);
        }
        sb.append(text + SP_SEPARATOR);      // 使用逗号来分隔每条历史记录

        // 判断搜索内容是否已存在于历史文件中，已存在则不再添加
        if (!old_text.contains(text + SP_SEPARATOR)) {
            saveHistoryToSharedPreferences(SP_KEY_SEARCH, sb.toString());  // 实时保存历史记录
            mSearchAdapter.add(text);        // 实时更新下拉提示框中的历史记录
            Toast.makeText(this, "Search saved: " + text, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Search existed: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCustomHistory() {
        String text = mCustomTv.getText().toString().trim();     // 获取搜索框信息
        if (TextUtils.isEmpty(text)) {          // null or ""
            Toast.makeText(this, "Please type something again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String old_text = getHistoryFromSharedPreferences(SP_KEY_CUSTOM);    // 获取SP中保存的历史记录
        StringBuilder sb;
        if (SP_EMPTY_TAG.equals(old_text)) {
            sb = new StringBuilder();
        } else {
            sb = new StringBuilder(old_text);
        }
        sb.append(text + SP_SEPARATOR);      // 使用逗号来分隔每条历史记录

        // 判断搜索内容是否已存在于历史文件中，已存在则不再添加
        if (!old_text.contains(text + SP_SEPARATOR)) {
            saveHistoryToSharedPreferences(SP_KEY_CUSTOM, sb.toString());  // 实时保存历史记录
            mCustomAdapter.add(text);        // 实时更新下拉提示框中的历史记录
            Toast.makeText(this, "Custom saved: " + text, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Custom existed: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveHistory(AutoCompleteTextView view, String key, String tip) {
        String text = view.getText().toString().trim();     // 去掉前后的空白符
        if (TextUtils.isEmpty(text)) {      // null or ""
            Toast.makeText(this, "Please type something again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String old_text = getHistoryFromSharedPreferences(key);    // 获取SP中保存的历史记录
        StringBuilder sb;
        if (SP_EMPTY_TAG.equals(old_text)) {
            sb = new StringBuilder();
        } else {
            sb = new StringBuilder(old_text);
        }
        sb.append(text + SP_SEPARATOR);      // 使用逗号来分隔每条历史记录

        // 判断搜索内容是否已存在于历史文件中，已存在则不再添加
        if (!old_text.contains(text + SP_SEPARATOR)) {
            saveHistoryToSharedPreferences(key, sb.toString());  // 实时保存历史记录
            if ("Search".equals(tip)) {
                mSearchAdapter.add(text);        // 实时更新下拉提示框中的历史记录
            } else if ("Custom".equals(tip)) {
                mCustomAdapter.add(text);        // 实时更新下拉提示框中的历史记录
            }
            Toast.makeText(this, tip + " saved: " + text, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, tip + " existed: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    // 获取当前获取到焦点的控件的id
    private int getCurrentFocusedViewId() {
        return this.getWindow().getDecorView().findFocus().getId();
    }

    private String getHistoryFromSharedPreferences(String key) {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        return sp.getString(key, SP_EMPTY_TAG);
    }

    private void saveHistoryToSharedPreferences(String key, String history) {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, history);
        editor.apply();
    }

    private void clearHistoryInSharedPreferences() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
    }
}
