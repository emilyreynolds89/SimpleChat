package com.codepath.simplechat;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    static final String TAG = ChatActivity.class.getSimpleName();

    static final String USER_ID_KEY = "userId";
    static final String BODY_KEY = "body";

    EditText etMessage;
    Button btSend;

    RecyclerView rvChat;
    ArrayList<Message> mMessages;
    ChatAdapter mAdapter;

    boolean mFirstLoad;

    static final int MAX_CHAT_MESSAGES_TO_SHOW = 50;

    static final int POLL_INTERVAL = 1000; // milliseconds
    Handler myHandler = new Handler();  // android.os.Handler
    Runnable mRefreshMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            refreshMessages();
            myHandler.postDelayed(this, POLL_INTERVAL);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_chat);

        if (ParseUser.getCurrentUser() != null) {
            startWithCurrentUser();
        } else {
            login();
        }

        myHandler.postDelayed(mRefreshMessagesRunnable, POLL_INTERVAL);

    }

    void startWithCurrentUser() {
        setupMessagePosting();
    }

    void login() {
        ParseAnonymousUtils.logIn(new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Anonymous login failed");
                } else {
                    startWithCurrentUser();
                }
            }
        });
    }

    void setupMessagePosting() {
        etMessage = findViewById(R.id.etMessage);
        btSend = findViewById(R.id.btSend);
        rvChat = findViewById(R.id.rvChat);

        mMessages = new ArrayList<>();
        mFirstLoad = true;
        final String userId = ParseUser.getCurrentUser().getObjectId();
        mAdapter = new ChatAdapter(ChatActivity.this, userId, mMessages);
        rvChat.setAdapter(mAdapter);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        linearLayoutManager.setReverseLayout(true);
        rvChat.setLayoutManager(linearLayoutManager);

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = etMessage.getText().toString();

                /*ParseObject message = ParseObject.create("Message");
                message.put(USER_ID_KEY, ParseUser.getCurrentUser().getObjectId());
                message.put(BODY_KEY, data);*/

                Message message = new Message();
                message.setBody(data);
                message.setUserId(ParseUser.getCurrentUser().getObjectId());

                message.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            Toast.makeText(ChatActivity.this, "Successfully created message on Parse",
                                    Toast.LENGTH_SHORT).show();
                            refreshMessages();
                        } else {
                            Log.e(TAG, "Failed to save message", e);
                        }
                    }
                });

                etMessage.setText(null);
            }
        });
    }

    void refreshMessages() {

        ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
        query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);
        query.orderByDescending("createdAt");

        query.findInBackground(new FindCallback<Message>() {
            @Override
            public void done(List<Message> messages, ParseException e) {
                if (e == null) {
                    mMessages.clear();
                    mMessages.addAll(messages);
                    mAdapter.notifyDataSetChanged();

                    if (mFirstLoad) {
                        rvChat.scrollToPosition(0);
                        mFirstLoad = false;
                    }
                } else {
                    Log.e("Message", "Error loading messages", e);
                }
            }
        });

    }

}
