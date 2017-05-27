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
		void onShutterButtonFocus(ShutterButton b, boolean pressed);

		void onShutterButtonClick(ShutterButton b);
	}

	private OnShutterButtonListener mListener;
	private boolean mOldPressed;


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


	/**
	 * Hook into the drawable state changing to get changes to isPressed -- the
	 * onPressed listener doesn't always get called when the pressed state
	 * changes.
	 */
	@Override
	protected void drawableStateChanged() {
		super .drawableStateChanged();
		final boolean pressed = isPressed();
		if (pressed != mOldPressed) {
			if (!pressed) {
				post(new Runnable() {
					public void run() {
						callShutterButtonFocus(pressed);
					}
				});
			} else {
				callShutterButtonFocus(pressed);
			}
			mOldPressed = pressed;
		}
	}

	private void callShutterButtonFocus(boolean pressed) {
		if (mListener != null) {
			mListener.onShutterButtonFocus(this , pressed);
		}
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