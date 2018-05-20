package inc.loop.room;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;

public class StartActivity extends AppCompatActivity
{
    ParseRooms parseRooms;

    Button start;
    SeekBar room_size;
    Switch room_mode;

    public static int room_id;
    public static String username;

    public static final String APP_PREFERENCES = "settings";

    public static final String APP_PREFERENCES_ROOM_ID = "room_id";
    public static final String APP_PREFERENCES_USERNAME = "username";

    SharedPreferences mSettings;

    public static String randomWord(int length) {
        Random random = new Random();
        StringBuilder word = new StringBuilder(length);
        for (int i = 0; i < length; i++)
        {
            word.append((char)('a' + random.nextInt(26)));
        }

        return word.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        room_size = findViewById(R.id.seekBar);
        room_mode = findViewById(R.id.roomMode);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        if (mSettings.getInt(APP_PREFERENCES_ROOM_ID, 0) > 0 && !mSettings.getString(APP_PREFERENCES_USERNAME, "").equals(""))
        {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
        }

        start = findViewById(R.id.startButton);
        start.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                parseRooms = new ParseRooms(StartActivity.this);

                parseRooms.execute((room_size.getProgress() + 1) * 4 + "", Boolean.toString(room_mode.isChecked()));
            }
        });
    }

    public class ParseRooms extends AsyncTask<String, Void, Integer>
    {
        private ProgressDialog dialog;
        Context context;

        public ParseRooms(Context context)
        {
            this.dialog = new ProgressDialog(context);
            this.context = context;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            dialog.setMessage("Please wait...");
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Integer doInBackground(String... strings)
        {
            ConnectivityManager cm = (ConnectivityManager)getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected() && activeNetwork.isAvailable())
            {
                HttpRequest request = HttpRequest.get("https://roomapi.herokuapp.com/rooms");
                request.connectTimeout(1000);
                if (request.code() == 200)
                {
                    try
                    {
                        String json_rooms = request.body();

                        JSONArray array_of_sorted = new JSONArray();

                        JSONArray list_of_rooms = new JSONArray(json_rooms);
                        for (int i = 0; i < list_of_rooms.length(); i++)
                        {
                            JSONObject this_room = list_of_rooms.getJSONObject(i);
                            JSONArray users_in_this_room = this_room.getJSONArray("users");

                            if (this_room.getString("size").equals(strings[0]) &&
                                    this_room.getString("mode").equals(strings[1]) &&
                                    this_room.getInt("size") > users_in_this_room.length())
                            {
                                array_of_sorted.put(this_room);
                            }
                        }

                        if (array_of_sorted.length() > 0)
                        {
                            JSONObject first_required = (JSONObject) array_of_sorted.get(0);
                            return first_required.getInt("id");
                        }
                        else
                        {
                            Map<String, String> data = new HashMap<>();
                            data.put("size", strings[0]);
                            data.put("mode", strings[1]);

                            HttpRequest postRequest = HttpRequest.post("https://roomapi.herokuapp.com/rooms").form(data);
                            Log.w("body of post", postRequest.body());
                            // тело post-реквеста не нужно
                            HttpRequest getRequest = HttpRequest.get("https://roomapi.herokuapp.com/rooms");

                            // 2nd round

                            String json_new_rooms = getRequest.body();

                            JSONArray new_array_of_sorted = new JSONArray();

                            JSONArray new_list_of_rooms = new JSONArray(json_new_rooms);
                            for (int i = 0; i < new_list_of_rooms.length(); i++)
                            {
                                JSONObject this_room = new_list_of_rooms.getJSONObject(i);
                                JSONArray users_in_this_room = this_room.getJSONArray("users");

                                if (this_room.getString("size").equals(strings[0]) &&
                                        this_room.getString("mode").equals(strings[1]) &&
                                        this_room.getInt("size") > users_in_this_room.length())
                                {
                                    new_array_of_sorted.put(this_room);
                                }
                            }

                            if (new_array_of_sorted.length() > 0)
                            {
                                JSONObject new_first_required = (JSONObject) new_array_of_sorted.get(0);
                                return new_first_required.getInt("id");
                            }
                            else
                            {
                                return -4;
                            }
                        }
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();

                        return -3;
                    }
                }
                else
                {
                    return -2;
                }
            }
            else
            {
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer integer)
        {
            super.onPostExecute(integer);

            dialog.dismiss();

            room_id = integer;
            username = randomWord(8);

            RequestParams params = new RequestParams();
            params.put("name", username);

            if (room_id > 0)
            {
                AsyncHttpClient client = new AsyncHttpClient();
                client.post("https://roomapi.herokuapp.com/rooms/" + room_id + "/users", params, new AsyncHttpResponseHandler()
                {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody)
                    {
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putInt(APP_PREFERENCES_ROOM_ID, room_id);
                        editor.putString(APP_PREFERENCES_USERNAME, username);
                        editor.apply();

                        Intent intent = new Intent(StartActivity.this, LoadingActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error)
                    {
                        Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }
                });
            }
            else if (room_id == -4)
            {
                Toast.makeText(getApplicationContext(), "Something went wrong. Please, try again", Toast.LENGTH_LONG).show();
            }
            else if (room_id == -3)
            {
                Toast.makeText(getApplicationContext(), "JSONException", Toast.LENGTH_LONG).show();
            }
            else if (room_id == -2)
            {
                Toast.makeText(getApplicationContext(), "Internal server error", Toast.LENGTH_LONG).show();
            }
            else if (room_id == -1)
            {
                Toast.makeText(getApplicationContext(), "Some problems with internet connection", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        System.exit(1);
    }
}