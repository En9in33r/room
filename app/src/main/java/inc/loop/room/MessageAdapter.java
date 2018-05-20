package inc.loop.room;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MessageAdapter extends BaseAdapter
{
    Context context;
    LayoutInflater inflater;
    ArrayList<Message> messages;

    public MessageAdapter(Context context, ArrayList<Message> messages)
    {
        this.context = context;
        this.messages = messages;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount()
    {
        return messages.size();
    }

    @Override
    public Object getItem(int i)
    {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = convertView;
        if (view == null)
        {
            view = inflater.inflate(R.layout.message, parent, false);
        }

        Message m = getMessage(position);

        ((TextView) view.findViewById(R.id.nicknameView)).setText(m.nickname);
        ((TextView) view.findViewById(R.id.messageView)).setText(m.msg);

        return view;
    }

    Message getMessage(int position)
    {
        return ((Message)getItem(position));
    }
}
