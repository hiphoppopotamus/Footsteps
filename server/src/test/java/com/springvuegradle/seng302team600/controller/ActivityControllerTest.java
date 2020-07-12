package com.springvuegradle.seng302team600.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springvuegradle.seng302team600.model.Activity;
import com.springvuegradle.seng302team600.model.User;
import com.springvuegradle.seng302team600.repository.ActivityRepository;
import com.springvuegradle.seng302team600.repository.EmailRepository;
import com.springvuegradle.seng302team600.repository.UserRepository;
import com.springvuegradle.seng302team600.service.ActivityTypeService;
import com.springvuegradle.seng302team600.service.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
class ActivityControllerTest {

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private EmailRepository emailRepository;
    @MockBean
    private ActivityRepository activityRepository;
    @MockBean
    private UserValidationService userValidationService;
    @MockBean
    private ActivityTypeService activityTypeService;
    @Autowired
    private MockMvc mvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Long DEFAULT_USER_ID = 1L;
    private static final Long DEFAULT_EMAIL_ID = 1L;
    private static final Long DEFAULT_ACTIVITY_ID = 1L;
    private static Long activityCount = 0L;

    private User dummyUser1;
    private User dummyUser2; // Used when a second user is required
    private final String validToken = "valid";
    private Set<Activity> activityMockTable = new HashSet<>();


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        dummyUser1 = new User();

        // Mocking ActivityTypeService
        when(activityTypeService.getMatchingEntitiesFromRepository(Mockito.any())).thenAnswer(i -> i.getArgument(0));
    }

    /**
     * Mock ActivityType repository actions
     */
    @BeforeEach
    void setupActivityRepository() {
        activityCount = 0L;
        // Save
        when(activityRepository.save(Mockito.any(Activity.class))).thenAnswer(i -> {
            Activity newActivity = i.getArgument(0);
            ReflectionTestUtils.setField(newActivity, "activityId", DEFAULT_ACTIVITY_ID + activityCount++);
            activityMockTable.add(i.getArgument(0));
            return newActivity;
        });
        // FindByActivityId
        when(activityRepository.findByActivityId(Mockito.any(Long.class))).thenAnswer(i -> {
            for (Activity activity: activityMockTable) {
                if (activity.getActivityId() == i.getArgument(0)) {
                    return activity;
                }
            }
            return null;
        });
    }



    // ----------- Tests -----------

    private final String newActivity1Json = JsonConverter.toJson(true,
            "activity_name", "Kaikoura Coast Track race",
            "description", "A big and nice race on a lovely peninsula",
            "activity_type", new Object[]{
                    "Astronomy", "Hiking"
            },
            "continuous", false,
            "start_time", "2020-02-20T08:00:00+1300",
            "end_time", "2020-02-20T08:00:00+1300",
            "location", "Kaikoura, NZ");

    /**
     * Test successful creation of new activity.
     */
    @Test
    void newActivity() throws Exception {

        MockHttpServletRequestBuilder httpReq = MockMvcRequestBuilders.post("/profiles/{profileId}/activities", DEFAULT_USER_ID)
                .content(newActivity1Json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReq)
                .andExpect(status().isCreated())
                .andReturn();
        assertNotNull(result.getResponse());
    }

    /**
     * Test successful deletion of an activity.
     */
    @Test
    void deleteActivity() throws Exception {
        Activity activityInRepo = objectMapper.readValue(newActivity1Json, Activity.class);
        activityRepository.save(activityInRepo);

        MockHttpServletRequestBuilder httpReqDelete = MockMvcRequestBuilders.delete("/activities/{activityId}", DEFAULT_ACTIVITY_ID)
                .header("Token", validToken);
        MvcResult result = mvc.perform(httpReqDelete).andExpect(status().isOk()).andReturn();
        assertNotNull(result.getResponse());
    }

    private final String newActivityEditJson = JsonConverter.toJson(true,
            "activity_name", "Nelson Coast Track race",
            "activity_type", new Object[]{
                    "Astronomy", "Hiking"
            },
            "location", "Nelson, NZ");
    /**
     * Test successful edit/update of an activity details
     */
    @Test
    void editActivity() throws Exception {
        Activity activityInRepo = objectMapper.readValue(newActivity1Json, Activity.class);
        activityRepository.save(activityInRepo);

        MockHttpServletRequestBuilder httpReqEdit = MockMvcRequestBuilders.put("/activities/{activityId}", DEFAULT_ACTIVITY_ID)
                .header("Token", validToken)
                .content(newActivityEditJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReqEdit).andExpect(status().isOk()).andReturn();
        assertNotNull(result.getResponse());
    }

    private final String newActivityWrongDateFormatJson = JsonConverter.toJson(true,
            "activity_name", "Port Hills Rock Climbing",
            "description", "Cattlestop Crag lead climbing",
            "activity_type", new Object[]{
                    "Rock Climbing", "Mountaineering"
            },
            "continuous", false,
            "start_time", "2020-02-20T08:00:00Z",
            "end_time", "2020-02-20T08:00:00Z",
            "location", "Christchurch, NZ");

    /**
     * Tests that Bad Request is returned when the date format is not correct, in this case representing time zone
     * with a 'Z'.  It must be represented with +/- time.  See Java SimpleDateFormat and @JsonFormat in Activity.
     */
    @Test
    void newActivityWrongDateFormat() throws Exception {

        MockHttpServletRequestBuilder httpReq = MockMvcRequestBuilders.post("/profiles/{profileId}/activities", DEFAULT_USER_ID)
                .content(newActivityWrongDateFormatJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReq)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertNotNull(result.getResponse());
    }

    private final String newActivity2Json = JsonConverter.toJson(true,
            "activity_name", "Kaikoura Coast Track race",
            "description", "A big and nice race on a lovely peninsula",
            "activity_type", new Object[]{
                    "Astronomy", "Hiking"
            },
            "continuous", false,
            "start_time", "2020-02-20T08:00:00+1300",
            "end_time", "2020-02-20T08:00:00+1300",
            "location", "Kaikoura, NZ");


    /**
     * Test successful get request of activity.
     */
    @Test
    void getActivity() throws Exception {
        Activity activityInRepo = objectMapper.readValue(newActivity2Json, Activity.class);
        activityRepository.save(activityInRepo);

        MockHttpServletRequestBuilder httpReq = MockMvcRequestBuilders.get("/activities/{activityId}", DEFAULT_ACTIVITY_ID)
                .content(newActivity2Json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReq)
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponseStr = result.getResponse().getContentAsString();
        Activity activityReceived = objectMapper.readValue(jsonResponseStr, Activity.class);  // Convert JSON to Activity obj

        assertEquals(activityRepository.findByActivityId(DEFAULT_ACTIVITY_ID), activityReceived);
    }


}