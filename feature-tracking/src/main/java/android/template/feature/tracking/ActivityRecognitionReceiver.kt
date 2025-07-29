package android.template.feature.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val mostProbableActivity = result?.mostProbableActivity
            if (mostProbableActivity != null) {
                Log.d(TAG, "onReceive() :: Detected activity = ${mostProbableActivity.type} with confidence ${mostProbableActivity.confidence}")
                if (mostProbableActivity.type == DetectedActivity.STILL) {
                    val intent = Intent(ACTIVITY_RECOGNITION_ACTION).apply {
                        putExtra(ACTIVITY_RECOGNITION_ACTION_EXTRA_ACTIVITY_TYPE, mostProbableActivity.type)
                        putExtra(ACTIVITY_RECOGNITION_ACTION_EXTRA_ACTIVITY_CONFIDENCE, mostProbableActivity.confidence)
                    }
                    context.sendBroadcast(intent)
                }
            }
        }
    }
}

private const val TAG = "ActivityRecognitionReceiver"
