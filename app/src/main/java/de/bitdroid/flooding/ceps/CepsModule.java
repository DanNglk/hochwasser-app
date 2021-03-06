package de.bitdroid.flooding.ceps;

import android.content.Context;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.jvalue.ceps.api.RegistrationApi;
import org.jvalue.ceps.api.UserApi;

import javax.inject.Inject;
import javax.inject.Named;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.auth.AuthClient;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;


/**
 * Dependecy injection configuratoin for CEPS related classes.
 */
public final class CepsModule implements Module {

	public static final String NAME_CEPS_REST_ADAPTER = "CEPS_REST_ADAPTER";

	@Override
	public void configure(Binder binder) {
		// nothing to do for now
	}


	@Provides
	@Inject
	@Named(NAME_CEPS_REST_ADAPTER)
	public RestAdapter provideCepsRestAapter(Context context, AuthClient authClient) {
		return new RestAdapter.Builder()
				.setClient(authClient)
				.setEndpoint(context.getString(R.string.ceps_base_url))
				.setConverter(new JacksonConverter())
				.build();
	}


	@Provides
	@Inject
	public UserApi provideUserApi(@Named(NAME_CEPS_REST_ADAPTER) RestAdapter restAdapter) {
		return restAdapter.create(UserApi.class);
	}


	@Provides
	@Inject
	public RegistrationApi provideRegistrationApi(@Named(NAME_CEPS_REST_ADAPTER) RestAdapter restAdapter) {
		return restAdapter.create(RegistrationApi.class);
	}

}
