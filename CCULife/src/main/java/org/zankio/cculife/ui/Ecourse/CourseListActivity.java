package org.zankio.cculife.ui.Ecourse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;

import org.zankio.cculife.CCUService.ecourse.Ecourse;
import org.zankio.cculife.CCUService.kiki.Kiki;
import org.zankio.cculife.Debug;
import org.zankio.cculife.R;
import org.zankio.cculife.override.AsyncTaskWithErrorHanding;
import org.zankio.cculife.ui.Base.BaseActivity;

public class CourseListActivity extends BaseActivity {

    public static Ecourse ecourse = null;
    private CourseAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_courselist);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        adapter = new CourseAdapter();

        ListView courselist = (ListView)findViewById(R.id.courselist);
        courselist.setAdapter(adapter);
        courselist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                final Ecourse.Course course = (Ecourse.Course) parent.getAdapter().getItem(position);
                final Toast toast = Toast.makeText(CourseListActivity.this, "請稍後...", Toast.LENGTH_SHORT);
                toast.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ecourse.switchCourse(course);
                        startActivity(new Intent(CourseListActivity.this, CourseActivity.class));
                        toast.cancel();
                    }
                }).start();
            }
        });

        setMessageView(R.id.courselist);
        setSSOService(new org.zankio.cculife.CCUService.portal.service.Ecourse());

        if(ecourse != null) {
            ecourse.openSource();
        }
        new LoadDataAsyncTask().execute();

    }

    @Override
    protected void onPause() {
        if(ecourse != null) ecourse.closeSource();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if(ecourse != null) ecourse.openSource();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.course_list, menu);
        return true;
    }

    public class LoadDataAsyncTask extends AsyncTaskWithErrorHanding<Void, Void, Ecourse.Course[]> {

        @Override
        protected void onError(String msg) {
            showMessage(msg);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showMessage("讀取中...", true);
        }

        @Override
        protected void _onPostExecute(Ecourse.Course[] result) {
            if(result == null || result.length == 0) {
                showMessage("沒有課程");
                return;
            }

            adapter.setCourses(result);
            hideMessage();
        }

        @Override
        protected Ecourse.Course[] _doInBackground(Void... params) throws Exception {
            if(ecourse == null) ecourse = new Ecourse(CourseListActivity.this);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(CourseListActivity.this);
            if (Debug.debug && preferences.getBoolean("debug_ecourse_custom", false)) {
                int year, term;
                year = Integer.parseInt(preferences.getString("debug_ecourse_year", "-1"));
                term = Integer.parseInt(preferences.getString("debug_ecourse_term", "-1"));
                return ecourse.getCourses(year, term, new Kiki(CourseListActivity.this));
            }
            return ecourse.getCourses();
        }
    }

    public class CourseAdapter extends BaseAdapter {

        Ecourse.Course[] courses = null;

        public void setCourses(Ecourse.Course[] courses){
            this.courses = courses;
            this.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return courses == null ? 0 : courses.length;
        }

        @Override
        public Object getItem(int position) {
            return courses == null ? null : courses[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(CourseListActivity.this);
            LayoutInflater inflater = (LayoutInflater) CourseListActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = convertView;
            if(view == null) {
                view = inflater.inflate(R.layout.item_course, null);
            }

            Ecourse.Course course = courses[position];
            ((TextView) (view.findViewById(R.id.course_name))).setText(course.name + "");

            ((TextView)view.findViewById(R.id.unread)).setText(course.notice + course.homework + course.exam + "");
            if (!preferences.getBoolean("ignore_ecourse_warnning", false)) {
                view.findViewById(R.id.warring).setBackgroundColor( course.warning ? getResources().getColor(R.color.Red_Course_Warring) : 0);
            }
            return view;
        }
    }
    
}
