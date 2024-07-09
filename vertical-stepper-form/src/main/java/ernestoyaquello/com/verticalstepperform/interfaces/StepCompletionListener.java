package ernestoyaquello.com.verticalstepperform.interfaces;

public interface StepCompletionListener {
    void onStepCompleted(int stepNumber);
    void onStepUncompleted(int stepNumber);
}
