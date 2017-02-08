package de.bitdroid.flooding.app;

import com.orm.SugarApp;
import de.bitdroid.flooding.ceps.CepsModule;
import de.bitdroid.flooding.ods.OdsModule;
import roboguice.RoboGuice;

/**
 * Main application for flooding.
 */
public class FloodingApplication extends SugarApp {

	@Override
	public void onCreate() {
		super.onCreate();

		RoboGuice.getOrCreateBaseApplicationInjector(
				this,
				RoboGuice.DEFAULT_STAGE,
				RoboGuice.newDefaultRoboModule(this),
				new AppModule(),
				new OdsModule(),
				new CepsModule());
	}
}
