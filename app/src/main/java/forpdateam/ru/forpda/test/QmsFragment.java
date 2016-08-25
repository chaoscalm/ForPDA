package forpdateam.ru.forpda.test;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trello.rxlifecycle.FragmentEvent;

import java.util.ArrayList;
import java.util.Date;

import forpdateam.ru.forpda.R;
import forpdateam.ru.forpda.api.Api;
import forpdateam.ru.forpda.api.qms.models.QmsChatItem;
import forpdateam.ru.forpda.api.qms.models.QmsContact;
import forpdateam.ru.forpda.api.qms.models.QmsTheme;
import forpdateam.ru.forpda.fragments.TabFragment;
import forpdateam.ru.forpda.utils.ErrorHandler;
import forpdateam.ru.forpda.utils.IntentHandler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by radiationx on 03.08.16.
 */
public class QmsFragment extends TabFragment {
    private static final String LINk = "http://4pda.ru/forum/index.php?&act=qms-xhr&action=userlist";

    private Date date;
    private TextView text;
    private LinearLayout container;
    private EditText searchText;
    private Button search;

    @Override
    public String getTabUrl() {
        return LINk;
    }

    @Override
    public boolean isAlone() {
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initBaseView(inflater, container);
        inflater.inflate(R.layout.activity_newslist, (ViewGroup) view.findViewById(R.id.fragment_content), true);
        text = (TextView) findViewById(R.id.textView2);
        this.container = (LinearLayout) findViewById(R.id.container);
        findViewById(R.id.search_field).setVisibility(View.VISIBLE);
        searchText = (EditText) findViewById(R.id.search);
        search = (Button) findViewById(R.id.search_nick);
        search.setOnClickListener(view -> search(searchText.getText().toString()));
        date = new Date();
        return view;
    }

    @Override
    public void loadData() {
        getCompositeSubscription().add(Api.Qms().getContactList()
                .onErrorReturn(throwable -> {
                    ErrorHandler.handle(this, throwable, view1 -> loadData());
                    return new ArrayList<>();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::bindUi, throwable -> {
                    Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void loadThreads(String url) {
        getCompositeSubscription().add(Api.Qms().getThemesList(url)
                .onErrorReturn(throwable -> {
                    throwable.printStackTrace();
                    return new ArrayList<>();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(this::addText, throwable -> {
                    Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void loadChat(String url) {
        getCompositeSubscription().add(Api.Qms().getChat(url)
                .onErrorReturn(throwable -> {
                    throwable.printStackTrace();
                    return new ArrayList<>();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(this::showChat, throwable -> {
                    Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void search(String nick) {
        getCompositeSubscription().add(Api.Qms().search(nick)
                .onErrorReturn(throwable -> {
                    throwable.printStackTrace();
                    return new String[]{};
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(this::showResult, throwable -> {
                    Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void bindUi(ArrayList<QmsContact> contacts) {
        String temp = "";
        if (contacts != null) {
            for (QmsContact contact : contacts) {
                //temp+=contact.getNick()+"\n";
                Button button = new Button(getContext());
                button.setText(contact.getNick() + (contact.getCount().isEmpty() ? "" : " : " + contact.getCount()));
                button.setOnClickListener(view -> {
                    mid = contact.getId();
                    loadThreads("http://4pda.ru/forum/index.php?act=qms&mid=" + mid);
                });
                button.setOnClickListener(view1 -> {
                    IntentHandler.handle("http://4pda.ru/forum/index.php?showuser=" + contact.getId());
                });
                container.addView(button);
            }
        }

        Log.d("kek", "time: " + (new Date().getTime() - date.getTime()));
        text.setText(temp);
    }


    String mid, tid;
    int lastThreads = -1;

    private void addText(ArrayList<QmsTheme> threads) {
        //String temp = "";
        if (threads != null) {
            if (lastThreads != -1) {
                for (int i = 0; i < lastThreads; i++) {
                    container.removeViewAt(0);
                }
            }
            lastThreads = threads.size();
            for (QmsTheme thread : threads) {
                //temp += thread.getName() + (thread.getCountNew().isEmpty() ? "" : " : " + thread.getCountNew() + " /") + " " + thread.getCountMessages() + "\n";
                Button button = new Button(getContext());
                button.setBackgroundColor(Color.parseColor("#55ff55"));
                button.setText(thread.getName() + (thread.getCountNew().isEmpty() ? "" : " : " + thread.getCountNew() + " /") + " " + thread.getCountMessages());
                button.setOnClickListener(view -> loadChat("http://4pda.ru/forum/index.php?act=qms&mid=" + mid + "&t=" + thread.getId()));
                container.addView(button, 0);
            }
        }
        //text.setText(temp);
    }

    private void showChat(ArrayList<QmsChatItem> threads) {
        Log.d("kekos", threads.size() + " size");
        String temp = "";
        if (threads != null) {
            for (QmsChatItem item : threads) {
                temp += (item.isDate() ? item.getDate() : ((item.getWhoseMessage() ? "Я" : "Он") + ":\n") + item.getContent()) + "\n\n";
            }
        }
        text.setText(temp);
    }

    private void showResult(String[] res) {
        String temp = "";
        if (res != null) {
            for (String nick : res) {
                temp += nick + "\n";
            }
        }
        Toast.makeText(getContext(), temp, Toast.LENGTH_SHORT).show();
    }
}
