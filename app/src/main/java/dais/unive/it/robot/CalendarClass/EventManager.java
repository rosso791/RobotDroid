package dais.unive.it.robot.CalendarClass;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import dais.unive.it.robot.Automation.DataExchange;

public class EventManager extends AppCompatActivity {

    private static final String FILENAME = "/calendar.json";
    private static EventManager instance;

    Timer timer = new Timer();

    private List<Event> events;

    private EventManager() {
        events = EventManager.deserialize(Environment.getExternalStorageDirectory() + FILENAME);
        timer.schedule( new TimerTask() {
            public void run() {
                DataExchange dataExchange = DataExchange.GetInstance();
                    events
                    .stream()
                    .filter(e -> e.getWhen().get(Calendar.HOUR) == Calendar.getInstance().get(Calendar.HOUR)
                        && e.getWhen().get(Calendar.MINUTE) == Calendar.getInstance().get(Calendar.MINUTE)
                        && e.getDay().ordinal() == Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
                    .findFirst()
                    .ifPresent(x -> DataExchange.SetColorDischargeRequest(x.getColorCode()));
            }
        }, 0, 10*1000);
    }

    public static EventManager GetInstance() {
        if(instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    private static List<Event> deserialize(String path) {
        List<Event> events = new ArrayList<>();
        Gson unserializer = new Gson();
        try {
            File file = new File(path);
            if(file.exists()) {
                FileReader r = new FileReader(path);
                Type t = new TypeToken<List<Event>>(){}.getType();
                events = (ArrayList<Event>)unserializer.fromJson(r, t);
            }
        } catch (IOException e) {
            Toast.makeText(instance.getApplicationContext(), "Error reading a backup of the calendar", Toast.LENGTH_LONG).show();
        }
        return events;
    }
    private static void serialize(String path) {
        Gson serializer = new Gson();
        try {
            File file = new File(path);
            FileWriter w = new FileWriter(file);
            serializer.toJson(instance.events, w);
            w.flush();
            w.close();
        } catch (IOException e) {
            Toast.makeText(instance.getApplicationContext(), "Error writing a backup of the calendar", Toast.LENGTH_LONG).show();
        }
    }

    public void AddEvent(PillColors color, Event.OccurrencyType occurrencyType, Calendar when, WeekDay day) throws Exception {
        switch(occurrencyType) {
            case daily:
                for (WeekDay wday: WeekDay.values()) {
                    events.add(new Event(wday, color, when, true));
                }
                break;
            case weekly:
                if(day == null) throw new Exception("The day isn't specified for a weekly event");
                events.add(new Event(day, color, when, true));
                break;
            case onetime:
                if(day == null) throw new Exception("The day isn't specified for a onetime event");
                events.add(new Event(day, color, when, false));
                break;
        }
        serialize(Environment.getExternalStorageDirectory() + FILENAME);
    }

    public void DeleteEvent(Event e) {
        events.remove(e);
        serialize(Environment.getExternalStorageDirectory() + FILENAME);
    }

    public Map<Integer, Map<WeekDay, Event>> getAllEvents() {
        Map<Integer, Map<WeekDay, Event>> eventsList = new HashMap<>();
        events
                .forEach(e -> {
                    int when = e.getWhen().get(Calendar.HOUR) * 60 + e.getWhen().get(Calendar.MINUTE);
                    if(!eventsList.containsKey(when)) eventsList.put(when, new HashMap<>());
                    eventsList.get(when).put(e.getDay(), e);
                });
        return eventsList;
    }

}