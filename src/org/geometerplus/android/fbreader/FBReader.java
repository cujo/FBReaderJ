/*
 * Copyright (C) 2009-2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import java.util.LinkedList;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.ActionCode;
import android.provider.Settings;

public final class FBReader extends ZLAndroidActivity {
	static FBReader Instance;

	private int myFullScreenFlag;

	private static class NavigationButtonPanel extends ControlButtonPanel {
		public volatile boolean NavigateDragging;
		public ZLTextPosition StartPosition;

		@Override
		public void onShow() {
			if (FBReader.Instance != null && myControlPanel != null) {
				FBReader.Instance.setupNavigation(myControlPanel);
			}
		}

		@Override
		public void updateStates() {
			super.updateStates();
			if (!NavigateDragging && FBReader.Instance != null && myControlPanel != null) {
				FBReader.Instance.setupNavigation(myControlPanel);
			}
		}
	}

	private static class TextSearchButtonPanel extends ControlButtonPanel {
		@Override
		public void onHide() {
			final ZLTextView textView = (ZLTextView) ZLApplication.Instance().getCurrentView();
			textView.clearFindResults();
		}
	}

	private static TextSearchButtonPanel myTextSearchPanel;
	private static NavigationButtonPanel myNavigatePanel;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		/*
		android.telephony.TelephonyManager tele =
			(android.telephony.TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		System.err.println(tele.getNetworkOperator());
		*/
		Instance = this;
		final ZLAndroidApplication application = ZLAndroidApplication.Instance();
		myFullScreenFlag =
			application.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN, myFullScreenFlag
		);
		if (myTextSearchPanel == null) {
			myTextSearchPanel = new TextSearchButtonPanel();
			myTextSearchPanel.register();
		}
		if (myNavigatePanel == null) {
			myNavigatePanel = new NavigationButtonPanel();
			myNavigatePanel.register();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		final ZLAndroidApplication application = ZLAndroidApplication.Instance();

		final int fullScreenFlag =
			application.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (fullScreenFlag != myFullScreenFlag) {
			finish();
			startActivity(new Intent(this, this.getClass()));
		}

		final RelativeLayout root = (RelativeLayout)FBReader.this.findViewById(R.id.root_view);
		if (!myTextSearchPanel.hasControlPanel()) {
			final ControlPanel panel = new ControlPanel(this);

			panel.addButton(ActionCode.FIND_PREVIOUS, false, R.drawable.text_search_previous);
			panel.addButton(ActionCode.CLEAR_FIND_RESULTS, true, R.drawable.text_search_close);
			panel.addButton(ActionCode.FIND_NEXT, false, R.drawable.text_search_next);

			myTextSearchPanel.setControlPanel(panel, root, false);
		}
		if (!myNavigatePanel.hasControlPanel()) {
			final ControlPanel panel = new ControlPanel(this);
			final View layout = getLayoutInflater().inflate(R.layout.navigate, panel, false);
			createNavigation(layout);
			panel.setExtension(layout);
			myNavigatePanel.setControlPanel(panel, root, true);
		}

		findViewById(R.id.main_view).setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				if (!myNavigatePanel.getVisibility()) {
					navigate();
					return true;
				}
				return false;
			}
		});
	}

	private PowerManager.WakeLock myWakeLock;

	@Override
	public void onResume() {
		super.onResume();
		ControlButtonPanel.restoreVisibilities();
		if (ZLAndroidApplication.Instance().DontTurnScreenOffOption.getValue()) {
			myWakeLock =
				((PowerManager)getSystemService(POWER_SERVICE)).
					newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader");
			myWakeLock.acquire();
		} else {
			myWakeLock = null;
		}
	}

	@Override
	public void onPause() {
		if (myWakeLock != null) {
			myWakeLock.release();
			myWakeLock = null;
		}
		ControlButtonPanel.saveVisibilities();
		super.onPause();
	}

	@Override
	public void onStop() {
		ControlButtonPanel.removeControlPanels();
		if (additionalInfo) {
			app.ScrollbarTypeOption.setValue(scrollbarOpt);
		}
		super.onStop();
	}

	void showTextSearchControls(boolean show) {
		if (show) {
			myTextSearchPanel.show(true);
		} else {
			myTextSearchPanel.hide(false);
		}
	}

	private static org.geometerplus.fbreader.fbreader.FBReader app;
	protected ZLApplication createApplication(String fileName) {
		new SQLiteBooksDatabase();
		String[] args = (fileName != null) ? new String[] { fileName } : new String[0];

		app = new org.geometerplus.fbreader.fbreader.FBReader(args);
		return app;
	}

	@Override
	public boolean onSearchRequested() {
		final LinkedList<Boolean> visibilities = new LinkedList<Boolean>();
		ControlButtonPanel.saveVisibilitiesTo(visibilities);
		ControlButtonPanel.hideAllPendingNotify();
		final SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
		manager.setOnCancelListener(new SearchManager.OnCancelListener() {
			public void onCancel() {
				ControlButtonPanel.restoreVisibilitiesFrom(visibilities);
				manager.setOnCancelListener(null);
			}
		});
		final org.geometerplus.fbreader.fbreader.FBReader fbreader =
			(org.geometerplus.fbreader.fbreader.FBReader)ZLApplication.Instance();
		startSearch(fbreader.TextSearchPatternOption.getValue(), true, null, false);
		return true;
	}


	public void navigate() {
		final ZLTextView textView = (ZLTextView) ZLApplication.Instance().getCurrentView();
		myNavigatePanel.NavigateDragging = false;
		myNavigatePanel.StartPosition = new ZLTextFixedPosition(textView.getStartCursor());
		myNavigatePanel.show(true);
	}



	public final boolean canNavigate() {
		final org.geometerplus.fbreader.fbreader.FBReader fbreader =
			(org.geometerplus.fbreader.fbreader.FBReader)ZLApplication.Instance();
		final ZLView view = fbreader.getCurrentView();
		if (!(view instanceof ZLTextView)) {
			return false;
		}
		final ZLTextModel textModel = ((ZLTextView) view).getModel();
		if (textModel == null || textModel.getParagraphsNumber() == 0) {
			return false;
		}
		final BookModel bookModel = fbreader.Model;
		return bookModel != null && bookModel.Book != null;
	}

	private final void createNavigation(View layout) {
		final SeekBar slider = (SeekBar) layout.findViewById(R.id.book_position_slider);
		final TextView text = (TextView) layout.findViewById(R.id.book_position_text);

		slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private void gotoPage(int page) {
				final ZLView view = ZLApplication.Instance().getCurrentView();
				if (view instanceof ZLTextView) {
					ZLTextView textView = (ZLTextView) view;
					if (page == 1) {
						textView.gotoHome();
					} else {
						textView.gotoPage(page);
					}
					ZLApplication.Instance().repaintView();
				}
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				myNavigatePanel.NavigateDragging = false;
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				myNavigatePanel.NavigateDragging = true;
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					final int page = progress + 1;
					final int pagesNumber = seekBar.getMax() + 1; 
					text.setText(makeProgressText(page, pagesNumber));
					gotoPage(page);
				}
			}
		});

		final Button btnOk = (Button) layout.findViewById(android.R.id.button1);
		final Button btnCancel = (Button) layout.findViewById(android.R.id.button3);
		View.OnClickListener listener = new View.OnClickListener() {
			public void onClick(View v) {
				final ZLTextPosition position = myNavigatePanel.StartPosition;
				myNavigatePanel.StartPosition = null;
				if (v == btnCancel && position != null) {
					((ZLTextView) ZLApplication.Instance().getCurrentView()).gotoPosition(position);
				}
				myNavigatePanel.hide(true);
			}
		};
		btnOk.setOnClickListener(listener);
		btnCancel.setOnClickListener(listener);
		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		btnOk.setText(buttonResource.getResource("ok").getValue());
		btnCancel.setText(buttonResource.getResource("cancel").getValue());
	}

	private final void setupNavigation(ControlPanel panel) {
		final SeekBar slider = (SeekBar) panel.findViewById(R.id.book_position_slider);
		final TextView text = (TextView) panel.findViewById(R.id.book_position_text);

		final ZLTextView textView = (ZLTextView) ZLApplication.Instance().getCurrentView();
		final int page = textView.computeCurrentPage();
		final int pagesNumber = textView.computePageNumber();

		if (slider.getMax() != (pagesNumber - 1)
				|| slider.getProgress() != (page - 1)) {
			slider.setMax(pagesNumber - 1);
			slider.setProgress(page - 1);
			text.setText(makeProgressText(page, pagesNumber));
		}
	}

	private static String makeProgressText(int page, int pagesNumber) {
		return "" + page + " / " + pagesNumber;
	}

	private int scrollbarOpt;
	private boolean additionalInfo = false;

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((KeyEvent.KEYCODE_BACK == keyCode && additionalInfo) || KeyEvent.KEYCODE_DPAD_CENTER == keyCode) {
				if (!additionalInfo) {
					scrollbarOpt = app.ScrollbarTypeOption.getValue();
					app.ScrollbarTypeOption.setValue(1);
					setFullScreen(false);
				} else {
					app.ScrollbarTypeOption.setValue(scrollbarOpt);
					setFullScreen(WindowManager.LayoutParams.FLAG_FULLSCREEN == myFullScreenFlag);
				}
				additionalInfo = !additionalInfo;
				return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	public static FBReader getInstance() {
		return Instance;
	}

	public void setFullScreen(boolean fullscreen) {
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
		if (fullscreen) {
			attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
		} else {
			attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		getWindow().setAttributes(attrs);
	}

	public void setBrightness(int delta) {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		int b = 50;
		if (lp.screenBrightness < 0) {
			try {
				b = Math.round(Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / (float)255 * (float)100);
			} catch (Settings.SettingNotFoundException e) { }
		} else {
			b = Math.round(lp.screenBrightness * (float)100);
		}
		if (delta > 0 && b < 100) {
			b += Math.min(delta, 100 - b);
		} else if (delta < 0 && b > 1) {
			b += Math.max(delta, 1 - b);
		}
		lp.screenBrightness = (float)b / (float)100;
		getWindow().setAttributes(lp);
	}
}
