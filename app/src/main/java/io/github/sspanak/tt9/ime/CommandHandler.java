package io.github.sspanak.tt9.ime;

import android.view.KeyEvent;

import java.util.ArrayList;

import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.commands.CmdCommandPalette;
import io.github.sspanak.tt9.commands.Command;
import io.github.sspanak.tt9.commands.CommandCollection;
import io.github.sspanak.tt9.db.words.DictionaryLoader;
import io.github.sspanak.tt9.ime.modes.InputMode;
import io.github.sspanak.tt9.ime.modes.InputModeKind;
import io.github.sspanak.tt9.languages.LanguageCollection;
import io.github.sspanak.tt9.languages.NaturalLanguage;
import io.github.sspanak.tt9.ui.UI;
import io.github.sspanak.tt9.util.Ternary;

abstract public class CommandHandler extends TextEditingHandler {
	private int developerMetaState = 0;
	private boolean awaitingDeveloperComboKey = false;
  private final CmdCommandPalette cmdPalette = new CmdCommandPalette();

	@Override
	protected Ternary onBack() {
		if (hideDeveloperCommands() || cmdPalette.hideCommandPalette(getFinalContext())) {
			return Ternary.TRUE;
		}
		return super.onBack();
	}


	@Override
	protected boolean onNumber(int key, boolean hold, int repeat) {
		if (statusBar.isErrorShown()) {
			resetStatus();
		}

		if (!shouldBeOff() && mainView.isDeveloperCommandsShown()) {
			onDeveloperCommand(key);
			return true;
		}

		if (!shouldBeOff() && awaitingDeveloperComboKey) {
			return sendDeveloperCombination(key, repeat);
		}

		if (!shouldBeOff() && mainView.isCommandPaletteShown()) {
			Command cmd = CommandCollection.getByHardKey(CommandCollection.COLLECTION_PALETTE, key);
			if (cmd.isAvailable(getFinalContext())) {
				cmd.run(getFinalContext());
			}
			return true;
		}

		return super.onNumber(key, hold, repeat);
	}

	@Override
	public boolean onText(String text, boolean validateOnly) {
		if (mainView.isDeveloperCommandsShown() && "#".equals(text)) {
			if (!validateOnly) {
				awaitingDeveloperComboKey = developerMetaState != 0;
				hideDeveloperCommands();
			}
			return true;
		}

		return super.onText(text, validateOnly);
	}

	private void onCommand(int key) {
		switch (key) {
			case 1:
				showSettings();
				break;
			case 2:
				addWord();
				break;
			case 3:
				toggleVoiceInput();
				break;
			case 4:
				undo();
				break;
			case 5:
				showTextEditingPalette();
				break;
			case 6:
				redo();
				break;
			case 7:
				showDeveloperCommands();
				break;
			case 8:
				selectKeyboard();
				break;
		}
	}


	private void onDeveloperCommand(int key) {
		switch (key) {
			case 1:
				toggleDeveloperMeta(KeyEvent.META_CTRL_ON);
				break;
			case 2:
				toggleDeveloperMeta(KeyEvent.META_ALT_ON);
				break;
			case 3:
				toggleDeveloperMeta(KeyEvent.META_FUNCTION_ON);
				break;
			case 4:
				toggleDeveloperMeta(KeyEvent.META_META_ON);
				break;
			case 5:
				toggleDeveloperMeta(KeyEvent.META_SHIFT_ON);
				break;
			case 6:
				toggleDeveloperMeta(KeyEvent.META_CTRL_LEFT_ON);
				break;
			case 7:
				toggleDeveloperMeta(KeyEvent.META_ALT_LEFT_ON);
				break;
			case 8:
				clearDeveloperModifiers();
				break;
			case 9:
				toggleDeveloperMeta(KeyEvent.META_CAPS_LOCK_ON);
				break;
		}

		mainView.renderKeys();
	}


	private void toggleDeveloperMeta(int metaFlag) {
		developerMetaState = (developerMetaState & metaFlag) == 0 ? (developerMetaState | metaFlag) : (developerMetaState & ~metaFlag);
	}


	private boolean sendDeveloperCombination(int key, int repeat) {
		final int keyCode = resolveDeveloperKeyCode(key, repeat);
		if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
			return false;
		}

