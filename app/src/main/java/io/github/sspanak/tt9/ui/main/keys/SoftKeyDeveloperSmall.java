package io.github.sspanak.tt9.ui.main.keys;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;

import io.github.sspanak.tt9.R;

public class SoftKeyDeveloperSmall extends SoftKeyFnSmall {
	public SoftKeyDeveloperSmall(Context context) { super(context); }
	public SoftKeyDeveloperSmall(Context context, AttributeSet attrs) { super(context, attrs); }
	public SoftKeyDeveloperSmall(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

	@Override
	protected boolean isVisible() {
		return tt9 != null && tt9.isDeveloperCommandsActive();
	}

	@Override
	protected String getTitle() {
		int id = getId();
		if (id == R.id.soft_key_1) {
			return "1\nCtrl";
		} else if (id == R.id.soft_key_2) {
			return "2\nAlt";
		} else if (id == R.id.soft_key_3) {
			return "3\nFn";
		} else if (id == R.id.soft_key_4) {
			return "4\nMeta";
		} else if (id == R.id.soft_key_5) {
			return "5\nShift";
		} else if (id == R.id.soft_key_6) {
			return "6\nCtrlL";
		} else if (id == R.id.soft_key_7) {
			return "7\nAltL";
		} else if (id == R.id.soft_key_8) {
			return "8\nClr";
		} else if (id == R.id.soft_key_9) {
			return "9\nCaps";
		}

		return super.getTitle();
	}

	@Override
	protected float getTitleScale() {
		return 0.8f;
	}

	@Override
	protected int getBottomIconId() {
		int id = getId();
		if (id == R.id.soft_key_1 || id == R.id.soft_key_6) {
			return R.drawable.ic_dev_ctrl;
		} else if (id == R.id.soft_key_2 || id == R.id.soft_key_7) {
			return R.drawable.ic_dev_alt;
		} else if (id == R.id.soft_key_3) {
			return R.drawable.ic_dev_fn;
		} else if (id == R.id.soft_key_4) {
			return R.drawable.ic_dev_meta;
		} else if (id == R.id.soft_key_5) {
			return R.drawable.ic_fn_shift_up;
		} else if (id == R.id.soft_key_8) {
			return R.drawable.ic_txt_select_none;
		} else if (id == R.id.soft_key_9) {
			return R.drawable.ic_dev_caps;
		}

		return -1;
	}

	@Override
	public void render() {
		final int iconId = getBottomIconId();
		final Drawable icon = iconId > 0 && tt9 != null ? AppCompatResources.getDrawable(tt9.getApplicationContext(), iconId) : null;
		setCompoundDrawablesWithIntrinsicBounds(null, null, null, icon);
		super.render();
		setTextColor(tt9 != null && tt9.isDeveloperModifierHeld(getNumber()) ? Color.RED : getTextColors().getDefaultColor());
	}
}
