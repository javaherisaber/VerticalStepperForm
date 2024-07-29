package ernestoyaquello.com.verticalstepperform;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ernestoyaquello.com.verticalstepperform.interfaces.StepCompletionListener;
import ernestoyaquello.com.verticalstepperform.interfaces.VerticalStepperForm;
import ernestoyaquello.com.verticalstepperform.utils.Animations;

/**
 * Custom layout that implements a vertical stepper form
 */
public class VerticalStepperFormLayout extends LinearLayout implements View.OnClickListener {

    // Style
    protected float alphaOfDisabledElements;
    protected int stepNumberBackgroundColor;
    protected int stepNumberDisabledBackgroundColor;
    protected int buttonBackgroundColor;
    protected int buttonPressedBackgroundColor;
    protected int stepNumberTextColor;
    protected int stepNumberDisabledTextColor;
    protected int stepTitleTextColor;
    protected int stepSubtitleTextColor;
    protected int buttonTextColor;
    protected int buttonPressedTextColor;
    protected int errorMessageTextColor;
    protected int verticalLineColor;
    protected boolean displayBottomNavigation;
    protected boolean materialDesignInDisabledSteps;
    protected boolean hideKeyboard;
    protected boolean showConfirmationStep;
    protected boolean showLastStepNextButton;
    protected boolean showVerticalLineWhenStepsAreCollapsed;
    protected int scrollViewBackgroundColor;
    protected int contentViewBackgroundColor;

    // Views
    protected LayoutInflater mInflater;
    protected LinearLayout content;
    public ScrollView stepsScrollView;
    protected List<LinearLayout> stepLayouts;
    protected List<View> stepContentViews;
    protected List<TextView> stepsTitlesViews;
    protected List<TextView> stepsSubtitlesViews;
    protected AppCompatButton confirmationButton;
    protected ProgressBar progressBar;
    protected AppCompatImageButton previousStepButton, nextStepButton;
    protected LinearLayout bottomNavigation;

    // Data
    protected List<String> steps;
    protected List<String> stepsSubtitles;

    // Logic
    protected int activeStep = 0;
    protected int numberOfSteps;
    protected boolean[] completedSteps;

    // Listeners and callbacks
    protected VerticalStepperForm verticalStepperFormImplementation;
    protected StepCompletionListener stepCompletionListener;

    // Context
    protected Context context;
    protected Activity activity;

    private static final String TAG = "VerticalStepperFormLayout";

    public VerticalStepperFormLayout(Context context) {
        super(context);
        init(context, null);
    }

