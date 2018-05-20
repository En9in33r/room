package inc.loop.room;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class ChatActivity extends AppCompatActivity
{
    ArrayList<Message> messages = new ArrayList<>();
    MessageAdapter adapter;

    ListView chatList;
    EditText msgField;
    ImageView msgSend;

    public static final String APP_PREFERENCES = "settings";

    public static final String APP_PREFERENCES_ROOM_ID = "room_id";
    public static final String APP_PREFERENCES_USERNAME = "username";

    SharedPreferences mSettings;
    SharedPreferences.Editor editor;

    int room_id;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        msgField = findViewById(R.id.editMessage);
        msgSend = findViewById(R.id.sendButton);

        adapter = new MessageAdapter(this, messages);
        chatList = findViewById(R.id.chatView);
        chatList.setAdapter(adapter);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        room_id = mSettings.getInt(APP_PREFERENCES_ROOM_ID, 0);

        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while (true)
                    {
                        ConnectivityManager cm = (ConnectivityManager)getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        if (activeNetwork != null && activeNetwork.isConnected() && activeNetwork.isAvailable())
                        {
                            sleep(1000);
                            HttpRequest request = HttpRequest.get("https://roomapi.herokuapp.com/rooms/" + room_id + "/messages");
                            JSONArray messages_array = new JSONArray(request.body());
                            if (messages.size() < messages_array.length())
                            {
                                messages.clear();

                                for (int i = 0; i < messages_array.length(); i++)
                                {
                                    final JSONObject this_element = messages_array.getJSONObject(i);
                                    handler.post(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            try
                                            {
                                                messages.add(new Message(this_element.getString("author"), this_element.getString("content")));
                                                adapter.notifyDataSetChanged();
                                            }
                                            catch (JSONException e)
                                            {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
                catch (InterruptedException | JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        msgSend.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final Handler fieldController = new Handler();
                Thread sendMessageThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ConnectivityManager cm = (ConnectivityManager)getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        if (activeNetwork != null && activeNetwork.isConnected() && activeNetwork.isAvailable())
                        {
                            Map<String, String> content = new HashMap<>();
                            content.put("author", mSettings.getString(APP_PREFERENCES_USERNAME, ""));
                            content.put("content", msgField.getText().toString());
                            HttpRequest sendRequest = HttpRequest.post("https://roomapi.herokuapp.com/rooms/" + room_id + "/messages").form(content);
                            Log.w("body", sendRequest.body());
                            fieldController.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    msgField.setText("");
                                }
                            });
                        }
                        else
                        {
                            Toast.makeText(ChatActivity.this, "Something went wrong. Please, check your internet connection",
                                    Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(ChatActivity.this, StartActivity.class);
                            startActivity(intent);
                        }
                    }
                });
                sendMessageThread.start();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.quit:
                editor = mSettings.edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(ChatActivity.this, StartActivity.class);
                startActivity(intent);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        System.exit(1);
    }
}
