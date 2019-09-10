package com.ambinusian.adab.ui.student;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ambinusian.adab.all.NextClassActivity;
import com.ambinusian.adab.manager.APIManager;
import com.ambinusian.adab.manager.NetworkHelper;
import com.ambinusian.adab.preferences.UserPreferences;
import com.ambinusian.adab.recyclerview.course.CourseAdapter;
import com.ambinusian.adab.recyclerview.course.CourseModel;
import com.ambinusian.adab.R;
import com.ambinusian.adab.recyclerview.discussion.DiscussionAdapter;
import com.ambinusian.adab.recyclerview.discussion.DiscussionModel;
import com.ambinusian.adab.utility.TextUtility;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class FragmentHome extends Fragment {

    private RecyclerView coursesRecyclerView, discussionRecyclerView;
    private ArrayList<CourseModel> coursesList;
    private ArrayList<DiscussionModel> discussionList;
    private LinearLayout liveLayout, welcomeLayout;
    private ImageView liveClassIcon;
    private LinearLayoutManager linearLayoutManager;
    private TextView liveClassTitle, liveCourse, liveClassMeeting;
    private TextView welcomeTitle;
    private ImageView nextClassIcon;
    private TextView nextClassTime, nextClassTitle, nextCourse, nextClassSession;
    private Chip nextCourseCode, nextClassCode, nextClassType;
    private Boolean hasLiveClass;
    private MaterialButton seeAllLatestClass, seeAllNextClass, seeAllDiscussion;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_student_all_classes, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        coursesRecyclerView = view.findViewById(R.id.rv_courses);
        discussionRecyclerView = view.findViewById(R.id.rv_discussions);
        liveLayout = view.findViewById(R.id.liveLayout);
        liveClassIcon = view.findViewById(R.id.liveClassIcon);
        liveClassTitle = view.findViewById(R.id.tv_liveClassTitle);
        liveCourse = view.findViewById(R.id.tv_liveCourse);
        liveClassMeeting = view.findViewById(R.id.tv_liveClassMeeting);
        welcomeLayout = view.findViewById(R.id.welcomeLayout);
        welcomeTitle = view.findViewById(R.id.tv_welcomeTitle);
        nextClassIcon = view.findViewById(R.id.nextClassIcon);
        nextClassTime = view.findViewById(R.id.tv_nextClassTime);
        nextClassTitle = view.findViewById(R.id.tv_nextClassTitle);
        nextCourse = view.findViewById(R.id.tv_nextCourse);
        nextClassSession = view.findViewById(R.id.tv_nextClassSession);
        nextCourseCode = view.findViewById(R.id.chip_nextCourseCode);
        nextClassCode = view.findViewById(R.id.chip_nextClassCode);
        nextClassType = view.findViewById(R.id.chip_nextClassType);
        nextClassType = view.findViewById(R.id.chip_nextClassType);
        seeAllNextClass = view.findViewById(R.id.see_all_next_class);
        seeAllLatestClass = view.findViewById(R.id.see_all_latest_class);
        seeAllDiscussion = view.findViewById(R.id.see_all_discussion);
        coursesList = new ArrayList<>();
        discussionList = new ArrayList<>();
        hasLiveClass = false;

        // set visibility gone
        coursesRecyclerView.setVisibility(View.GONE);

        // get classes data
        APIManager apiManager = new APIManager(getContext());
        UserPreferences userPreferences = new UserPreferences(getContext());
        apiManager.getUserClasses(userPreferences.getUserToken(), new NetworkHelper.getUserClasses() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onResponse(Boolean success, Map<String, Object>[] userClasses) {
                if (success) {
                    //set layout manager for recycler view
                    linearLayoutManager = new LinearLayoutManager(getContext());
                    linearLayoutManager.setReverseLayout(true);
                    coursesRecyclerView.setLayoutManager(linearLayoutManager);

                    for (Map<String, Object> userClass : userClasses) {

                        if ((int) userClass.get("is_done") == 1) {

                            // parse date
                            String classDate = "";
                            String classTime = "";
                            try {
                                Date startDate = new SimpleDateFormat("yyyy-MM-dd")
                                        .parse((String) userClass.get("transaction_date"));
                                classDate = new SimpleDateFormat("EEEE, d MMMM yyyy")
                                        .format(startDate);

                                Date startTime = new SimpleDateFormat("HH:mm:ss")
                                        .parse((String) userClass.get("transaction_time"));
                                classTime = new SimpleDateFormat("HH:mm")
                                        .format(startTime);

                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            //set list data for recycler view
                            coursesList.add(new CourseModel(
                                    (int) userClass.get("transaction_Id"),
                                    (int) userClass.get("class_icon"),
                                    classDate + " " + classTime,
                                    (String) userClass.get("topic"),
                                    (String) userClass.get("course_name"),
                                    getString(R.string.class_session) + " " + userClass.get("session"),
                                    (String) userClass.get("course_code"),
                                    (String) userClass.get("class_code"),
                                    (String) userClass.get("class_type")
                            ));
                        }

                        if ((int) userClass.get("is_live") == 1) {
                            liveLayout.setVisibility(View.VISIBLE);
                            welcomeLayout.setVisibility(View.GONE);
                            liveClassIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_class_59_pencilpaper));
                            liveClassTitle.setText((String) userClass.get("topic"));
                            liveCourse.setText((String) userClass.get("course_name"));
                            String meeting = getString(R.string.class_session) + " " + userClass.get("session");
                            liveClassMeeting.setText(meeting);

                            liveLayout.setOnClickListener(v -> {
                                Intent intent = new Intent(getContext(), ActivityLive.class);
                                int classId = (int) userClass.get("transaction_Id");

                                //set all data to bundle
                                Bundle bundle = new Bundle();
                                bundle.putInt("class_id", classId);

                                //set bundle to the intent
                                intent.putExtras(bundle);

                                //go to ActivityLive
                                startActivity(intent);
                            });

                            hasLiveClass = true;
                        }
                    }

                    //set adapter for recycler view
                    coursesRecyclerView.setAdapter(new CourseAdapter(getContext(), coursesList));

                    //set visible
                    coursesRecyclerView.setVisibility(View.VISIBLE);

                    //show welcome page if no any class is live
                    if (!hasLiveClass) {
                        welcomeLayout.setVisibility(View.VISIBLE);
                        liveLayout.setVisibility(View.GONE);
                        welcomeTitle.setText(getString(R.string.welcome_title, TextUtility.toTitleCase(userPreferences.getUserName().toUpperCase())));
                    }

                }
            }


            @Override
            public void onError(int errorCode, String errorReason) {

            }
        });

        /*
        Discussion
        Fill using dummy data
        */

        discussionList.add(new DiscussionModel("Thursday, 22 June 2019", "Saya Binusian 2022", "Chandra", "on Storage(MOBI6009)"));
        discussionList.add(new DiscussionModel("Thursday, 22 June 2019", "Saya Binusian 2022", "Chandra", "on Storage(MOBI6009)"));
        discussionList.add(new DiscussionModel("Thursday, 22 June 2019", "Saya Binusian 2022", "Chandra", "on Storage(MOBI6009)"));

        //Set layout manager and adapter for discussionRecyclerView
        linearLayoutManager = new LinearLayoutManager(getContext());
        discussionRecyclerView.setLayoutManager(linearLayoutManager);

        DiscussionAdapter adapter = new DiscussionAdapter(getContext(), discussionList);
        discussionRecyclerView.setAdapter(adapter);

        //Next Class
        //TODO: change this fill using dummy data
        nextClassIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_class_56_pencilnote));
        nextClassTime.setText("Monday, 26 August 2019");
        nextClassTitle.setText("Storage");
        nextCourse.setText("Mobile Object Oriented Programming");
        nextClassSession.setText("Session 9");
        nextCourseCode.setText("MOBI6002");
        nextClassCode.setText("LA03");
        nextClassType.setText("LEC");

        // see all the next classes
        seeAllNextClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), NextClassActivity.class);
                getActivity().startActivity(intent);
            }
        });

        //see all the latetest classes
        seeAllLatestClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        seeAllDiscussion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show fragment discussion
            }
        });


    }


}