		boolean handled = textField.sendDownUpKeyEvents(keyCode, developerMetaState);
		clearDeveloperModifiers();
		awaitingDeveloperComboKey = false;
		resetStatus();
		return handled;
	}


	private int resolveDeveloperKeyCode(int key, int repeat) {
		if (key < 0 || key > 9 || mLanguage == null) {
			return KeyEvent.KEYCODE_UNKNOWN;
		}

		ArrayList<String> keyChars = mLanguage.getKeyCharacters(key);
		if (keyChars.isEmpty()) {
			return KeyEvent.KEYCODE_UNKNOWN;
		}

		int index = repeat % keyChars.size();
		String keyValue = keyChars.get(index);
		if (keyValue == null || keyValue.isEmpty()) {
			return KeyEvent.KEYCODE_UNKNOWN;
		}

		char keyChar = keyValue.charAt(0);
		if (Character.isLetter(keyChar)) {
			String name = "KEYCODE_" + Character.toUpperCase(keyChar);
			return KeyEvent.keyCodeFromString(name);
		}

		if (Character.isDigit(keyChar)) {
			return KeyEvent.KEYCODE_0 + Character.getNumericValue(keyChar);
		}

		return switch (keyChar) {
			case ' ' -> KeyEvent.KEYCODE_SPACE;
			case '\n' -> KeyEvent.KEYCODE_ENTER;
			default -> KeyEvent.KEYCODE_UNKNOWN;
		};
	}


	@Override
	protected boolean navigateBack() {
		return cmdPalette.hideCommandPalette(getFinalContext()) || hideDeveloperCommands() || super.navigateBack();
	}


	public void resetStatus() {
		if (mainView.isCommandPaletteShown()) {
			statusBar.setText(R.string.commands_select_command);
			statusBar.setAccessibilityText(R.string.commands_select_command);
		} else if (mainView.isTextEditingPaletteShown()) {
			statusBar.setText(R.string.commands_select_command);
			statusBar.setAccessibilityText(R.string.commands_select_command);
		} else {
			statusBar.setText(mInputMode);
			statusBar.setAccessibilityText(mInputMode);
		}

		if (mainView.isTextEditingPaletteShown()) {
			String preview = Clipboard.getPreview(this);
			statusBar.setText(preview.isEmpty() ? getString(R.string.commands_select_command) : "[ \"" + preview + "\" ]");
			return;
		}
		if (mainView.isDeveloperCommandsShown()) {
			statusBar.setText(R.string.developer_select_modifier);
			return;
		}

		statusBar.setText(mInputMode);
	}

	public boolean isDeveloperModifierHeld(int keyNumber) {
		if (!mainView.isDeveloperCommandsShown()) {
			return false;
		}

		return switch (keyNumber) {
			case 1 -> (developerMetaState & KeyEvent.META_CTRL_ON) != 0;
			case 2 -> (developerMetaState & KeyEvent.META_ALT_ON) != 0;
			case 3 -> (developerMetaState & KeyEvent.META_FUNCTION_ON) != 0;
			case 4 -> (developerMetaState & KeyEvent.META_META_ON) != 0;
			case 5 -> (developerMetaState & KeyEvent.META_SHIFT_ON) != 0;
			case 6 -> (developerMetaState & KeyEvent.META_CTRL_LEFT_ON) != 0;
			case 7 -> (developerMetaState & KeyEvent.META_ALT_LEFT_ON) != 0;
			case 9 -> (developerMetaState & KeyEvent.META_CAPS_LOCK_ON) != 0;
			default -> false;
		};
	}

	public boolean isDeveloperCommandsEnabled() {
		return settings.getDeveloperCommandsEnabled();
	}

	public void setInputMode(int modeId) {
		if (!allowedInputModes.contains(modeId) && modeId != InputMode.MODE_RECOMPOSING) {
			return;
		}

		suggestionOps.cancelDelayedAccept();
		mInputMode.onAcceptSuggestion(suggestionOps.acceptIncomplete());
		resetKeyRepeat();

		mInputMode = InputMode.getInstance(settings, mLanguage, inputType, textField, modeId);
		determineTextCase();

		if (modeId != InputMode.MODE_RECOMPOSING) {
			settings.saveInputMode(mInputMode.getId());
		}

		// update the UI
		getDisplayTextCase(mLanguage, mInputMode.getTextCase());
		setStatusIcon(mInputMode, mLanguage);
		statusBar.setText(mInputMode);
		statusBar.setAccessibilityText(mInputMode);
		mainView.render();

		if (settings.isMainLayoutStealth() && !settings.isStatusIconEnabled()) {
			UI.toastShortSingle(this, mInputMode.getClass().getSimpleName(), mInputMode.toString());
		}
	}


	public void setLang(int langId) {
		if (!mEnabledLanguages.contains(langId)) {
			return;
		}

		suggestionOps.cancelDelayedAccept();
		stopVoiceInput();

		mLanguage = LanguageCollection.getLanguage(langId);
		validateLanguages();

		settings.setDefaultChars(mLanguage, false); // initialize default order, if missing
		((NaturalLanguage) mLanguage).updateKeyCharacters(settings); // and update the layout for 2..9 keys, if needed

		// for languages that do not have ABC or Predictive, make sure we remain in valid state
		mInputMode = InputMode
			.getInstance(settings, mLanguage, inputType, textField, determineInputModeId())
			.copy(mInputMode);

		if (mInputMode.isTyping()) {
			getSuggestions(0, null, this::onAfterLanguageChange);
		} else {
			onAfterLanguageChange();
		}

		if (InputModeKind.isPredictive(mInputMode)) {
			DictionaryLoader.autoLoad(this, settings, mLanguage);
		}

		mindReader.setLanguage(mLanguage).seed(getFinalContext(), mLanguage);

		forceShowWindow();
	}


	private void onAfterLanguageChange() {
		getDisplayTextCase(mLanguage, mInputMode.getTextCase());
		setStatusIcon(mInputMode, mLanguage);
		statusBar.setText(mInputMode);
		statusBar.setAccessibilityText(mInputMode);
		suggestionOps.setLanguage(mLanguage);
		mainView.render();
		if (settings.isMainLayoutStealth() && !settings.isStatusIconEnabled()) {
			UI.toastShortSingle(this, mInputMode.getClass().getSimpleName(), mInputMode.toString());
		}
	}


	public boolean nextTextCase() {
		final String currentWord = !suggestionOps.isEmpty() && mInputMode.isTyping() ? suggestionOps.getCurrent() : "";

		if (!mInputMode.nextTextCase(currentWord, displayTextCase)) {
			return false;
		}

		mInputMode.skipNextTextCaseDetection();
		if (!InputModeKind.isRecomposing(mInputMode)) {
			settings.saveTextCase(mInputMode.getTextCase());
		}

		if (currentWord.isEmpty() && !suggestionOps.isEmpty()) {
			// if we have set the suggestions from a different source, e.g. Clipboard or MindReader,
			// they won't be in the InputMode's state, so adjust the list directly, without any specific rules
			suggestionOps.setTextCase(mLanguage, mInputMode.getTextCase());
			appHacks.setComposingText(suggestionOps.getCurrent());
			return true;
		} else if (currentWord.isEmpty() || (currentWord.length() == 1 && !Character.isAlphabetic(currentWord.charAt(0)))) {
			// if there are no suggestions, or they are special chars, we don't need to adjust their text case
			return true;
		}

		// if there are suggestions, we need to adjust their text case to acknowledge the change
		int currentSuggestionIndex = suggestionOps.getCurrentIndex();
		currentSuggestionIndex = suggestionOps.containsStem() ? currentSuggestionIndex - 1 : currentSuggestionIndex;

		suggestionOps.set(mInputMode.getSuggestions(), currentSuggestionIndex, mInputMode.containsGeneratedSuggestions());

		if (InputModeKind.isRecomposing(mInputMode)) {
			appHacks.setComposingTextPartsWithHighlightedJoining(mInputMode.getWordStem() + suggestionOps.getCurrent(), mInputMode.getRecomposingSuffix());
		} else {
			mindReader.setTextCase(mInputMode.getTextCaseRaw());
			suggestionOps.addGuesses(mindReader.getGuesses());
			appHacks.setComposingText(suggestionOps.getCurrent());
		}

		return true;
	}


	public void showSettings() {
		suggestionOps.cancelDelayedAccept();
		stopVoiceInput();
		UI.showSettingsScreen(this, null);
	}


	public void showCommandPalette() {
		if (mainView.isCommandPaletteShown()) {
			return;
		}

		suggestionOps.cancelDelayedAccept();
		mInputMode.onAcceptSuggestion(suggestionOps.acceptIncomplete());
		mInputMode.reset();
		awaitingDeveloperComboKey = false;

		mainView.showCommandPalette();
		resetStatus();
	}


	public boolean hideCommandPalette() {
		if (!mainView.isCommandPaletteShown()) {
			return false;
		}

		mainView.showKeyboard();
		if (voiceInputOps.isListening()) {
			stopVoiceInput();
		} else {
			resetStatus();
		}

		return true;
	}


	public void showDeveloperCommands() {
		if (!settings.getDeveloperCommandsEnabled() || mainView.isDeveloperCommandsShown()) {
			return;
		}

		suggestionOps.cancelDelayedAccept();
		mInputMode.onAcceptSuggestion(suggestionOps.acceptIncomplete());
		mInputMode.reset();
		awaitingDeveloperComboKey = false;
		mainView.showDeveloperCommands();
		resetStatus();
	}


	public boolean hideDeveloperCommands() {
		if (!mainView.isDeveloperCommandsShown()) {
			return false;
		}

		mainView.showKeyboard();
		if (voiceInputOps.isListening()) {
			stopVoiceInput();
		} else {
			resetStatus();
		}

		return true;
	}


	private void clearDeveloperModifiers() {
		developerMetaState = 0;
		mainView.renderKeys();
	}


	protected boolean undo() {
		return textField.sendDownUpKeyEvents(KeyEvent.KEYCODE_Z, false, true);
	}


	protected boolean redo() {
		return textField.sendDownUpKeyEvents(KeyEvent.KEYCODE_Z, true, true);
	}

	public void showDeveloperCommands() {
		if (mainView.isDeveloperCommandsShown()) {
			return;
		}
		suggestionOps.cancelDelayedAccept();
		mInputMode.onAcceptSuggestion(suggestionOps.acceptIncomplete());
		mInputMode.reset();
		mainView.showDeveloperCommands();
		resetStatus();
	}
	
	public boolean hideDeveloperCommands() {
		if (!mainView.isDeveloperCommandsShown()) {
			return false;
		}
		mainView.showKeyboard();
		if (voiceInputOps.isListening()) {
			stopVoiceInput();
		} else {
			resetStatus();
		}
	}
}