    public VerticalStepperFormLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public VerticalStepperFormLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        this.context = context;
        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.vertical_stepper_form_layout, this, true);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VerticalStepperFormLayout, 0, 0);
            try {
                scrollViewBackgroundColor = a.getColor(R.styleable.VerticalStepperFormLayout_screenBackground, Color.WHITE);
                contentViewBackgroundColor = a.getColor(R.styleable.VerticalStepperFormLayout_formBackground, Color.WHITE);
            } finally {
                a.recycle();
            }
        } else {
            int colorWhite = ContextCompat.getColor(context, android.R.color.white);
            scrollViewBackgroundColor = colorWhite;
            contentViewBackgroundColor = colorWhite;
        }
    }

    /**
     * Returns the title of a step
     * @param stepNumber The step number (counting from 0)
     * @return the title string
     */
    public String getStepTitle(int stepNumber) {
        return steps.get(stepNumber);
    }

    /**
     * Returns the subtitle of a step
     * @param stepNumber The step number (counting from 0)
     * @return the subtitle string
     */
    public String getStepsSubtitles(int stepNumber) {
        if(stepsSubtitles != null) {
            return stepsSubtitles.get(stepNumber);
        }
        return null;
    }

    /**
     * Returns the active step number
     * @return the active step number (counting from 0)
     */
    public int getActiveStepNumber() {
        return activeStep;
    }

    /**
     * Set the title of certain step
     * @param stepNumber The step number (counting from 0)
     * @param title New title of the step
     */
    public void setStepTitle(int stepNumber, String title) {
        if(title != null && !title.isEmpty()) {
            steps.set(stepNumber, title);
            TextView titleView = stepsTitlesViews.get(stepNumber);
            if (titleView != null) {
                titleView.setText(title);
            }
        }
    }

    /**
     * Set the subtitle of certain step
     * @param stepNumber The step number (counting from 0)
     * @param subtitle New subtitle of the step
     */
    public void setStepSubtitle(int stepNumber, String subtitle) {
        if(stepsSubtitles != null && subtitle != null && !subtitle.isEmpty() && stepNumber < stepsSubtitles.size()) {
            stepsSubtitles.set(stepNumber, subtitle);
            TextView subtitleView = stepsSubtitlesViews.get(stepNumber);
            if (subtitleView != null) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void setActiveStepSubtitle(String subtitle) {
        setStepSubtitle(activeStep, subtitle);
    }

    public void closeActiveStep() {
        disableStepLayout(activeStep, true);
    }

    /**
     * Set the active step as completed
     */
    public void setActiveStepAsCompleted() {
        setStepAsCompleted(activeStep);
    }

    /**
     * Set the active step as not completed
     * @param errorMessage Error message that will be displayed (null for no message)
     */
    public void setActiveStepAsUncompleted(String errorMessage) {
        setStepAsUncompleted(activeStep, errorMessage);
    }

    /**
     * Set the step as completed
     * @param stepNumber the step number (counting from 0)
     */
    public void setStepAsCompleted(int stepNumber) {
        completedSteps[stepNumber] = true;

        if (stepLayouts == null || stepLayouts.isEmpty()) {
            Log.e(TAG, "No step layout found for stepNumber: " + stepNumber);
            return;
        }
        LinearLayout stepLayout = stepLayouts.get(stepNumber);
        RelativeLayout stepHeader = stepLayout.findViewById(R.id.step_header);
        ImageView stepDone = stepHeader.findViewById(R.id.step_done);
        TextView stepNumberTextView = stepHeader.findViewById(R.id.step_number);
        LinearLayout errorContainer = stepLayout.findViewById(R.id.error_container);
        TextView errorTextView = errorContainer.findViewById(R.id.error_message);
        AppCompatButton nextButton = stepLayout.findViewById(R.id.next_step);

        enableStepHeader(stepLayout);

        nextButton.setEnabled(true);
        nextButton.setAlpha(1);

        if (stepNumber != activeStep) {
            stepDone.setVisibility(View.VISIBLE);
            stepNumberTextView.setVisibility(View.INVISIBLE);
        } else {
            if (stepNumber != numberOfSteps) {
                enableNextButtonInBottomNavigationLayout();
            } else {
                disableNextButtonInBottomNavigationLayout();
            }
        }

        errorTextView.setText("");
        //errorContainer.setVisibility(View.GONE);
        Animations.slideUp(errorContainer);

        displayCurrentProgress();
        if (stepCompletionListener != null) {
            stepCompletionListener.onStepCompleted(stepNumber);
        }
    }

    /**
     * Set the step as not completed
     * @param stepNumber the step number (counting from 0)
     * @param errorMessage Error message that will be displayed (null for no message)
     */
    public void setStepAsUncompleted(int stepNumber, String errorMessage) {
        completedSteps[stepNumber] = false;

        if (stepLayouts == null || stepLayouts.isEmpty()) {
            Log.e(TAG, "No step layout found for stepNumber: " + stepNumber);
            return;
        }
        LinearLayout stepLayout = stepLayouts.get(stepNumber);
        RelativeLayout stepHeader = stepLayout.findViewById(R.id.step_header);
        ImageView stepDone = stepHeader.findViewById(R.id.step_done);
        TextView stepNumberTextView = stepHeader.findViewById(R.id.step_number);
        AppCompatButton nextButton = stepLayout.findViewById(R.id.next_step);

        stepDone.setVisibility(View.INVISIBLE);
        stepNumberTextView.setVisibility(View.VISIBLE);

        nextButton.setEnabled(false);
        nextButton.setAlpha(alphaOfDisabledElements);

        if (stepNumber == activeStep) {
            disableNextButtonInBottomNavigationLayout();
        } else {
            disableStepHeader(stepLayout);
        }

        if (stepNumber < numberOfSteps && showConfirmationStep) {
            setStepAsUncompleted(numberOfSteps, null);
        }

        if (errorMessage != null && !errorMessage.equals("")) {
            LinearLayout errorContainer = stepLayout.findViewById(R.id.error_container);
            TextView errorTextView = errorContainer.findViewById(R.id.error_message);

            errorTextView.setText(errorMessage);
            //errorContainer.setVisibility(View.VISIBLE);
            Animations.slideDown(errorContainer);
        }

        displayCurrentProgress();
        if (stepCompletionListener != null) {
            stepCompletionListener.onStepUncompleted(stepNumber);
        }
    }

    /**
     * Determines whether the active step is completed or not
     * @return true if the active step is completed; false otherwise
     */
    public boolean isActiveStepCompleted() {
        return isStepCompleted(activeStep);
    }

    /**
     * Determines whether the given step is completed or not
     * @param stepNumber the step number (counting from 0)
     * @return true if the step is completed, false otherwise
     */
    public boolean isStepCompleted(int stepNumber) {
        return completedSteps[stepNumber];
    }

    /**
     * Determines if any step has been completed
     * @return true if at least 1 step has been completed; false otherwise
     */
    public boolean isAnyStepCompleted() {
        for (boolean completedStep : completedSteps) {
            if (completedStep) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the steps that are previous to the given one are completed
     * @param stepNumber the selected step number (counting from 0)
     * @return true if all the previous steps have been completed; false otherwise
     */
    public boolean arePreviousStepsCompleted(int stepNumber) {
        boolean previousStepsAreCompleted = true;
        for (int i = (stepNumber - 1); i >= 0 && previousStepsAreCompleted; i--) {
            previousStepsAreCompleted = completedSteps[i];
        }
        return previousStepsAreCompleted;
    }

    /**
     * Go to the next step
     */
    public void goToNextStep() {
        goToStep(activeStep + 1, false);
    }

    /**
     * Go to the previous step
     */
    public void goToPreviousStep() {
        goToStep(activeStep - 1, false);
    }

    /**
     * Go to the selected step
     * @param stepNumber the selected step number (counting from 0)
     * @param restoration true if the method has been called to restore the form; false otherwise
     */
    public void goToStep(int stepNumber, boolean restoration) {
        if (activeStep != stepNumber || restoration) {
            if(hideKeyboard) {
                hideSoftKeyboard();
            }
            boolean previousStepsAreCompleted =
                    arePreviousStepsCompleted(stepNumber);
            if (stepNumber == 0 || previousStepsAreCompleted) {
                openStep(stepNumber, restoration);
            }
        } else if (isStepCompleted(stepNumber)) {
            openStep(stepNumber, false);
        }
    }

    /**
     * Set the active step as not completed
     * @deprecated use {@link #setActiveStepAsUncompleted(String)} instead
     */
    @Deprecated
    public void setActiveStepAsUncompleted() {
        setStepAsUncompleted(activeStep, null);
    }

    /**
     * Set the selected step as not completed
     * @param stepNumber the step number (counting from 0)
     * @deprecated use {@link #setStepAsUncompleted(int, String)} instead
     */
    @Deprecated
    public void setStepAsUncompleted(int stepNumber) {
        setStepAsUncompleted(stepNumber, null);
    }

    /**
     * Set up and initialize the form
     * @param stepsTitles names of the steps
     * @param colorPrimary primary color
     * @param colorPrimaryDark primary color (dark)
     * @param verticalStepperForm instance that implements the interface "VerticalStepperForm"
     * @param activity activity where the form is
     *
     * @deprecated use {@link Builder#newInstance(VerticalStepperFormLayout, String[], VerticalStepperForm, Activity)} instead like this:
     * <blockquote><pre>
     * VerticalStepperFormLayout.Builder.newInstance(verticalStepperFormLayout, stepsTitles, verticalStepperForm, activity)<br>
     *     .primaryColor(colorPrimary)<br>
     *     .primaryDarkColor(colorPrimaryDark)<br>
     *     .init();
     * </pre></blockquote>
     */
    @Deprecated
    public void initialiseVerticalStepperForm(String[] stepsTitles,
                                              int colorPrimary, int colorPrimaryDark,
                                              VerticalStepperForm verticalStepperForm,
                                              Activity activity) {

        this.alphaOfDisabledElements = 0.25f;
        this.buttonTextColor = Color.rgb(255, 255, 255);
        this.buttonPressedTextColor = Color.rgb(255, 255, 255);
        this.stepNumberTextColor = Color.rgb(255, 255, 255);
        this.stepNumberDisabledTextColor = Color.rgb(255, 255, 255);
        this.stepTitleTextColor = Color.rgb(33, 33, 33);
        this.stepSubtitleTextColor = Color.rgb(162, 162, 162);
        this.stepNumberBackgroundColor = colorPrimary;
        this.stepNumberDisabledBackgroundColor = Color.rgb(176, 176, 176);
        this.buttonBackgroundColor = colorPrimary;
        this.buttonPressedBackgroundColor = colorPrimaryDark;
        this.verticalLineColor =  Color.rgb(224, 224, 224);
        this.errorMessageTextColor = Color.rgb(175, 18, 18);
        this.displayBottomNavigation = true;
        this.materialDesignInDisabledSteps = false;
        this.hideKeyboard = true;
        this.showConfirmationStep = true;
        this.showLastStepNextButton = true;
        this.showVerticalLineWhenStepsAreCollapsed = false;

        this.verticalStepperFormImplementation = verticalStepperForm;
        this.activity = activity;

        initStepperForm(stepsTitles, null);
    }

    /**
     * Set up and initialize the form
     * @param stepsTitles names of the steps
     * @param buttonBackgroundColor background colour of the buttons
     * @param buttonTextColor text color of the buttons
     * @param buttonPressedBackgroundColor background color of the buttons when clicked
     * @param buttonPressedTextColor text color of the buttons when clicked
     * @param stepNumberBackgroundColor background color of the left circles
     * @param stepNumberTextColor text color of the left circles
     * @param verticalStepperForm instance that implements the interface "VerticalStepperForm"
     * @param activity activity where the form is
     *
     * @deprecated use {@link Builder#newInstance(VerticalStepperFormLayout, String[], VerticalStepperForm, Activity)} instead like this:
     * <blockquote><pre>
     * VerticalStepperFormLayout.Builder.newInstance(verticalStepperFormLayout, stepsTitles, verticalStepperForm, activity)<br>
     *     .buttonBackgroundColor(buttonBackgroundColor)<br>
     *     .buttonTextColor(buttonTextColor)<br>
     *     .buttonPressedBackgroundColor(buttonPressedBackgroundColor)<br>
     *     .buttonPressedTextColor(buttonPressedTextColor)<br>
     *     .stepNumberBackgroundColor(stepNumberBackgroundColor)<br>
     *     .stepNumberTextColor(stepNumberTextColor)<br>
     *     .init();
     * </pre></blockquote>
     */
    @Deprecated
    public void initialiseVerticalStepperForm(String[] stepsTitles,
                                              int buttonBackgroundColor, int buttonTextColor,
                                              int buttonPressedBackgroundColor, int buttonPressedTextColor,
                                              int stepNumberBackgroundColor, int stepNumberTextColor,
                                              VerticalStepperForm verticalStepperForm,
                                              Activity activity) {

        this.alphaOfDisabledElements = 0.25f;
        this.buttonBackgroundColor = buttonBackgroundColor;
        this.buttonTextColor = buttonTextColor;
        this.buttonPressedBackgroundColor = buttonPressedBackgroundColor;
        this.buttonPressedTextColor = buttonPressedTextColor;
        this.stepNumberBackgroundColor = stepNumberBackgroundColor;
        this.stepNumberDisabledBackgroundColor = Color.rgb(176, 176, 176);
        this.stepTitleTextColor = Color.rgb(33, 33, 33);
        this.stepSubtitleTextColor = Color.rgb(162, 162, 162);
        this.stepNumberTextColor = stepNumberTextColor;
        this.stepNumberDisabledTextColor = Color.rgb(255, 255, 255);
        this.errorMessageTextColor = Color.rgb(175, 18, 18);
        this.verticalLineColor = Color.rgb(224, 224, 224);
        this.displayBottomNavigation = true;
        this.materialDesignInDisabledSteps = false;
        this.hideKeyboard = true;
        this.showConfirmationStep = true;
        this.showLastStepNextButton = true;
        this.showVerticalLineWhenStepsAreCollapsed = false;

        this.verticalStepperFormImplementation = verticalStepperForm;
        this.activity = activity;

        initStepperForm(stepsTitles, null);
    }

    protected void initialiseVerticalStepperForm(Builder builder) {

        this.verticalStepperFormImplementation = builder.verticalStepperFormImplementation;
        this.stepCompletionListener = builder.stepCompletionListener;
        this.activity = builder.activity;

        this.alphaOfDisabledElements = builder.alphaOfDisabledElements;
        this.stepNumberBackgroundColor = builder.stepNumberBackgroundColor;
        this.stepNumberDisabledBackgroundColor = builder.stepNumberDisabledBackgroundColor;
        this.buttonBackgroundColor = builder.buttonBackgroundColor;
        this.buttonPressedBackgroundColor = builder.buttonPressedBackgroundColor;
        this.stepNumberTextColor = builder.stepNumberTextColor;
        this.stepNumberDisabledTextColor = builder.stepNumberDisabledTextColor;
        this.stepTitleTextColor = builder.stepTitleTextColor;
        this.stepSubtitleTextColor = builder.stepSubtitleTextColor;
        this.buttonTextColor = builder.buttonTextColor;
        this.buttonPressedTextColor = builder.buttonPressedTextColor;
        this.errorMessageTextColor = builder.errorMessageTextColor;
        this.verticalLineColor = builder.verticalLineColor;
        this.displayBottomNavigation = builder.displayBottomNavigation;
        this.materialDesignInDisabledSteps = builder.materialDesignInDisabledSteps;
        this.hideKeyboard = builder.hideKeyboard;
        this.showConfirmationStep = builder.showConfirmationStep;
        this.showLastStepNextButton = builder.showLastStepNextButton;
        this.showVerticalLineWhenStepsAreCollapsed = builder.showVerticalLineWhenStepsAreCollapsed;

        initStepperForm(builder.steps, builder.stepsSubtitles);
    }

    protected void initStepperForm(String[] stepsTitles, String[] stepsSubtitles) {
        setSteps(stepsTitles, stepsSubtitles);

        List<View> stepContentLayouts = new ArrayList<>();
        for (int i = 0; i < numberOfSteps; i++) {
            View stepLayout = verticalStepperFormImplementation.createStepContentView(i);
            stepContentLayouts.add(stepLayout);
        }
        stepContentViews = stepContentLayouts;

        initializeForm();

        verticalStepperFormImplementation.onStepOpening(activeStep);
    }

    protected void setSteps(String[] steps, String[] stepsSubtitles) {
        this.steps = new ArrayList<>(Arrays.asList(steps));
        if(stepsSubtitles != null) {
            this.stepsSubtitles = new ArrayList<>(Arrays.asList(stepsSubtitles));
        } else {
            this.stepsSubtitles = null;
        }
        numberOfSteps = steps.length;
        setAuxVars();
        if (showConfirmationStep) {
            addConfirmationStepToStepsList();
        }
    }

    protected void registerListeners() {
        previousStepButton.setOnClickListener(this);
        nextStepButton.setOnClickListener(this);
    }

    protected void initializeForm() {
        stepsTitlesViews = new ArrayList<>();
        stepsSubtitlesViews = new ArrayList<>();
        setUpSteps();
        if (!displayBottomNavigation) {
            hideBottomNavigation();
        }
        goToStep(0, true);

        setObserverForKeyboard();
    }

    // http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
    protected void setObserverForKeyboard() {
        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                content.getWindowVisibleDisplayFrame(r);

                int heightDiff = content.getRootView().getHeight() - (r.bottom - r.top);
                if (heightDiff > 100) { // if more than 100 pixels, it is probably a keyboard...
                    scrollToActiveStep(true);
                }
            }
        });
    }

    protected void hideBottomNavigation() {
        bottomNavigation.setVisibility(View.GONE);
    }

    protected void setUpSteps() {
        stepLayouts = new ArrayList<>();
        // Set up normal steps
        for (int i = 0; i < numberOfSteps; i++) {
            setUpStep(i);
        }
        if (showConfirmationStep) {
            // Set up confirmation step
            setUpStep(numberOfSteps);
        }
        addFormGap();
    }

    private void addFormGap() {
        View formBottomGap = new View(context);
        int height = (int) getResources().getDimension(R.dimen.vertical_stepper_bottom_gap);
        int paddingVertical = (int) getResources().getDimension(R.dimen.vertical_stepper_padding_vertical);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
        params.setMargins(0, paddingVertical, 0, 0);
        formBottomGap.setLayoutParams(params);
        formBottomGap.setBackgroundColor(scrollViewBackgroundColor);
        content.addView(formBottomGap);
    }

    protected void setUpStep(int stepNumber) {
        LinearLayout stepLayout = createStepLayout(stepNumber);
        if (stepNumber < numberOfSteps) {
            // The content of the step is the corresponding custom view previously created
            LinearLayout stepContent = stepLayout.findViewById(R.id.step_content);
            stepContent.addView(stepContentViews.get(stepNumber));
        } else if (showConfirmationStep) {
            setUpStepLayoutAsConfirmationStepLayout(stepLayout);
        }
        addStepToContent(stepLayout);
    }

    protected void addStepToContent(LinearLayout stepLayout) {
        int paddingHorizontal = (int) getResources().getDimension(R.dimen.vertical_stepper_padding_horizontal);
        stepLayout.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        content.addView(stepLayout);
    }

    protected void setUpStepLayoutAsConfirmationStepLayout(LinearLayout stepLayout) {
        View stepLeftLine =  stepLayout.findViewById(R.id.vertical_line_content);
        View stepLeftLine2 = stepLayout.findViewById(R.id.vertical_line_subtitle);
        confirmationButton = stepLayout.findViewById(R.id.next_step);

        stepLeftLine.setVisibility(View.INVISIBLE);
        stepLeftLine2.setVisibility(View.INVISIBLE);

        disableConfirmationButton();

        confirmationButton.setText(R.string.vertical_form_stepper_form_confirm_button);
        confirmationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareSendingAndSend();
            }
        });

        // Some content could be added to the final step inside stepContent layout
        // RelativeLayout stepContent = (RelativeLayout) stepLayout.findViewById(R.id.step_content);
    }

    protected LinearLayout createStepLayout(final int stepNumber) {
        LinearLayout stepLayout = generateStepLayout();

        LinearLayout circle = stepLayout.findViewById(R.id.circle);
        Drawable bg = ContextCompat.getDrawable(context, R.drawable.circle_step_done);
        bg.setColorFilter(new PorterDuffColorFilter(
                stepNumberBackgroundColor, PorterDuff.Mode.SRC_IN));
        circle.setBackground(bg);

        TextView stepTitle = stepLayout.findViewById(R.id.step_title);
        stepTitle.setText(steps.get(stepNumber));
        stepTitle.setTextColor(stepTitleTextColor);
        stepsTitlesViews.add(stepNumber, stepTitle);

        TextView stepSubtitle = null;
        if(stepsSubtitles != null && stepNumber < stepsSubtitles.size()) {
            String subtitle = stepsSubtitles.get(stepNumber);
            stepSubtitle = stepLayout.findViewById(R.id.step_subtitle);
            stepSubtitle.setTextColor(stepSubtitleTextColor);
            if(subtitle != null && !subtitle.equals("")) {
                stepSubtitle.setText(subtitle);
                stepSubtitle.setVisibility(View.VISIBLE);
            }
        }
        stepsSubtitlesViews.add(stepNumber, stepSubtitle);

        TextView stepNumberTextView = stepLayout.findViewById(R.id.step_number);
        stepNumberTextView.setText(String.valueOf(stepNumber + 1));
        stepNumberTextView.setTextColor(stepNumberTextColor);

        ImageView stepDoneImageView = stepLayout.findViewById(R.id.step_done);
        stepDoneImageView.setColorFilter(stepNumberTextColor);
        View subtitleVerticalLine = stepLayout.findViewById(R.id.vertical_line_subtitle);
        View contentVerticalLine = stepLayout.findViewById(R.id.vertical_line_content);
        View nextVerticalLine = stepLayout.findViewById(R.id.vertical_line_next);
        setVerticalLinesColor(stepLayout, verticalLineColor, false);

        TextView errorMessage = stepLayout.findViewById(R.id.error_message);
        ImageView errorIcon = stepLayout.findViewById(R.id.error_icon);
        errorMessage.setTextColor(errorMessageTextColor);
        errorIcon.setColorFilter(errorMessageTextColor);

        RelativeLayout stepHeader = stepLayout.findViewById(R.id.step_header);
        stepHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToStep(stepNumber, false);
            }
        });

        AppCompatButton nextButton = stepLayout.findViewById(R.id.next_step);
        setButtonColor(nextButton,
                buttonBackgroundColor, buttonTextColor, buttonPressedBackgroundColor, buttonPressedTextColor);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stepNumber == (numberOfSteps - 1) && !showConfirmationStep) {
                    prepareSendingAndSend();
                } else {
                    goToStep((stepNumber + 1), false);
                }
            }
        });
        if (!showLastStepNextButton && stepNumber == (numberOfSteps - 1)) {
            nextButton.setVisibility(View.GONE);
            contentVerticalLine.setVisibility(View.INVISIBLE);
            nextVerticalLine.setVisibility(View.INVISIBLE);
            subtitleVerticalLine.setVisibility(View.INVISIBLE);
        }

        stepLayouts.add(stepLayout);

        return stepLayout;
    }

    protected LinearLayout generateStepLayout() {
        LayoutInflater inflater = LayoutInflater.from(context);
        return (LinearLayout) inflater.inflate(R.layout.step_layout, content, false);
    }

    protected void openStep(int stepNumber, boolean restoration) {
        int lastStepIndex = numberOfSteps;
        if (showConfirmationStep) {
            lastStepIndex = numberOfSteps + 1;
        }
        if (stepNumber >= 0 && stepNumber < lastStepIndex) {
            activeStep = stepNumber;

            if (stepNumber == 0) {
                disablePreviousButtonInBottomNavigationLayout();
            } else {
                enablePreviousButtonInBottomNavigationLayout();
            }

            if (completedSteps[stepNumber] && activeStep != numberOfSteps) {
                enableNextButtonInBottomNavigationLayout();
            } else {
                disableNextButtonInBottomNavigationLayout();
            }

            for (int i = 0; i < lastStepIndex; i++) {
                if (i != stepNumber) {
                    disableStepLayout(i, !restoration);
                } else {
                    enableStepLayout(i, !restoration);
                }
            }

            scrollToActiveStep(!restoration);

            if (stepNumber == numberOfSteps) {
                setStepAsCompleted(stepNumber);
            }

            verticalStepperFormImplementation.onStepOpening(stepNumber);
        }
    }

    protected void scrollToStep(final int stepNumber, boolean smoothScroll) {
        if (stepLayouts == null || stepNumber >= stepLayouts.size()) {
            return;
        }
        if (smoothScroll) {
            stepsScrollView.post(new Runnable() {
                public void run() {
                    stepsScrollView.smoothScrollTo(0, stepLayouts.get(stepNumber).getTop());
                }
            });
        } else {
            stepsScrollView.post(new Runnable() {
                public void run() {
                    stepsScrollView.scrollTo(0, stepLayouts.get(stepNumber).getTop());
                }
            });
        }
    }

    protected void scrollToActiveStep(boolean smoothScroll) {
        scrollToStep(activeStep, smoothScroll);
    }

    protected void findViews() {
        content = findViewById(R.id.content);
        stepsScrollView = findViewById(R.id.steps_scroll);
        progressBar = findViewById(R.id.progress_bar);
        previousStepButton = findViewById(R.id.down_previous);
        nextStepButton = findViewById(R.id.down_next);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        content.setBackgroundColor(contentViewBackgroundColor);
        stepsScrollView.setBackgroundColor(scrollViewBackgroundColor);
    }

    protected void disableStepLayout(int stepNumber, boolean smoothieDisabling) {
        LinearLayout stepLayout = stepLayouts.get(stepNumber);
        RelativeLayout stepHeader = stepLayout.findViewById(R.id.step_header);
        ImageView stepDone = stepHeader.findViewById(R.id.step_done);
        TextView stepNumberTextView = stepHeader.findViewById(R.id.step_number);
        LinearLayout button = stepLayout.findViewById(R.id.next_step_button_container);
        LinearLayout stepContent = stepLayout.findViewById(R.id.step_content);
        ImageView editStep = stepLayout.findViewById(R.id.edit_step);
        TextView subtitle = stepHeader.findViewById(R.id.step_subtitle);
        TextView errorTextView = stepLayout.findViewById(R.id.error_message);
        if(subtitle.getText() != null && !subtitle.getText().equals("")) {
            subtitle.setVisibility(View.VISIBLE);
        }

        if (smoothieDisabling) {
            Animations.slideUp(button);
            Animations.slideUp(stepContent);
        } else {
            button.setVisibility(View.GONE);
            stepContent.setVisibility(View.GONE);
        }

        stepNumberTextView.setTextColor(stepNumberDisabledTextColor);
        if (!completedSteps[stepNumber]) {
            disableStepHeader(stepLayout);
            stepDone.setVisibility(View.INVISIBLE);
            stepNumberTextView.setVisibility(View.VISIBLE);
        } else {
            enableStepHeader(stepLayout);
            stepDone.setVisibility(View.VISIBLE);
            stepNumberTextView.setVisibility(View.INVISIBLE);
        }
        if (completedSteps[stepNumber] || !errorTextView.getText().toString().isBlank()) {
            editStep.setVisibility(View.VISIBLE);
        }
        if ((stepNumber == numberOfSteps) && showConfirmationStep) {
            editStep.setVisibility(View.GONE);
        }

        showVerticalLineInCollapsedStepIfNecessary(stepLayout);
    }

    protected void enableStepLayout(int stepNumber, boolean smoothieEnabling) {
        LinearLayout stepLayout = stepLayouts.get(stepNumber);
        LinearLayout stepContent = stepLayout.findViewById(R.id.step_content);
        RelativeLayout stepHeader = stepLayout.findViewById(R.id.step_header);
        ImageView stepDone = stepHeader.findViewById(R.id.step_done);
        TextView stepNumberTextView = stepHeader.findViewById(R.id.step_number);
        LinearLayout button = stepLayout.findViewById(R.id.next_step_button_container);
        ImageView editStep = stepLayout.findViewById(R.id.edit_step);
        TextView subtitle = stepHeader.findViewById(R.id.step_subtitle);
        setVerticalLinesColor(stepLayout, verticalLineColor, false);
        if(subtitle.getText() != null && !subtitle.getText().equals("")) {
            subtitle.setVisibility(View.GONE);
        }

        enableStepHeader(stepLayout);

        if (smoothieEnabling) {
            Animations.slideDown(stepContent);
            Animations.slideDown(button);
        } else {
            stepContent.setVisibility(View.VISIBLE);
            button.setVisibility(View.VISIBLE);
        }
        editStep.setVisibility(View.GONE);
        stepNumberTextView.setTextColor(stepNumberTextColor);

        if (completedSteps[stepNumber] && activeStep != stepNumber) {
            stepDone.setVisibility(View.VISIBLE);
            stepNumberTextView.setVisibility(View.INVISIBLE);
        } else {
            stepDone.setVisibility(View.INVISIBLE);
            stepNumberTextView.setVisibility(View.VISIBLE);
        }

        hideVerticalLineInCollapsedStepIfNecessary(stepLayout);
    }

    private void setVerticalLinesColor(LinearLayout stepLayout, int color, boolean disable) {
        View subtitleVerticalLine = stepLayout.findViewById(R.id.vertical_line_subtitle);
        View contentVerticalLine = stepLayout.findViewById(R.id.vertical_line_content);
        View nextVerticalLine = stepLayout.findViewById(R.id.vertical_line_next);
        if (disable) {
            if (contentVerticalLine.getVisibility() == View.GONE) {
                subtitleVerticalLine.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_vertical_curved));
            } else {
                subtitleVerticalLine.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_top_curved));
                contentVerticalLine.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_bottom_curved));
                Drawable contentBackground = contentVerticalLine.getBackground();
                contentBackground.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            }
            Drawable subtitleBackground = subtitleVerticalLine.getBackground();
            subtitleBackground.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        } else {
            contentVerticalLine.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_top_curved));
            subtitleVerticalLine.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_vertical_curved));
            nextVerticalLine.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_bottom_curved));
            Drawable contentBackground = contentVerticalLine.getBackground();
            Drawable nextBackground = nextVerticalLine.getBackground();
            Drawable subtitleBackground = subtitleVerticalLine.getBackground();
            subtitleBackground.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            nextBackground.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            contentBackground.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        }
    }

    protected void enableStepHeader(LinearLayout stepLayout) {
        setHeaderAppearance(stepLayout, 1, buttonBackgroundColor);
    }

    protected void disableStepHeader(LinearLayout stepLayout) {
        setHeaderAppearance(stepLayout, alphaOfDisabledElements, stepNumberDisabledBackgroundColor);
        if (materialDesignInDisabledSteps) {
            setVerticalLinesColor(stepLayout, stepNumberDisabledBackgroundColor, true);
        }
    }

    protected void showVerticalLineInCollapsedStepIfNecessary(LinearLayout stepLayout) {
        // The height of the line will be 16dp when the subtitle textview is gone
        if(showVerticalLineWhenStepsAreCollapsed) {
            setVerticalLineNearSubtitleHeightWhenSubtitleIsGone(stepLayout, 16);
        }
    }

    protected void hideVerticalLineInCollapsedStepIfNecessary(LinearLayout stepLayout) {
        // The height of the line will be 0 when the subtitle text is being shown
        if(showVerticalLineWhenStepsAreCollapsed) {
            setVerticalLineNearSubtitleHeightWhenSubtitleIsGone(stepLayout, 0);
        }
    }

    protected void displayCurrentProgress() {
        int progress = 0;
        for (int i = 0; i < (completedSteps.length - 1); i++) {
            if (completedSteps[i]) {
                ++progress;
            }
        }
        progressBar.setProgress(progress);
    }

    protected void displayMaxProgress() {
        setProgress(numberOfSteps + 1);
    }

    protected void setAuxVars() {
        completedSteps = new boolean[numberOfSteps + 1];
        for (int i = 0; i < (numberOfSteps + 1); i++) {
            completedSteps[i] = false;
        }
        progressBar.setMax(numberOfSteps + 1);
    }

    protected void addConfirmationStepToStepsList() {
        String confirmationStepText = context.getString(R.string.vertical_form_stepper_form_last_step);
        steps.add(confirmationStepText);
    }

    protected void disablePreviousButtonInBottomNavigationLayout() {
        disableBottomButtonNavigation(previousStepButton);
    }

    protected void enablePreviousButtonInBottomNavigationLayout() {
        enableBottomButtonNavigation(previousStepButton);
    }

    protected void disableNextButtonInBottomNavigationLayout() {
        disableBottomButtonNavigation(nextStepButton);
    }

    protected void enableNextButtonInBottomNavigationLayout() {
        enableBottomButtonNavigation(nextStepButton);
    }

    protected void enableBottomButtonNavigation(ImageButton button) {
        button.setAlpha(1f);
        button.setEnabled(true);
    }

    protected void disableBottomButtonNavigation(ImageButton button) {
        button.setAlpha(alphaOfDisabledElements);
        button.setEnabled(false);
    }

    protected void setProgress(int progress) {
        if (progress > 0 && progress <= (numberOfSteps + 1)) {
            progressBar.setProgress(progress);
        }
    }

    protected void disableConfirmationButton() {
        confirmationButton.setEnabled(false);
        confirmationButton.setAlpha(alphaOfDisabledElements);
    }

    protected void hideSoftKeyboard() {
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void finalizeForm() {
        prepareSendingAndSend();
    }

    protected void prepareSendingAndSend() {
        if (showConfirmationStep) {
            displayDoneIconInConfirmationStep();
            disableConfirmationButton();
        }
        displayMaxProgress();
        verticalStepperFormImplementation.sendData();
    }

    protected void displayDoneIconInConfirmationStep() {
        LinearLayout confirmationStepLayout = stepLayouts.get(stepLayouts.size() - 1);
        ImageView stepDone = confirmationStepLayout.findViewById(R.id.step_done);
        TextView stepNumberTextView = confirmationStepLayout.findViewById(R.id.step_number);
        stepDone.setVisibility(View.VISIBLE);
        stepNumberTextView.setVisibility(View.INVISIBLE);
    }

    protected void restoreFormState() {
        goToStep(activeStep, true);
        displayCurrentProgress();
    }

    protected int convertDpToPixel(float dp){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return (int)px;
    }

    protected void setVerticalLineNearSubtitleHeightWhenSubtitleIsGone(LinearLayout stepLayout, int height) {
        TextView stepSubtitle = stepLayout.findViewById(R.id.step_subtitle);
        if (stepSubtitle.getVisibility() == View.GONE) {
            View stepLeftLine = stepLayout.findViewById(R.id.vertical_line_subtitle);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) stepLeftLine.getLayoutParams();
            params.height = convertDpToPixel(height);
            stepLeftLine.setLayoutParams(params);
        }
    }

    protected void setHeaderAppearance(LinearLayout stepLayout, float alpha, int stepCircleBackgroundColor) {
        RelativeLayout stepHeader = stepLayout.findViewById(R.id.step_header);
        TextView title = stepHeader.findViewById(R.id.step_title);
        LinearLayout circle = stepHeader.findViewById(R.id.circle);
        ImageView done = stepHeader.findViewById(R.id.step_done);
        if(!materialDesignInDisabledSteps) {
            title.setAlpha(alpha);
            circle.setAlpha(alpha);
            done.setAlpha(alpha);
        } else {
            Drawable bg = ContextCompat.getDrawable(context, R.drawable.circle_step_done);
            bg.setColorFilter(new PorterDuffColorFilter(stepCircleBackgroundColor, PorterDuff.Mode.SRC_IN));
            circle.setBackground(bg);
        }
    }

    protected void setButtonColor(AppCompatButton button, int buttonColor, int buttonTextColor,
                                  int buttonPressedColor, int buttonPressedTextColor) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_pressed},
                new int[]{android.R.attr.state_focused},
                new int[]{}
        };
        ColorStateList buttonColours = new ColorStateList(
                states,
                new int[]{
                        buttonPressedColor,
                        buttonPressedColor,
                        buttonColor
                });
        ColorStateList buttonTextColours = new ColorStateList(
                states,
                new int[]{
                        buttonPressedTextColor,
                        buttonPressedTextColor,
                        buttonTextColor
                });
        button.setBackgroundTintList(buttonColours);
        button.setTextColor(buttonTextColours);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        findViews();
        registerListeners();
    }

    @Override
    public void onClick(View v) {
        String previousNavigationButtonTag =
                context.getString(R.string.vertical_form_stepper_form_down_previous);
        if (v.getTag().equals(previousNavigationButtonTag)) {
            goToPreviousStep();
        } else {
            if (isActiveStepCompleted()) {
                goToNextStep();
            }
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putInt("activeStep", this.activeStep);
        bundle.putBooleanArray("completedSteps", this.completedSteps);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) // implicit null check
        {
            Bundle bundle = (Bundle) state;
            this.activeStep = bundle.getInt("activeStep");
            this.completedSteps = bundle.getBooleanArray("completedSteps");
            state = bundle.getParcelable("superState");
            restoreFormState();
        }
        super.onRestoreInstanceState(state);
    }

    public static class Builder {

        // Required parameters
        protected VerticalStepperFormLayout verticalStepperFormLayout;
        protected String[] steps;
        protected VerticalStepperForm verticalStepperFormImplementation;
        protected StepCompletionListener stepCompletionListener;
        protected Activity activity;

        // Optional parameters
        protected String[] stepsSubtitles = null;
        protected float alphaOfDisabledElements = 0.25f;
        protected int stepNumberBackgroundColor = Color.rgb(63, 81, 181);
        protected int stepNumberDisabledBackgroundColor = Color.rgb(176, 176, 176);
        protected int buttonBackgroundColor = Color.rgb(63, 81, 181);
        protected int buttonPressedBackgroundColor = Color.rgb(48, 63, 159);
        protected int stepNumberTextColor = Color.rgb(255, 255, 255);
        protected int stepNumberDisabledTextColor = Color.rgb(255, 255, 255);
        protected int stepTitleTextColor = Color.rgb(33, 33, 33);
        protected int stepSubtitleTextColor = Color.rgb(162, 162, 162);
        protected int buttonTextColor = Color.rgb(255, 255, 255);
        protected int buttonPressedTextColor = Color.rgb(255, 255, 255);
        protected int errorMessageTextColor = Color.rgb(175, 18, 18);
        protected int verticalLineColor = Color.rgb(224, 224, 224);
        protected boolean displayBottomNavigation = true;
        protected boolean materialDesignInDisabledSteps = false;
        protected boolean hideKeyboard = true;
        protected boolean showConfirmationStep = true;
        protected boolean showLastStepNextButton = true;
        protected boolean showVerticalLineWhenStepsAreCollapsed = false;

        protected Builder(VerticalStepperFormLayout stepperLayout,
                          String[] steps,
                          VerticalStepperForm stepperImplementation,
                          Activity activity) {

            this.verticalStepperFormLayout = stepperLayout;
            this.steps = steps;
            this.verticalStepperFormImplementation = stepperImplementation;
            this.activity = activity;
        }

        /**
         * Generates an instance of the builder that will set up and initialize the form (after
         * setting up the form it is mandatory to initialize it calling init())
         * @param stepperLayout the form layout
         * @param stepTitles a String array with the names of the steps
         * @param stepperImplementation The instance that implements "VerticalStepperForm" interface
         * @param activity The activity where the form is
         * @return an instance of the builder
         */
        public static Builder newInstance(VerticalStepperFormLayout stepperLayout,
                                          String[] stepTitles,
                                          VerticalStepperForm stepperImplementation,
                                          Activity activity) {

            return new Builder(stepperLayout, stepTitles, stepperImplementation, activity);
        }

        public Builder stepsSubtitles(String[] stepsSubtitles) {
            this.stepsSubtitles = stepsSubtitles;
            return this;
        }

        /**
         * Set the primary color (background color of the left circles and buttons)
         * @param colorPrimary primary color
         * @return the builder instance
         */
        public Builder primaryColor(int colorPrimary) {
            this.stepNumberBackgroundColor = colorPrimary;
            this.buttonBackgroundColor = colorPrimary;
            return this;
        }

        public Builder setStepCompletionListener(StepCompletionListener stepCompletionListener) {
            this.stepCompletionListener = stepCompletionListener;
            return this;
        }

        /**
         * Set the dark primary color (background color of the buttons when clicked)
         * @param colorPrimaryDark primary color (dark)
         * @return the builder instance
         */
        public Builder primaryDarkColor(int colorPrimaryDark) {
            this.buttonPressedBackgroundColor = colorPrimaryDark;
            return this;
        }

        public Builder stepNumberBackgroundColor(int stepNumberBackgroundColor) {
            this.stepNumberBackgroundColor = stepNumberBackgroundColor;
            return this;
        }

        public Builder buttonBackgroundColor(int buttonBackgroundColor) {
            this.buttonBackgroundColor = buttonBackgroundColor;
            return this;
        }

        public Builder buttonPressedBackgroundColor(int buttonPressedBackgroundColor) {
            this.buttonPressedBackgroundColor = buttonPressedBackgroundColor;
            return this;
        }

        public Builder stepNumberTextColor(int stepNumberTextColor) {
            this.stepNumberTextColor = stepNumberTextColor;
            return this;
        }

        public Builder stepNumberDisabledTextColor(int stepNumberDisabledTextColor) {
            this.stepNumberDisabledTextColor = stepNumberDisabledTextColor;
            return this;
        }

        public Builder stepTitleTextColor(int stepTitleTextColor) {
            this.stepTitleTextColor = stepTitleTextColor;
            return this;
        }

        public Builder stepSubtitleTextColor(int stepSubtitleTextColor) {
            this.stepSubtitleTextColor = stepSubtitleTextColor;
            return this;
        }

        public Builder stepNumberDisabledBackgroundColor(int stepNumberDisabledBackgroundColor) {
            this.stepNumberDisabledBackgroundColor = stepNumberDisabledBackgroundColor;
            return this;
        }

        public Builder verticalLineColor(int verticalLineColor) {
            this.verticalLineColor = verticalLineColor;
            return this;
        }

        public Builder buttonTextColor(int buttonTextColor) {
            this.buttonTextColor = buttonTextColor;
            return this;
        }

        public Builder buttonPressedTextColor(int buttonPressedTextColor) {
            this.buttonPressedTextColor = buttonPressedTextColor;
            return this;
        }

        public Builder errorMessageTextColor(int errorMessageTextColor) {
            this.errorMessageTextColor = errorMessageTextColor;
            return this;
        }

        public Builder displayBottomNavigation(boolean displayBottomNavigationBar) {
            this.displayBottomNavigation = displayBottomNavigationBar;
            return this;
        }

        public Builder showConfirmationStep(boolean showConfirmationStep) {
            this.showConfirmationStep = showConfirmationStep;
            return this;
        }

        public Builder showLastStepNextButton(boolean showLastStepNextButton) {
            this.showLastStepNextButton = showLastStepNextButton;
            return this;
        }

        /**i
         * Set whether or not the disabled steps will have a Material Design look
         * @param materialDesignInDisabledSteps true to use Material Design for disabled steps; false otherwise
         * @return the builder instance
         */
        public Builder materialDesignInDisabledSteps(boolean materialDesignInDisabledSteps) {
            this.materialDesignInDisabledSteps = materialDesignInDisabledSteps;
            return this;
        }

        /**
         * Specify whether or not the keyboard should be hidden at the beginning
         * @param hideKeyboard true to hide the keyboard; false to not hide it
         * @return the builder instance
         */
        public Builder hideKeyboard(boolean hideKeyboard) {
            this.hideKeyboard = hideKeyboard;
            return this;
        }

        /**
         * Specify whether or not the vertical lines should be displayed when steps are collapsed
         * @param showVerticalLineWhenStepsAreCollapsed true to show the lines; false to not
         * @return the builder instance
         */
        public Builder showVerticalLineWhenStepsAreCollapsed(boolean showVerticalLineWhenStepsAreCollapsed) {
            this.showVerticalLineWhenStepsAreCollapsed = showVerticalLineWhenStepsAreCollapsed;
            return this;
        }

        public Builder alphaOfDisabledElements(float alpha) {
            this.alphaOfDisabledElements = alpha;
            return this;
        }

        public void init() {
            verticalStepperFormLayout.initialiseVerticalStepperForm(this);
        }
    }
}