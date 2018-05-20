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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static java.lang.Thread.sleep;

public class LoadingActivity extends AppCompatActivity
{
    /* В этом классе осуществляется ожидание заполнения комнаты.
    *  Из SharedPreferences достаем id комнаты и сгенерированный никнейм,
    *  ежесекундно отправляем GET-запрос с этим самым id и, когда
    *  комната заполнится, переходим в ChatActivity
    * */
    ProgressBar progressBar;
    TextView joined_widget;
    TextView room_size_widget;

    public static final String APP_PREFERENCES = "settings";
    public static final String APP_PREFERENCES_ROOM_ID = "room_id";

    SharedPreferences mSettings;

    int room_id;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        progressBar = findViewById(R.id.progressBar);
        joined_widget = findViewById(R.id.numberOfJoined);
        room_size_widget = findViewById(R.id.numberOfSize);

        room_id = mSettings.getInt(APP_PREFERENCES_ROOM_ID, 0);

        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ConnectivityManager cm = (ConnectivityManager)getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    if (activeNetwork != null && activeNetwork.isConnected() && activeNetwork.isAvailable())
                    {
                        HttpRequest request = HttpRequest.get("https://roomapi.herokuapp.com/rooms/" + room_id);

                        JSONObject current_room = new JSONObject(request.body());
                        final int room_size = current_room.getInt("size");
                        int count_of_users = 0;
                        handler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                room_size_widget.setText(room_size + "");
                                progressBar.setMax(room_size);
                            }
                        });

                        while (count_of_users < room_size)
                        {
                            sleep(1000);
                            HttpRequest request_cycle = HttpRequest.get("https://roomapi.herokuapp.com/rooms/" + room_id + "/users");
                            final JSONArray users = new JSONArray(request_cycle.body());
                            count_of_users = users.length();

                            handler.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    joined_widget.setText(users.length() + "");
                                    progressBar.setProgress(users.length());
                                }
                            });

                            Log.w("test", count_of_users + "");
                        }

                        Intent intent = new Intent(LoadingActivity.this, ChatActivity.class);
                        startActivity(intent);
                    }
                    else
                    {
                        Toast.makeText(LoadingActivity.this, "Something went wrong. Please, check your internet connection",
                                Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(LoadingActivity.this, StartActivity.class);
                        startActivity(intent);
                    }
                }
                catch (InterruptedException | JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}
