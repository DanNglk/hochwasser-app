package de.bitdroid.flooding.ui;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.SignInButton;

import org.jvalue.ceps.api.UserApi;
import org.jvalue.commons.auth.OAuthUserDescription;
import org.jvalue.commons.auth.Role;
import org.jvalue.commons.auth.User;

import java.io.IOException;

import javax.inject.Inject;

import de.bitdroid.flooding.BuildConfig;
import de.bitdroid.flooding.R;
import de.bitdroid.flooding.auth.LoginManager;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

@ContentView(R.layout.activity_login)
public class LoginActivity extends AbstractActivity {

	private static final String
			GOOGLE_ACCOUNT_TYPE = "com.google";

	private static final String
			STATE_SELECTED_ACCOUNT = "STATE_SELECTED_ACCOUNT";

	private static final int
			REQUEST_CODE_AUTH = 42,
			REQUEST_CODE_ACCOUNT = 43;

	@InjectView(R.id.login) private SignInButton loginButton;
	@Inject private LoginManager loginManager;
	@Inject private UserApi userApi;

	private Account selectedAccount = null;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		analyticsUtils.onScreen("login screen");

		// setup login
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				analyticsUtils.onClick("login");
				uiUtils.createBetaDialog()
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// check if there is only one account --> bypass account picker
										Account[] accounts = AccountManager.get(LoginActivity.this).getAccountsByType(GOOGLE_ACCOUNT_TYPE);
										if (accounts.length == 1) {
											selectedAccount = accounts[0];
											registerAndGetUser();
											return;
										}

										// if multiple accounts are present show account picker
										String[] accountTypes = new String[]{ GOOGLE_ACCOUNT_TYPE };
										Intent intent = AccountPicker.newChooseAccountIntent(null, null, accountTypes, false, null, null, null, null);
										startActivityForResult(intent, REQUEST_CODE_ACCOUNT);
									}
								})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
			}
		});

		// check if signed in
		if (loginManager.getAccount().isPresent()) {
			if (BuildConfig.DEBUG) {
				showMainActivity();
			} else {
				// show main activity after delay (slash screen)
				loginButton.setVisibility(View.GONE);
				loginButton.postDelayed(new Runnable() {
					@Override
					public void run() {
						showMainActivity();
					}
				}, 2000);
			}
		}

		// restore state
		if (savedInstanceState != null) {
			selectedAccount = savedInstanceState.getParcelable(STATE_SELECTED_ACCOUNT);
		}
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_SELECTED_ACCOUNT, selectedAccount);
	}


	private void showMainActivity() {
		Intent intent = new Intent(this, MainDrawerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) return;
		switch (requestCode) {
			case REQUEST_CODE_AUTH:
				// recovered from Google auth exception
				registerAndGetUser();
				break;

			case REQUEST_CODE_ACCOUNT:
				// user selected account
				selectedAccount = new Account(
						data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),
						data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
				registerAndGetUser();
				break;
		}
	}


	private void registerAndGetUser() {
		showSpinner();
		compositeSubscription.add(Observable
				.defer(new Func0<Observable<String>>() {
					@Override
					public Observable<String> call() {
						try {
							return Observable.just(loginManager.getToken(selectedAccount));
						} catch (IOException | GoogleAuthException e) {
							return Observable.error(e);
						}
					}
				})
				.flatMap(new Func1<String, Observable<User>>() {
					@Override
					public Observable<User> call(String accessToken) {
						return userApi.addUser(new OAuthUserDescription(Role.PUBLIC, accessToken));
					}
				})
				.compose(networkUtils.<User>getDefaultTransformer())
				.subscribe(new Action1<User>() {
					@Override
					public void call(User user) {
						Timber.d("login success (" + user.getId() + ")");
						loginManager.setAccount(selectedAccount);
						showMainActivity();
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Timber.e(throwable, "login error");
						hideSpinner();
						if (throwable instanceof UserRecoverableAuthException) {
							Timber.d("starting recover activity");
							startActivityForResult(((UserRecoverableAuthException) throwable).getIntent(), REQUEST_CODE_AUTH);

						} else {
							analyticsUtils.onException("login error", false);
							new AlertDialog.Builder(LoginActivity.this)
									.setTitle(R.string.error_login_title)
									.setMessage(R.string.error_login_message)
									.setPositiveButton(android.R.string.ok, null)
									.show();
						}
					}
				}));
	}

}
