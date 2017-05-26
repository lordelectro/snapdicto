package snapdicto.com.snapdicto.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;
import android.widget.ImageView;
/**
 * On-screen shutter button that can call a delegate when the
 * pressed state changes.
 * 
 */
public class ShutterButton extends android.support.v7.widget.AppCompatImageView {
	/**
	 * A callback to be invoked when a ShutterButton's pressed state changes.
	 */
	public interface OnShutterButtonListener {
		/**
		 * Called when a ShutterButton has been pressed.
		 *
		 * @param b The ShutterButton that was pressed.
		 */


		void onShutterButtonClick(ShutterButton b);
	}

	private OnShutterButtonListener mListener;


	public ShutterButton(Context context) {
		super (context);
	}

	public ShutterButton(Context context, AttributeSet attrs) {
		super (context, attrs);
	}

	public ShutterButton(Context context, AttributeSet attrs,
                         int defStyle) {
		super (context, attrs, defStyle);
	}
	//Called when Shutter Button is clicked
	public void setOnShutterButtonListener(OnShutterButtonListener listener) {
		mListener = listener;
	}


	 //Performs Click on Success.
	 @Override
	 public boolean performClick() {
		 boolean result = super.performClick();
		 playSoundEffect(SoundEffectConstants.CLICK);
		 if (mListener != null) {
			 mListener.onShutterButtonClick(this);
		 }
		 return result;
	 }
}